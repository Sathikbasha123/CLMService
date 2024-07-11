package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import freemarker.template.TemplateException;
import saaspe.clm.configuration.mongo.SequenceGeneratorService;
import saaspe.clm.constant.Constant;
import saaspe.clm.document.CentralRepoDocument;
import saaspe.clm.document.DocumentVersionDocument;
import saaspe.clm.document.EnvelopeDocument;
import saaspe.clm.document.LooContractDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.TADocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
import saaspe.clm.docusign.model.DocumentVersioningResponse;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.AllSigners;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Document;
import saaspe.clm.model.DocumentCreateResponse;
import saaspe.clm.model.DocumentResponse;
import saaspe.clm.model.DocusignUrls;
import saaspe.clm.model.DocusignUserCache;
import saaspe.clm.model.EnvelopeResponse;
import saaspe.clm.model.Response;
import saaspe.clm.model.Reviewers;
import saaspe.clm.model.TADocumentRequest;
import saaspe.clm.model.UserDetails;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.DocumentVersionDocumentRepository;
import saaspe.clm.repository.EnvelopeRepository;
import saaspe.clm.repository.LooContractDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.TADocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.service.TAService;
import saaspe.clm.utills.AllRoleFlowMapper;
import saaspe.clm.utills.Base64ToMultipartFileConverter;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

@Service
public class TAServiceImpl implements TAService {

	private static final Logger log = LoggerFactory.getLogger(TAServiceImpl.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RedisUtility redisUtility;

	@Value("${redirecturl.path}")
	private String redirectUrl;

	@Value("${sendgrid.domain.name}")
	private String mailDomainName;

	@Autowired
	private EnvelopeRepository envelopeRepository;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Autowired
	private TADocumentRepository taDocumentRepository;

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private DocumentVersionDocumentRepository documentVersionDocumentRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository workFlowCreatorDocumentRespository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

	@Autowired
	private LooContractDocumentRepository looContractDocumentRepository;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Value("${azure.storage.container.name}")
	private String containerName;

	@Value("${docusign-urls-file}")
	private String docusignUrls;

	@Value("${spring.media.host}")
	private String mediaHost;

	@Value("${spring.image.key}")
	private String imageKey;

	@Value("${sendgrid.domain.support}")
	private String supportEmail;

	@Value("${docusign.host}")
	private String docusignHost;

	@Autowired
	private MailSenderService mailSenderService;

	@Autowired
	private CloudBlobClient cloudBlobClient;

	Random random = new Random();

	private final MongoTemplate mongoTemplate;

	public TAServiceImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public DocusignUrls getDousignUrl() {
		ClassPathResource resource = new ClassPathResource(docusignUrls);
		DocusignUrls docusignUrl = null;
		try {
			docusignUrl = mapper.readValue(resource.getInputStream(), DocusignUrls.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return docusignUrl;
	}

	public CommonResponse addTaDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String envelopeId, String profile, String name)
			throws DataValidationException, IOException {
		TADocumentRequest jsonrequest = mapper.readValue(json, TADocumentRequest.class);
		int minRange = 1000;
		int maxRange = 9999;
		List<String> createIdsList = createId != null ? Arrays.asList(createId) : new ArrayList<>();
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();
		if (createId == null && createDocumentFiles == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()),
					"Provide existing documentId or upload document to send envelope");

		}
		List<DocumentResponse> blobDocumentResponses = new ArrayList<>();
		List<MultipartFile> multipartList = new ArrayList<>();
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + profile);

		ArrayList<saaspe.clm.model.Document> documentRequests = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				try {
					documentRequest
							.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
					documentRequest.setName(
							createDocumentFile != null
									? createDocumentFile.getOriginalFilename().substring(0,
											createDocumentFile.getOriginalFilename().indexOf("."))
									: null);
					documentRequest.setCategory("createFile");
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					documentRequest.setDocumentId(String.valueOf(randomNumber));
					documentRequests.add(documentRequest);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		jsonrequest.setDocuments(documentRequests);
		if (createId != null) {
			int i = 0;
			for (String createid : createIdsList) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				documentRequest.setDocumentId(createIdsList.get(i));
				documentRequest.setCategory("createId");
				documentRequests.add(documentRequest);
				i++;
			}
		}
		jsonrequest.setDocuments(documentRequests);
		if (deleteIdsList != null) {
			for (String deleteid : deleteIdsList) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				documentRequest.setDocumentId(deleteid);
				documentRequest.setCategory("delete");
				documentRequests.add(documentRequest);
			}
		}
		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		List<Document> versionDocDocuments = new ArrayList<>();
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(profile);
		TADocument taDocument = new TADocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(TADocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> projectIdmap = new HashMap<>();
		projectIdmap.put("projectId", projectId);
		taDocument.setId(id);
		taDocument.setProjectId(projectId);
		taDocument.setBuID("BUID");
		taDocument.setOpID("SAASPE");
		taDocument.setCreatedOn(new Date());
		taDocument.setCreatedBy(profile);
		taDocument.setFlowCompleted(false);
		taDocument.setStatus(jsonrequest.getStatus());
		taDocument.setVersion("1.0");
		taDocument.setOrder(0);
		taDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		taDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		taDocument.setContractTitle(jsonrequest.getContractTitle());
		taDocument.setTemplateId(envelopeId);
		taDocument.setTenant(jsonrequest.getTenant());
		taDocument.setTenderNo(jsonrequest.getTenderNo());
		taDocument.setReferenceNo(jsonrequest.getReferenceNo());
		taDocument.setTradingName(jsonrequest.getTradingName());
		taDocument.setTenancyTerm(jsonrequest.getTenancyTerm());
		taDocument.setCommencementDate(jsonrequest.getCommencementDate());
		taDocument.setCommencementBusinessDate(jsonrequest.getCommencementBusinessDate());
		taDocument.setExpiryDate(jsonrequest.getExpiryDate());
		taDocument.setLotNumber(jsonrequest.getLotNumber());
		taDocument.setLocation(jsonrequest.getLocation());
		taDocument.setArea(jsonrequest.getArea());
		taDocument.setAirport(jsonrequest.getAirport());
		taDocument.setTenantSigningOrder(jsonrequest.getTenantSigningOrder());
		taDocument.setCvefSigningOrder(jsonrequest.getCvefSigningOrder());
		taDocument.setTenants(jsonrequest.getTenants());
		taDocument.setStampers(jsonrequest.getStampers());
		taDocument.setCvefSigners(jsonrequest.getCvefSigners());
		taDocument.setSubsidiary(jsonrequest.getSubsidiary());
		taDocument.setReferenceId(envelopeId);
		taDocument.setLhdnStampers(jsonrequest.getLhdnStampers());
		taDocument.setLhdnSigningOrder(jsonrequest.isLhdnSigningOrder());
		taDocument.setAllSigners(jsonrequest.getAllSigners());
		String buildurl = getDousignUrl().getCreateLOAContractDocument().replace(Constant.DOCUSIGN_HOST,
				docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
		builder.queryParam("envelopeId", envelopeId);
		builder.queryParam("senderUserId", userId.getUserId());
		String url = builder.toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		ResponseEntity<Object> response = null;
		try {
			response = restTemplate.postForEntity(url, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.TA_DOCUMENT_RESPONSE, "Internal Server Error"),
					"TA Document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String newEnvelopeId = rootNode.get("envelopeId").asText();
		System.out.println("NewEnv - " + newEnvelopeId);
		if (newEnvelopeId != null) {
			buildurl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
			String envelopeDataUrl = buildurl + newEnvelopeId;
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
				System.out.println(envelopeDataResponse.getBody());
			} catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.TA_DOCUMENT_RESPONSE, "Internal Server Error"),
						"TA Document Creation Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
			}
			if (envelopeDataResponse.getBody() != null) {
				JsonNode responseData = mapper.valueToTree(envelopeDataResponse.getBody());
				JsonNode documents = responseData.path("envelope").path("envelopeDocuments");
				if (documents.isArray()) {
					for (JsonNode document : documents) {
						String documentId = document.path("documentId").asText();
						String documentName = document.path("name").asText();
						String documentbase64 = null;
//					CommonResponse base64Response = clmServiceImpl.getEnvelopeDocument(newEnvelopeId, documentId);
//					documentbase64 = base64Response.getResponse().getData().toString();
						DocumentResponse blobResponse = new DocumentResponse();
						MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentbase64,
								documentName, "text/plain");
						multipartList.add(multipartFile);
						blobResponse.setDocumentBase64(documentbase64);
						blobResponse.setName(documentName);
						blobResponse.setDocumentId(documentId);
						blobDocumentResponses.add(blobResponse);
						Document newDoc = new Document();
						newDoc.setDocumentId(documentId);
						newDoc.setName(documentName);
						versionDocDocuments.add(newDoc);
					}
				}
				documentVersion.setEnvelopeId(newEnvelopeId);
				documentVersion.setVersionOrder("1.0");
				documentVersion.setDocVersion(0);
				documentVersion.setPath(containerName + newEnvelopeId + "/V1");
				documentVersion.setDocuments(versionDocDocuments);
				documentVersion.setCreatedOn(new Date());
				documentVersionDocumentRepository.save(documentVersion);
				envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
				envelopeDocument.setEnvelopeId(newEnvelopeId);
				Date createDate = new Date();
				WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
				creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
				creator.setEnvelopeId(newEnvelopeId);
				creator.setCreatedOn(new Date());
				creator.setContractName(jsonrequest.getContractTitle());
				creator.setFlowType("TA_Create");
				List<String> pendingWith = new ArrayList<>();
				creator.setEmail(profile);
				creator.setTenantName(jsonrequest.getTenant());
				creator.setProjectId(projectId);
				creator.setStartDate(jsonrequest.getCommencementDate());
				creator.setExpiryDate(jsonrequest.getExpiryDate());
				List<ReviewerDocument> reviewers = new ArrayList<>();
				for (Reviewers reviewer : jsonrequest.getReviewers()) {
					ReviewerDocument reviewerDocument = new ReviewerDocument();
					reviewerDocument.setId(sequenceGeneratorService.generateSequence(ReviewerDocument.SEQUENCE_NAME));
					reviewerDocument.setEmail(reviewer.getEmail());
					reviewerDocument.setDocVersion(0);
					reviewerDocument.setCompleted(false);
					reviewerDocument.setEnvelopeId(newEnvelopeId);
					reviewerDocument.setCreatedOn(createDate);
					reviewerDocument.setCreatorName(name);
					reviewerDocument.setCreatedBy(profile);
					reviewerDocument.setContractName(jsonrequest.getContractTitle());
					reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
					reviewerDocument.setFlowType("TA_Review");
					reviewerDocument.setReviewerName(reviewer.getName());
					reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == 1 ? true : false);
					reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
					reviewerDocument.setExpiryDate(jsonrequest.getExpiryDate());
					reviewerDocument.setProjectId(projectId);
					if (reviewer.getRoutingOrder() == 1) {
						pendingWith.add(reviewer.getEmail());
					}
					reviewerDocumentRepository.save(reviewerDocument);
					reviewers.add(reviewerDocument);
				}
				creator.setPendingWith(pendingWith);
				workFlowCreatorDocumentRespository.save(creator);
				envelopeDocument.setCreatedOn(new Date());
				envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
				List<DocumentResponse> documentResponses = new ArrayList<>();
				for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
					DocumentResponse docResponse = new DocumentResponse();
					docResponse.setDocumentId(documentResponse.getDocumentId());
					docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
					docResponse.setName(documentResponse.getName());
					documentResponses.add(documentResponse);
				}
				String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
				JsonNode rootNode2 = mapper.readTree(json2);
				envelopeDocument.setDocuments(documentResponses);
				envelopeDocument.setStartDate(new Date());
				envelopeRepository.save(envelopeDocument);
				taDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
				taDocument.setEnvelopeId(newEnvelopeId);
				taDocumentRepository.save(taDocument);
			}
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(envelopeId);
			centralRepoDocument.setRepositoryName(Constant.TA_DOCUMENT_REPOSITORY);
			centralRepoDocumentRepository.save(centralRepoDocument);
			try {
				uploadFilesIntoBlob(multipartList, newEnvelopeId, blobDocumentResponses,
						"V" + (documentVersion.getDocVersion() + 1), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			throw new DataValidationException("create TA document failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.TA_DOCUMENT_RESPONSE, projectIdmap),
				"TA document details submitted successfully");
	}

	@Override
	public CommonResponse getTaDocumentDetailsView(String envelopeId, HttpServletRequest request)
			throws DataValidationException {

		String email = getEmailFromToken(request).getUserEmail();
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		TADocumentRequest documentDetailResponse = new TADocumentRequest();
		TADocument taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
		if (taDocument == null)
			throw new DataValidationException("Please provide the valid envelope ID", "400", HttpStatus.BAD_REQUEST);
		LooContractDocument looContractDocument = looContractDocumentRepository
				.findByEnvelopeId(taDocument.getTemplateId());
		documentDetailResponse.setReferenceNo(looContractDocument.getProjectId());
		documentDetailResponse.setContractTitle(taDocument.getContractTitle());
		documentDetailResponse.setTenant(taDocument.getTenant());
		documentDetailResponse.setTenderNo(taDocument.getTenderNo());
		documentDetailResponse.setTradingName(taDocument.getTradingName());
		documentDetailResponse.setTenancyTerm(taDocument.getTenancyTerm());
		documentDetailResponse.setCommencementDate(taDocument.getCommencementDate());
		documentDetailResponse.setCommencementBusinessDate(taDocument.getCommencementBusinessDate());
		documentDetailResponse.setExpiryDate(taDocument.getExpiryDate());
		documentDetailResponse.setLotNumber(taDocument.getLotNumber());
		documentDetailResponse.setLocation(taDocument.getLocation());
		documentDetailResponse.setAirport(taDocument.getAirport());
		documentDetailResponse.setArea(taDocument.getArea());
		documentDetailResponse.setTenantSigningOrder(taDocument.getTenantSigningOrder());
		documentDetailResponse.setCvefSigningOrder(taDocument.getCvefSigningOrder());
		documentDetailResponse.setTenants(taDocument.getTenants());
		documentDetailResponse.setCvefSigners(taDocument.getCvefSigners());
		documentDetailResponse.setStampers(taDocument.getStampers());
		documentDetailResponse.setSubsidiary(taDocument.getSubsidiary());
		documentDetailResponse.setLhdnSigningOrder(taDocument.isLhdnSigningOrder());
		documentDetailResponse.setLhdnStampers(taDocument.getLhdnStampers());
		documentDetailResponse.setCategory(taDocument.getCategory());
		documentDetailResponse
				.setTemplateName(looContractDocument != null ? looContractDocument.getContractTitle() : null);
		documentDetailResponse.setAllSigners(taDocument.getAllSigners());
		return new CommonResponse(HttpStatus.OK, new Response("TA Document Response", documentDetailResponse),
				"TA Document details Retrieved Successfully");
	}

	@Override
	public CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId)
			throws DataValidationException {
		List<DocumentVersioningResponse> response = new ArrayList<>();
		String email = getEmailFromToken(request).getUserEmail();
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		String flowType = (cacheValue != null) ? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()) : "null";
		TADocument taDocument = null;
		if (flowType.equalsIgnoreCase("LOO_Create") || flowType.equalsIgnoreCase("TA_Review")) {
			taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
			if (taDocument == null) {
				throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
			}
		}
		List<DocumentVersionDocument> documentList = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
		for (DocumentVersionDocument documentVersionDocument : documentList) {
			DocumentVersioningResponse documentVersioning = new DocumentVersioningResponse();
			documentVersioning.setEnvelopeId(documentVersionDocument.getEnvelopeId());
			documentVersioning.setOrder(documentVersionDocument.getDocVersion());
			documentVersioning.setDocumentVersion(documentVersionDocument.getVersionOrder());
			documentVersioning.setCreatedTime(documentVersionDocument.getCreatedOn());
			documentVersioning.setUpdatedTime(documentVersionDocument.getUpdatedOn());
			documentVersioning.setEnvelopeName(taDocument != null ? taDocument.getContractTitle() : null);
			response.add(documentVersioning);
		}
		Collections.sort(response, Comparator.comparingInt(DocumentVersioningResponse::getOrder).reversed());
		return new CommonResponse(HttpStatus.OK, new Response("Document Versioning Response", response),
				"Documents fetched successfully");
	}

	@Override
	public CommonResponse getCreateListForTa(HttpServletRequest request, int page, int limit, String searchText,
			String order, String orderBy, String subsidiary, String status, String signingStatus, String category)
			throws DataValidationException, UnsupportedEncodingException, JsonMappingException,
			JsonProcessingException {

		Sort sort = null;
		PageRequest pageable = PageRequest.of(page, limit);
		if (order != null && !order.isEmpty()) {
			Sort.Direction sortDirection = Sort.Direction.ASC;
			if (order.equalsIgnoreCase("desc")) {
				sortDirection = Sort.Direction.DESC;
			}
			sort = Sort.by(sortDirection, orderBy);
			pageable = PageRequest.of(page, limit, sort);
		}
		List<DocumentCreateResponse> createResponses = new ArrayList<>();
		Map<String, Object> commonResponse = new HashMap<>();
		String email = getEmailFromToken(request).getUserEmail();
		long totalCount = 0;
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		String currentRole = usersInfoDocument.getCurrentRole();
		if (!(currentRole.equalsIgnoreCase("LOO_CREATOR") || currentRole.equalsIgnoreCase("TA_CREATOR")
				|| (currentRole.equalsIgnoreCase("COMMERCIAL_ADMIN") || currentRole.equalsIgnoreCase("SUPER_ADMIN")
						|| currentRole.equalsIgnoreCase("LEGAL_ADMIN"))
				|| currentRole.equalsIgnoreCase("LEGAL_USER") || currentRole.equalsIgnoreCase("COMMERCIAL_USER"))) {
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		}
		List<TADocument> list;
		if (order == null) {
			totalCount = taDocumentRepository.findAll().size();
			list = taDocumentRepository.findAll(pageable).getContent();
		} else {
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			Query query = new Query();
			query.collation(collation);
			if (subsidiary != null && !subsidiary.isBlank()) {
				query.addCriteria(Criteria.where("subsidiary").regex(subsidiary.replaceAll("[()]", "\\\\$0"), "i"));
			}
			if (status != null && !status.isEmpty()) {
				query.addCriteria(Criteria.where("status").regex(status, "i"));
			}
			if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
			}
			if (signingStatus != null && !signingStatus.isEmpty()) {
//				query.addCriteria(
//						Criteria.where("allSigners").elemMatch(Criteria.where("recipientType").is("CVEF_Signer")
//								.and("signingStatus").regex(Pattern.compile(cvefStatus, Pattern.CASE_INSENSITIVE))));
				signingStatus = URLDecoder.decode(signingStatus, "UTF-8");
				JsonNode rootNode = mapper.readTree(signingStatus);
				String module = rootNode.get("module").asText().toUpperCase();
				String signStatus = rootNode.get("status").asText();
				switch (module) {
				case Constant.CVEF_DOCUMENT_REPOSITORY:
					query.addCriteria(Criteria.where("isCvefSignersCompleted").regex(signStatus, "i"));
					break;
				case "STAMPER":
					query.addCriteria(Criteria.where("isStampersCompleted").regex(signStatus, "i"));
					break;
				case "LHDN":
					query.addCriteria(Criteria.where("isLhdnSignersCompleted").regex(signStatus, "i"));
					break;
				}
			}
			Criteria criteria = new Criteria();
			if (searchText != null && !searchText.isEmpty()) {
				searchText = URLDecoder.decode(searchText, "UTF-8");
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("contractTitle").regex(pattern),
						Criteria.where("projectId").regex(pattern));
				query.addCriteria(criteria);
			}
			log.info("query: {}", query);
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, TADocument.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query, TADocument.class).stream().collect(Collectors.toList());
		}
		List<String> listOfEnvelopeIds = list.stream().map(document -> document.getEnvelopeId())
				.collect(Collectors.toList());
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		for (TADocument taDocument : list) {
			List<String> reviewers = new ArrayList<>();
			List<ReviewerDocument> reviewerslist = reviewerList.stream()
					.filter(document -> document.getEnvelopeId().equals(taDocument.getEnvelopeId()))
					.collect(Collectors.toList());
			if (reviewerslist != null) {
				List<ReviewerDocument> reviewerlist = reviewerslist.stream().filter(m -> m.getEndDate() == null)
						.collect(Collectors.toList());
				if (reviewerslist.stream().mapToInt(ReviewerDocument::getRoutingOrder).distinct().count() == 1) {
					if (!reviewerlist.isEmpty()) {
						reviewers.addAll(
								reviewerlist.stream().map(ReviewerDocument::getEmail).collect(Collectors.toList()));
					}
				} else {
					if (!reviewerlist.isEmpty()) {
						reviewerlist.sort(Comparator.comparing(ReviewerDocument::getRoutingOrder));
						reviewers.add(reviewerlist.get(0).getEmail());
					}
				}
			}
			DocumentCreateResponse resp = DocumentCreateResponse.builder().contractTitle(taDocument.getContractTitle())
					.createdOn(taDocument.getCreatedOn()).projectId(taDocument.getProjectId()).ownersEmail(email)
					.senderName(getEmailFromToken(request).getUserName()).envelopeId(taDocument.getEnvelopeId())
					.status(taDocument.getStatus()).pendingWith(reviewers)
					.commencementDate(taDocument.getCommencementDate()).expiryDate(taDocument.getExpiryDate())
					.subsidiary(taDocument.getSubsidiary()).tenant(taDocument.getTenant()).build();
			createResponses.add(resp);
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", createResponses);
		return new CommonResponse(HttpStatus.OK, new Response("CreateListForTaResponse", commonResponse),
				"TA details fetched successfully");
	}

	// utility methods
	public String generateProjectId(String value) {
		while (value.length() < 6) {
			value = '0' + value;
		}
		return value;
	}

	private void uploadFilesIntoBlob(List<MultipartFile> multipartFiles, String envelopeId,
			List<DocumentResponse> documentResponses, String version, List<String> documentIds)
			throws URISyntaxException, StorageException, IOException {
		String path = envelopeId;
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		for (int i = 0; i < multipartFiles.size(); i++) {
			MultipartFile file = multipartFiles.get(i);
			String documentId = (version.equals("V1")) ? documentResponses.get(i).getDocumentId() : documentIds.get(i);
			CloudBlockBlob blob = container
					.getBlockBlobReference(containerName + path + "/" + version + "/" + documentId);
			blob.getProperties().setContentType("application/pdf");
			try (InputStream inputStream = file.getInputStream()) {
				blob.upload(inputStream, file.getSize());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private UserDetails getEmailFromToken(HttpServletRequest request) {
		String token = request.getHeader(Constant.HEADER_STRING);
		String xAuthProvider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		String email = null;
		UserDetails user = new UserDetails();
		if (xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			String name = jwt.getClaim("name").asString();
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
			user.setUserEmail(email);
			user.setUserName(name);
		}
		return user;
	}

	@Override
	public CommonResponse newAddTaDocument(String json, String templateId, String email, String name)
			throws DataValidationException, IOException, TemplateException, MessagingException {
		try {
			TADocumentRequest jsonrequest = mapper.readValue(json, TADocumentRequest.class);
			DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
			DocumentVersionDocument documentVersion = new DocumentVersionDocument();
			documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
			jsonrequest.setUserEmail(email);
			TADocument taDocument = new TADocument();
			EnvelopeDocument envelopeDocument = new EnvelopeDocument();
			Long id = sequenceGeneratorService.generateSequence(TADocument.SEQUENCE_NAME);
			String projectId = generateProjectId(id + "");
			Map<String, String> envelopeIdmap = new HashMap<>();
			taDocument.setId(id);
			taDocument.setProjectId(projectId);
			taDocument.setBuID("BUID");
			taDocument.setOpID("SAASPE");
			taDocument.setCreatedOn(new Date());
			taDocument.setCreatedBy(email);
			taDocument.setFlowCompleted(false);
			taDocument.setStatus(jsonrequest.getStatus());
			taDocument.setVersion("1.0");
			taDocument.setOrder(0);
			taDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
			taDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
			taDocument.setContractTitle(jsonrequest.getContractTitle());
			taDocument.setTemplateId(templateId);
			taDocument.setTenant(jsonrequest.getTenant());
			taDocument.setTenderNo(jsonrequest.getTenderNo());
			taDocument.setReferenceNo(jsonrequest.getReferenceNo());
			taDocument.setTradingName(jsonrequest.getTradingName());
			taDocument.setTenancyTerm(jsonrequest.getTenancyTerm());
			taDocument.setCommencementDate(jsonrequest.getCommencementDate());
			taDocument.setCommencementBusinessDate(jsonrequest.getCommencementBusinessDate());
			taDocument.setExpiryDate(jsonrequest.getExpiryDate());
			taDocument.setLotNumber(jsonrequest.getLotNumber());
			taDocument.setLocation(jsonrequest.getLocation());
			taDocument.setArea(jsonrequest.getArea());
			taDocument.setAirport(jsonrequest.getAirport());
			taDocument.setTenantSigningOrder(jsonrequest.getTenantSigningOrder());
			taDocument.setCvefSigningOrder(jsonrequest.getCvefSigningOrder());
			taDocument.setTenants(jsonrequest.getTenants());
			taDocument.setStampers(jsonrequest.getStampers());
			taDocument.setCvefSigners(jsonrequest.getCvefSigners());
			taDocument.setSubsidiary(jsonrequest.getSubsidiary());
			taDocument.setReferenceId(templateId);
			taDocument.setLhdnStampers(jsonrequest.getLhdnStampers());
			taDocument.setLhdnSigningOrder(jsonrequest.isLhdnSigningOrder());
			List<AllSigners> allSignersList = new ArrayList<>();
			for (AllSigners s : jsonrequest.getAllSigners()) {
				s.setSigningStatus("sent");
				allSignersList.add(s);
			}
			taDocument.setAllSigners(allSignersList);
			taDocument.setIsSignersCompleted("sent");
			taDocument.setIsCvefSignersCompleted("sent");
			taDocument.setIsStampersCompleted("sent");
			taDocument.setIsTenantsCompleted("sent");
			taDocument.setIsLhdnSignersCompleted("sent");
			taDocument.setCategory(jsonrequest.getCategory());
			taDocument.setWatcherEmailStatus(false);
			jsonrequest.setModuleName(Constant.TA_DOCUMENT_REPOSITORY);
			jsonrequest.setContractNumber(jsonrequest.getTenderNo());
			jsonrequest.setContractName(jsonrequest.getContractTitle());	
			String buildurl = getDousignUrl().getNewCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST,
					docusignHost.trim());
			log.info("Calling envelope create API");
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
			builder.queryParam("templateId", templateId);
			builder.queryParam("userId", userId.getUserId());
			String url = builder.toUriString();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
			ResponseEntity<Object> response = null;
			try {
				response = restTemplate.postForEntity(url, requestEntity, Object.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				log.info("Inside catch block envelope create API");
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.TA_DOCUMENT_RESPONSE, "Internal Server Error"),
						"TA Document Creation Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
			}
			log.info("Success in envelope create API");
			String json1 = mapper.writeValueAsString(response.getBody());
			JsonNode rootNode = mapper.readTree(json1);
			String newEnvelopeId = rootNode.get("envelopeId").asText();
			envelopeIdmap.put("envelopeId", newEnvelopeId);
			if (newEnvelopeId != null) {
				log.info("Calling envelope by ID API");
				buildurl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
				String envelopeDataUrl = buildurl + newEnvelopeId;
				HttpEntity<?> httpEntity = new HttpEntity<>(headers);
				ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
				try {
					envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
							EnvelopeResponse.class);
					System.out.println(envelopeDataResponse.getBody());
				} catch (HttpClientErrorException.BadRequest ex) {
					log.info("Inside catch-block envelopeById API");
					ex.printStackTrace();
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response(Constant.TA_DOCUMENT_RESPONSE, "Internal Server Error"),
							"TA Document Creation Failed");
				} catch (RestClientException ex) {
					ex.printStackTrace();
					if (ex instanceof ResourceAccessException) {
						return new CommonResponse(HttpStatus.NOT_FOUND,
								new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()),
								"Unable to access remote resources.Please check the connectivity");
					}
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response(Constant.TA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
				}
				log.info("Success in envelopeById API");
				documentVersion.setEnvelopeId(newEnvelopeId);
				documentVersion.setVersionOrder("1.0");
				documentVersion.setDocVersion(0);
				documentVersion.setPath(containerName + newEnvelopeId + "/V1");
				documentVersion.setCreatedOn(new Date());
				log.info("Saving DocumentVersion Document");
				documentVersionDocumentRepository.save(documentVersion);
				envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
				envelopeDocument.setEnvelopeId(newEnvelopeId);
				Date createDate = new Date();
				WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
				creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
				creator.setEnvelopeId(newEnvelopeId);
				creator.setCreatedOn(new Date());
				creator.setContractName(jsonrequest.getContractTitle());
				creator.setFlowType("TA_Create");
				List<String> pendingWith = new ArrayList<>();
				creator.setEmail(email);
				creator.setTenantName(jsonrequest.getTenant());
				creator.setProjectId(projectId);
				creator.setStartDate(jsonrequest.getCommencementDate());
				creator.setExpiryDate(jsonrequest.getExpiryDate());
				List<ReviewerDocument> reviewers = new ArrayList<>();
				for (Reviewers reviewer : jsonrequest.getReviewers()) {
					ReviewerDocument reviewerDocument = new ReviewerDocument();
					reviewerDocument.setId(sequenceGeneratorService.generateSequence(ReviewerDocument.SEQUENCE_NAME));
					reviewerDocument.setEmail(reviewer.getEmail());
					reviewerDocument.setDocVersion(0);
					reviewerDocument.setCompleted(false);
					reviewerDocument.setEnvelopeId(newEnvelopeId);
					reviewerDocument.setCreatedOn(createDate);
					reviewerDocument.setCreatorName(name);
					reviewerDocument.setCreatedBy(email);
					reviewerDocument.setContractName(jsonrequest.getContractTitle());
					reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
					reviewerDocument.setFlowType("TA_Review");
					reviewerDocument.setReviewerName(reviewer.getName());
					reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == 1 ? true : false);
					reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
					reviewerDocument.setExpiryDate(jsonrequest.getExpiryDate());
					reviewerDocument.setProjectId(projectId);
					reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
					reviewerDocument.setStatus(jsonrequest.getStatus());
					reviewerDocument.setTenant(jsonrequest.getTenant());
					if (reviewer.getRoutingOrder() == 1) {
						pendingWith.add(reviewer.getEmail());
						mailSenderService.sendRequestForReviewMail(newEnvelopeId, reviewer.getEmail(),
								reviewer.getName(), jsonrequest.getContractTitle(), taDocument.getVersion(),
								taDocument.getCreatedBy(), "commercial", Constant.TA_MODULE);
					}
					reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
					log.info("Saving ReviewerDocument");
					reviewerDocumentRepository.save(reviewerDocument);
					reviewers.add(reviewerDocument);
				}
				creator.setPendingWith(pendingWith);
				log.info("Saving Workflow creator Document");
				workFlowCreatorDocumentRespository.save(creator);
				envelopeDocument.setCreatedOn(new Date());
				envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
				String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
				JsonNode rootNode2 = mapper.readTree(json2);
				envelopeDocument.setStartDate(new Date());
				log.info("Saving Envelope Document");
				envelopeRepository.save(envelopeDocument);
				taDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
				taDocument.setEnvelopeId(newEnvelopeId);
				log.info("Saving TA Document");
				taDocumentRepository.save(taDocument);

				CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
				centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
				centralRepoDocument.setEnvelopeId(newEnvelopeId);
				centralRepoDocument.setRepositoryName(Constant.TA_DOCUMENT_REPOSITORY);
				log.info("Saving CentralRepo Document");
				centralRepoDocumentRepository.save(centralRepoDocument);

			} else {
				throw new DataValidationException("create TA document failed, try again!", null, null);
			}
			return new CommonResponse(HttpStatus.CREATED, new Response(Constant.TA_DOCUMENT_RESPONSE, envelopeIdmap),
					"TA document details submitted successfully");
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof MismatchedInputException) {
				throw new DataValidationException("Fields mismatch or malformed payload. Please try again!", "400",
						HttpStatus.BAD_REQUEST);
			}
			throw new DataValidationException(e.getMessage(), "400", HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	public CommonResponse setExpiringEnvelope() {
		List<String> envelopeList = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, -29);
		Date endDate = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, -12);
		List<TADocument> expiringDocuments = taDocumentRepository.findByCreatedOnBeforeAndStatus(endDate, "created");
		log.info("End Date :{}", endDate);
		if (!expiringDocuments.isEmpty()) {
			expiringDocuments.stream().forEach(e -> log.info(e.getEnvelopeId()));
			for (TADocument document : expiringDocuments) {
				log.info("Updating TA Document Status");
				log.info("Updating Status :{}", document.getEnvelopeId());
				document.setStatus("expired");
				envelopeList.add(document.getEnvelopeId());
				taDocumentRepository.save(document);
			}
			return new CommonResponse(HttpStatus.OK, new Response("Document Update Response", envelopeList),
					"Document upload Successful");
		} else
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Update Response", "No Documents found to update"),
					"Document upload Unsuccessful");
	}

	public boolean restrictionApi(UsersInfoDocument usersInfoDocument, String envelopeId) {
		CentralRepoDocument findRepo = centralRepoDocumentRepository.findByEnvelopeId(envelopeId);
		List<String> pcdEnvelope = Arrays.asList("LOA", "LOA_CONTRACT");
		List<String> commercialEnvelope = Arrays.asList("LOO", "CMU", "TA");
		String currentRole = usersInfoDocument.getCurrentRole();
		String reponame = findRepo.getRepositoryName();
		boolean flag = false;
		if (pcdEnvelope.contains(reponame) && Constant.PCD_ADMIN_ACCESS_LIST.contains(currentRole)) {
			flag = true;
		} else if (commercialEnvelope.contains(reponame)
				&& Constant.COMMERCIAL_ADMIN_ACCESS_LIST.contains(currentRole)) {
			flag = true;
		}
		return flag;
	}

}
