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
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import saaspe.clm.document.CreateTemplate;
import saaspe.clm.document.DocumentVersionDocument;
import saaspe.clm.document.EnvelopeDocument;
import saaspe.clm.document.LoaContractDocument;
import saaspe.clm.document.LoaDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
import saaspe.clm.docusign.model.DocumentVersioningResponse;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.AllSigners;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Currency;
import saaspe.clm.model.Document;
import saaspe.clm.model.DocumentCreateResponse;
import saaspe.clm.model.DocumentResponse;
import saaspe.clm.model.DocusignUrls;
import saaspe.clm.model.DocusignUserCache;
import saaspe.clm.model.EnvelopeResponse;
import saaspe.clm.model.LoaDocumentDetailResponse;
import saaspe.clm.model.LoaDocumentDetailsViewResponse;
import saaspe.clm.model.LoaDocumentListPagination;
import saaspe.clm.model.LoaDocumentRequest;
import saaspe.clm.model.LoaDocumentResponse;
import saaspe.clm.model.Response;
import saaspe.clm.model.Reviewers;
import saaspe.clm.model.UserDetails;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.CreateTemplateRepository;
import saaspe.clm.repository.DocumentVersionDocumentRepository;
import saaspe.clm.repository.EnvelopeRepository;
import saaspe.clm.repository.LoaContractDocumentRepository;
import saaspe.clm.repository.LoaDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.LOAService;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.utills.AllRoleFlowMapper;
import saaspe.clm.utills.Base64ToMultipartFileConverter;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

@Service
public class LOAServiceImpl implements LOAService {

	private static Logger log = LoggerFactory.getLogger(LOAServiceImpl.class);

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

	@Value("${sendgrid.domain.orgname}")
	private String senderName;

	@Value("${spring.mail.username}")
	private String fromMail;

	@Autowired
	private EnvelopeRepository envelopeRepository;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Autowired
	private LoaDocumentRepository loaDocumentRepository;

	@Autowired
	private CreateTemplateRepository createTemplateRepository;

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private DocumentVersionDocumentRepository documentVersionDocumentRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository workFlowCreatorDocumentRespository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

	@Autowired
	private LoaContractDocumentRepository loaContractDocumentRepository;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Autowired
	private MailSenderService mailSenderService;

	@Autowired
	private CloudBlobClient cloudBlobClient;

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

	@Value("${spring.host}")
	private String host;

	Random random = new Random();

	private final MongoTemplate mongoTemplate;

	public LOAServiceImpl(MongoTemplate mongoTemplate) {
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

	@Override
	@Transactional
	public CommonResponse addLoaDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId, String profile, String name)
			throws DataValidationException, JsonProcessingException, IOException, URISyntaxException, StorageException,
			TemplateException, MessagingException {
		LoaDocumentRequest jsonrequest = mapper.readValue(json, LoaDocumentRequest.class);
		int minRange = 1000;
		int maxRange = 9999;
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();
		if (createId == null && createDocumentFiles == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
					"Provide existing documentId or upload document to send envelope");
		}
		List<Document> versionDocDocuments = new ArrayList<>();
		List<DocumentResponse> BlobdocumentResponses = new ArrayList<>();
		List<MultipartFile> multipartFiles = new ArrayList<>();
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + profile);
		String buildurl = getDousignUrl().getListTemplateById().replace(Constant.DOCUSIGN_HOST, docusignHost);
		buildurl = buildurl.replace("{templateId}", templateId);
		HttpHeaders headersTemplate = new HttpHeaders();
		headersTemplate.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntityy = new HttpEntity<>(headersTemplate);
		try {
			ResponseEntity<Object> responseEntity = restTemplate.exchange(buildurl, HttpMethod.GET, requestEntityy,
					Object.class);
			if (responseEntity.getBody() == null) {
				throw new DataValidationException("Template id provided is not in the existing list", "400",
						HttpStatus.BAD_REQUEST);
			}
			JsonNode responseData = mapper.valueToTree(responseEntity.getBody());
			JsonNode documents = responseData.path("data").path("documents");
			if (documents.isArray()) {
				for (JsonNode document : documents) {
					String documentName = document.path("name").asText();
					String documentId = document.path("documentId").asText();
					String documentBase64 = document.path("documentBase64").asText();
					DocumentResponse blobResponse = new DocumentResponse();
					MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentBase64, documentName,
							"text/plain");
					multipartFiles.add(multipartFile);
					blobResponse.setDocumentBase64(documentBase64);
					blobResponse.setName(documentName);
					blobResponse.setDocumentId(documentId);
					BlobdocumentResponses.add(blobResponse);
					Document newDoc = new Document();
					newDoc.setDocumentId(documentId);
					newDoc.setName(documentName);
					newDoc.setUploaded(false);
					versionDocDocuments.add(newDoc);
				}
			}
		} catch (HttpClientErrorException e) {
			e.printStackTrace();
			throw new DataValidationException(Constant.INTERNAL_SERVER_ERROR, "400", HttpStatus.BAD_REQUEST);
		}
		ArrayList<saaspe.clm.model.Document> documentRequests = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				try {
					documentRequest
							.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
					if (createDocumentFile.getOriginalFilename() != null) {
						documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
								createDocumentFile.getOriginalFilename().lastIndexOf(".")));
					}
					documentRequest.setCategory(Constant.CREATE_FILE);
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					documentRequest.setDocumentId(String.valueOf(randomNumber));
					documentRequests.add(documentRequest);
					Document newDoc = new Document();
					newDoc.setDocumentId(String.valueOf(randomNumber));
					newDoc.setName(createDocumentFile.getOriginalFilename().substring(0,
							createDocumentFile.getOriginalFilename().indexOf(".")));
					newDoc.setUploaded(false);
					versionDocDocuments.add(newDoc);
					DocumentResponse blobResponse = new DocumentResponse();
					multipartFiles.add(createDocumentFile);
					blobResponse.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
					blobResponse.setName(createDocumentFile.getOriginalFilename().substring(0,
							createDocumentFile.getOriginalFilename().indexOf(".")));
					blobResponse.setDocumentId(String.valueOf(randomNumber));
					BlobdocumentResponses.add(blobResponse);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!deleteIdsList.isEmpty()) {
			for (String deleteid : deleteIdsList) {
				versionDocDocuments.removeIf(p -> p.getDocumentId().equalsIgnoreCase(deleteid));
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				documentRequest.setDocumentId(deleteid);
				documentRequest.setCategory(Constant.DELETE);
				documentRequests.add(documentRequest);
			}
		}
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(profile);
		LoaDocument loaContractDocument = new LoaDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(LoaDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> projectIdmap = new HashMap<>();
		projectIdmap.put("projectId", projectId);
		loaContractDocument.setId(id);
		loaContractDocument.setFlowCompleted(false);
		loaContractDocument.setCompleted(false);
		loaContractDocument.setTemplateId(templateId);
		loaContractDocument.setProjectId(projectId);
		loaContractDocument.setProjectName(jsonrequest.getProjectName());
		loaContractDocument.setVendorName(jsonrequest.getVendorName());
		loaContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		loaContractDocument.setCompletionDate(jsonrequest.getCompletionDate());
		loaContractDocument.setBuID("BUID");
		loaContractDocument.setOpID("SAASPE");
		loaContractDocument.setCreatedOn(new Date());
		loaContractDocument.setCreatedBy(profile);
		loaContractDocument.setContractTenure(jsonrequest.getContractTenure());
		loaContractDocument.setStatus(jsonrequest.getStatus());
		loaContractDocument.setVersion("1.0");
		loaContractDocument.setOrder(0);
		loaContractDocument.setUniqueString(generateRandomString(6));
		loaContractDocument.setReferenceId(templateId);
		loaContractDocument.setReferenceType("Template");
		loaContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		loaContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		Currency currency = new Currency(jsonrequest.getCurrency().getCurrencyCode(),
				jsonrequest.getCurrency().getTotalCost(), jsonrequest.getCurrency().getTax());
		loaContractDocument.setCurrency(currency);
		loaContractDocument.setVendors(jsonrequest.getVendors());
		loaContractDocument.setFilesUploaded(false);
		loaContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		loaContractDocument.setAllSigners(jsonrequest.getAllSigners());
		loaContractDocument.setVendorSigningOrder(jsonrequest.isVendorSigningOrder());
		loaContractDocument.setLhdnStampers(jsonrequest.getLhdnStampers());
		loaContractDocument.setLhdnSigningOrder(jsonrequest.getLhdnSigningOrder());
		loaContractDocument.setContractNumber(jsonrequest.getContractNumber());
		buildurl = getDousignUrl().getCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
		builder.queryParam(Constant.TEMPLATE_ID, templateId);
		builder.queryParam(Constant.USER_ID_ERROR_KEY, userId.getUserId());
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
					new Response(Constant.LOA_DOCUMENT_RESPONSE, Constant.INTERNAL_SERVER_ERROR),
					"LOA document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String envelopeId = rootNode.get(Constant.ENVELOPE_ID).asText();
		System.out.println(envelopeId);
		loaContractDocument.setEnvelopeId(envelopeId);
		loaDocumentRepository.save(loaContractDocument);
		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		documentVersion.setEnvelopeId(envelopeId);
		documentVersion.setVersionOrder("1.0");
		documentVersion.setDocVersion(0);
		documentVersion.setCreatedOn(new Date());
		documentVersion.setPath(containerName + envelopeId + "/V" + (documentVersion.getDocVersion() + 1));
		documentVersion.setDocuments(versionDocDocuments);
		documentVersionDocumentRepository.save(documentVersion);
		if (envelopeId != null) {
			buildurl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
			String envelopeDataUrl = buildurl + envelopeId;
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
						"LOA Document Creation Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Document Upload Response", new ArrayList<>()), ex.getLocalizedMessage());
			}
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(envelopeId);
			Date createDate = new Date();
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(envelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getProjectName());
			creator.setFlowType("LOA_Create");
			creator.setProjectId(projectId);
			creator.setStatus(jsonrequest.getStatus());
			creator.setSubsidiary(jsonrequest.getSubsidiary());
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(profile);
			boolean hasSameRoutingOrder = jsonrequest.getReviewers().stream().map(Reviewers::getRoutingOrder).distinct()
					.count() == 1;
			int firstRoutingOrder = jsonrequest.getReviewers().stream().map(Reviewers::getRoutingOrder).sorted()
					.findFirst().get();
			for (Reviewers reviewer : jsonrequest.getReviewers()) {
				ReviewerDocument reviewerDocument = new ReviewerDocument();
				reviewerDocument.setId(sequenceGeneratorService.generateSequence(ReviewerDocument.SEQUENCE_NAME));
				reviewerDocument.setEmail(reviewer.getEmail());
				reviewerDocument.setDocVersion(0);
				reviewerDocument.setCompleted(false);
				reviewerDocument.setEnvelopeId(envelopeId);
				reviewerDocument.setCreatedOn(createDate);
				reviewerDocument.setCreatorName(name);
				reviewerDocument.setCreatedBy(profile);
				reviewerDocument.setContractName(jsonrequest.getProjectName());
				reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				reviewerDocument.setFlowType("LOA_Review");
				reviewerDocument.setReviewerName(reviewer.getName());
				reviewerDocument.setStatus(jsonrequest.getStatus());
				reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
				reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
				reviewerDocument.setCompletionDate(jsonrequest.getCompletionDate());
				reviewerDocument.setProjectId(projectId);
				reviewerDocument.setVendorName(jsonrequest.getVendorName());
				reviewerDocument.setContractNumber(jsonrequest.getContractNumber());
				if (reviewer.getRoutingOrder() == firstRoutingOrder) {
					pendingWith.add(reviewer.getEmail());
					mailSenderService.sendRequestForReviewMail(envelopeId, reviewer.getEmail(), reviewer.getName(),
							jsonrequest.getProjectName(), loaContractDocument.getVersion(),
							loaContractDocument.getCreatedBy(), "pcd", Constant.LOA_MODULE);
				}
				reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
				reviewerDocumentRepository.save(reviewerDocument);
			}
			creator.setPendingWith(pendingWith);
			workFlowCreatorDocumentRespository.save(creator);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
			try {
				uploadFilesIntoBlob(multipartFiles, envelopeId, BlobdocumentResponses,
						"V" + (documentVersion.getDocVersion() + 1), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setStartDate(new Date());
			envelopeRepository.save(envelopeDocument);
			loaContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			loaContractDocument.setSenderName(rootNode2.path(Constant.SENDER).path(Constant.USERNAME).asText());
			loaDocumentRepository.save(loaContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(envelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOA_DOCUMENT_REPOSITORY);
			centralRepoDocumentRepository.save(centralRepoDocument);
		} else {
			throw new DataValidationException("LOA creation failed, try again!", "400", HttpStatus.BAD_REQUEST);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.LOA_DOCUMENT_RESPONSE, projectIdmap),
				"Loa document details submitted successfully");
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

	@Override
	public CommonResponse getListOfLoaDocument(int page, int limit, HttpServletRequest request, String status,
			String searchText, String order, String orderBy) throws DataValidationException, JsonProcessingException {
		Sort sort = null;
		PageRequest pageable = null;
		if (order != null && !order.isEmpty()) {
			Sort.Direction sortDirection = Sort.Direction.ASC;
			if (order.equalsIgnoreCase("desc")) {
				sortDirection = Sort.Direction.DESC;
			}
			sort = Sort.by(sortDirection, orderBy);
			pageable = PageRequest.of(page, limit, sort);
		}
		String token = request.getHeader(Constant.HEADER_STRING);
		String xAuthProvider = request.getHeader("X-Auth-Provider");
		String email = null;
		long totalCount = 0;
		if (xAuthProvider.equalsIgnoreCase("azure")) {
			DecodedJWT jwt = JWT.decode(token.replace("Bearer ", ""));
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
		}
		List<LoaDocumentResponse> loaDocumentEntityList = new ArrayList<>();
		if (status != null) {
			if (Constant.CLM_ENVELOPE_STATUS.stream().noneMatch(status::equalsIgnoreCase)) {
				throw new DataValidationException(
						"Status should be in the following values completed, created, declined, delivered, sent, signed, voided",
						null, null);
			}
			List<LoaDocument> list;
			if (order == null) {
				pageable = PageRequest.of(page, limit);
				totalCount = loaDocumentRepository.countByStatusAndSenderEmail(status, email);
				list = loaDocumentRepository.findAllLoaDocumentsByStatus(pageable, status, email);

			} else {
				Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
				if (searchText != null && !searchText.isEmpty()) {
					totalCount = loaDocumentRepository.countByCollatedFieldByStatus(orderBy, "^" + searchText, email,
							status, collation);
					list = loaDocumentRepository.findByCollatedFieldByStatus(orderBy, "^" + searchText, email, status,
							pageable, collation);
				} else {
					totalCount = loaDocumentRepository.countByCollatedFieldByStatus(orderBy, "^", email, status,
							collation);
					list = loaDocumentRepository.findByCollatedFieldByStatus(orderBy, "^", email, status, pageable,
							collation);
				}
			}
			for (LoaDocument loadocument : list) {
				EnvelopeDocument envelope = envelopeRepository.findByEnvelopeId(loadocument.getEnvelopeId());
				String json1 = mapper.writeValueAsString(envelope.getEnvelope());
				JsonNode rootNode = mapper.readTree(json1);
				LoaDocumentResponse loaDocumentResponse = new LoaDocumentResponse();
				loaDocumentResponse.setProjectId(loadocument.getProjectId());
				loaDocumentResponse.setProjectName(loadocument.getProjectName());
				loaDocumentResponse.setTemplateId(loadocument.getTemplateId());
				loaDocumentResponse.setEnvelopeId(loadocument.getEnvelopeId());
				loaDocumentResponse.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				loaDocumentResponse.setSenderMail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
				loaDocumentResponse.setStatus(rootNode.path("status").asText());
				loaDocumentResponse.setCommencementDate(loadocument.getCommencementDate());
				loaDocumentResponse.setCompletionDate(loadocument.getCompletionDate());
				loaDocumentResponse.setContractNumber(loadocument.getContractNumber());
				loaDocumentEntityList.add(loaDocumentResponse);
			}
		} else {
			List<LoaDocument> list;
			if (order == null) {
				pageable = PageRequest.of(page, limit);
				totalCount = loaDocumentRepository.countBySenderEmail(email);
				list = loaDocumentRepository.findAllLoaDocuments(email, pageable);
			} else {
				Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
				if (searchText != null && !searchText.isEmpty()) {
					totalCount = loaDocumentRepository.countByCollatedField(orderBy, "^" + searchText, email,
							collation);
					list = loaDocumentRepository.findByCollatedField(orderBy, "^" + searchText, email, pageable,
							collation);
				} else {
					totalCount = loaDocumentRepository.countByCollatedField(orderBy, "^", email, collation);
					list = loaDocumentRepository.findByCollatedField(orderBy, "^", email, pageable, collation);
				}
			}
			for (LoaDocument loaDocument : list) {
				EnvelopeDocument envelopedocuemnt = envelopeRepository.findByenvelopeId(loaDocument.getEnvelopeId());
				LoaDocumentResponse loaDocumentResponse = new LoaDocumentResponse();
				loaDocumentResponse.setProjectId(loaDocument.getProjectId());
				loaDocumentResponse.setTemplateId(loaDocument.getTemplateId());
				loaDocumentResponse.setProjectName(loaDocument.getProjectName());
				loaDocumentResponse.setEnvelopeId(loaDocument.getEnvelopeId());
				loaDocumentResponse.setCommencementDate(loaDocument.getCommencementDate());
				loaDocumentResponse.setCompletionDate(loaDocument.getCompletionDate());
				if (envelopedocuemnt != null) {
					String json1 = mapper.writeValueAsString(envelopedocuemnt.getEnvelope());
					JsonNode rootNode = mapper.readTree(json1);
					loaDocumentResponse.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
					loaDocumentResponse.setSenderMail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
					loaDocumentResponse.setStatus(rootNode.path("status").asText());

					loaDocument.setSenderEmail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
				} else {
					loaDocumentResponse.setSenderName(null);
					loaDocumentResponse.setSenderMail(null);
					loaDocumentResponse.setStatus(null);
					loaDocumentResponse.setCommencementDate(null);
					loaDocumentResponse.setCompletionDate(null);
				}
				loaDocumentEntityList.add(loaDocumentResponse);
			}
		}
		LoaDocumentListPagination data = new LoaDocumentListPagination(totalCount, loaDocumentEntityList);
		Response responseData = new Response(Constant.LOA_DOCUMENT_RESPONSE, data);
		return new CommonResponse(HttpStatus.OK, responseData, "LOA Document details fetched successfully");
	}

	@Override
	@Transactional
	public CommonResponse updateLoaDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles,
			String[] update_id, String[] delete_id, String envId, String email) throws Exception {
		List<MultipartFile> multipartFiles = new ArrayList<>();
		List<String> DocumentIds = new ArrayList<>();
		LoaDocumentRequest jsonrequest = new LoaDocumentRequest();
		int minRange = 1000;
		int maxRange = 9999;
		Random random = new Random();
		List<String> deleteIdsList = delete_id != null ? Arrays.asList(delete_id) : new ArrayList<>();
		List<String> updatedIdsList = update_id != null ? Arrays.asList(update_id) : new ArrayList<>();
		ArrayList<saaspe.clm.model.Document> documentRequests = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				try {
					documentRequest
							.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
					if (createDocumentFile.getOriginalFilename() != null) {
						documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
								createDocumentFile.getOriginalFilename().indexOf(".")));
					}
					documentRequest.setCategory("createFile");
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					documentRequest.setDocumentId(String.valueOf(randomNumber));
					MultipartFile multipartFile = Base64ToMultipartFileConverter
							.convert(documentRequest.getDocumentBase64(), documentRequest.getName(), "text/plain");
					DocumentIds.add(String.valueOf(randomNumber));
					multipartFiles.add(multipartFile);
					documentRequests.add(documentRequest);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!deleteIdsList.isEmpty()) {
			for (String deleteid : deleteIdsList) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				documentRequest.setDocumentId(deleteid);
				documentRequest.setCategory("delete");
				documentRequests.add(documentRequest);
			}
		}
		if (!updatedIdsList.isEmpty()) {
			if (updateDocumentFiles != null) {
				int i = 0;
				for (MultipartFile createDocumentFile : updateDocumentFiles) {
					saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
					try {
						documentRequest
								.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
						if (createDocumentFile.getOriginalFilename() != null) {
							documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
									createDocumentFile.getOriginalFilename().indexOf(".")));
						}
						documentRequest.setCategory("updateFile");
						documentRequest.setDocumentId(updatedIdsList.get(i));
						DocumentIds.add(updatedIdsList.get(i));
						MultipartFile multipartFile = Base64ToMultipartFileConverter
								.convert(documentRequest.getDocumentBase64(), documentRequest.getName(), "text/plain");
						multipartFiles.add(multipartFile);
						documentRequests.add(documentRequest);
					} catch (IOException e) {
						e.printStackTrace();
					}
					i++;
				}
			}
		}
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(email);
		List<DocumentVersionDocument> documentVersions = documentVersionDocumentRepository.findByEnvelopeId(envId);
		DocumentVersionDocument currentDocument = documentVersions.stream()
				.max(Comparator.comparingInt(doc -> Integer.parseInt(doc.getVersionOrder().replace(".", ""))))
				.orElseThrow(() -> new DataValidationException("Document Version Document doesn't exists", "400",
						HttpStatus.BAD_REQUEST));
		DocumentVersionDocument newdoc = new DocumentVersionDocument();
		newdoc.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		newdoc.setVersionOrder(generateVersion(currentDocument.getDocVersion() + 1));
		newdoc.setEnvelopeId(envId);
		newdoc.setDocVersion((currentDocument.getDocVersion() + 1));
		newdoc.setPath(containerName + envId + "/V" + (newdoc.getDocVersion() + 1));
		newdoc.setComments(null);
		newdoc.setDocuments(currentDocument.getDocuments());
		newdoc.setCreatedOn(currentDocument.getCreatedOn());
		newdoc.setUpdatedOn(new Date());
		List<Document> newdocuments = new ArrayList<>();
		newdocuments.addAll(newdoc.getDocuments());
		for (Document document : documentRequests) {
			if (document.getCategory().equalsIgnoreCase("createFile")) {
				Document createDocument = new Document();
				createDocument.setName(document.getName());
				createDocument.setDocumentId(document.getDocumentId());
				newdocuments.add(createDocument);
			} else if (document.getCategory().equalsIgnoreCase(Constant.DELETE)) {
				newdocuments.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
			} else if (document.getCategory().equalsIgnoreCase("updateFile")) {
				newdocuments.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
				Document updateDocument = new Document();
				updateDocument.setName(document.getName());
				updateDocument.setDocumentId(document.getDocumentId());
				newdocuments.add(updateDocument);
			}
		}
		newdoc.setDocuments(newdocuments);
		documentVersionDocumentRepository.save(newdoc);
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		for (Document document : currentDocument.getDocuments()) {
			if (delete_id != null) {
				if (Arrays.asList(delete_id).contains(document.getDocumentId())) {
					continue;
				}
			}
			if (update_id != null) {
				if (Arrays.asList(update_id).contains(document.getDocumentId())) {
					continue;
				}
			}
			String path = currentDocument.getPath() + "/" + document.getDocumentId();
			CloudBlockBlob blob = container.getBlockBlobReference(path);
			InputStream inputStream = blob.openInputStream();
			MultipartFile multipartFile = Base64ToMultipartFileConverter
					.convert(convertBlobInputStreamToBase64(inputStream), document.getName(), "text/plain");
			multipartFiles.add(multipartFile);
			DocumentIds.add(document.getDocumentId());
		}
		List<ReviewerDocument> reviewersList = reviewerDocumentRepository.findByEnvelopeId(envId);
		boolean allSameRoutingOrder = reviewersList.stream().mapToInt(ReviewerDocument::getRoutingOrder).distinct()
				.count() == 1;
		reviewersList.forEach(reviewerdocument -> {
			reviewerdocument.setCompleted(false);
			reviewerdocument.setEndDate(null);
			reviewerdocument.setOrderFlag(allSameRoutingOrder || reviewerdocument.getRoutingOrder() == 1);
		});
		reviewerDocumentRepository.saveAll(reviewersList);
		DocusignUserCache redis = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
				getDousignUrl().getUpdateEnvelopeMultiple().replace("{docusignHost}", docusignHost.trim()));
		builder.queryParam("envelopeId", envId);
		builder.queryParam("userId", redis.getUserId());
		String url = builder.toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		try {
			restTemplate.postForEntity(url, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Update Response", Constant.INTERNAL_SERVER_ERROR),
					"Document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("Document Update Response", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Upload Response", new ArrayList<>()), ex.getLocalizedMessage());
		}
		try {
			uploadFilesIntoBlob(multipartFiles, envId, null, "V" + (newdoc.getDocVersion() + 1), DocumentIds);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponse(HttpStatus.OK, new Response(Constant.LOA_DOCUMENT_RESPONSE, null),
				"Document updated successfully");
	}

	@Override
	public CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId)
			throws DataValidationException {
		List<DocumentVersioningResponse> response = new ArrayList<>();
		String email = getEmailFromToken(request).getUserEmail();
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		String flowType = (cacheValue != null) ? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()) : "null";
		LoaDocument currentEnvelopeLoa = null;
		LoaContractDocument loaContractDocument = null;
		if (flowType.equalsIgnoreCase("LOA_Create") || flowType.equalsIgnoreCase("LOA_Review")) {
			currentEnvelopeLoa = loaDocumentRepository.findByEnvelopeId(envelopeId);
			if (currentEnvelopeLoa == null) {
				throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
			}
		}
		if (flowType.equalsIgnoreCase("CONTRACT_create") || flowType.equalsIgnoreCase("CONTRACT_review")) {
			loaContractDocument = loaContractDocumentRepository.findByEnvelopeId(envelopeId);
			if (loaContractDocument == null) {
				throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
			}
		}
		if (cacheValue.getDisplayname() != null) {
			if (cacheValue.getDisplayname().equalsIgnoreCase("PCD_ADMIN")) {
				CentralRepoDocument centralRepoDocument = centralRepoDocumentRepository.findByEnvelopeId(envelopeId);
				switch (centralRepoDocument.getRepositoryName()) {
				case Constant.LOA_DOCUMENT_REPOSITORY:
					currentEnvelopeLoa = loaDocumentRepository.findByEnvelopeId(envelopeId);
					if (currentEnvelopeLoa == null) {
						throw new DataValidationException("Please provide valid Envelope Id", "400",
								HttpStatus.BAD_REQUEST);
					}
					break;
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					loaContractDocument = loaContractDocumentRepository.findByEnvelopeId(envelopeId);
					if (loaContractDocument == null) {
						throw new DataValidationException("Please provide valid Envelope Id", "400",
								HttpStatus.BAD_REQUEST);
					}
					break;
				}
			}
		}
		List<DocumentVersionDocument> documentList = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
		log.info("DocumentList :: {}", documentList.size());
		for (DocumentVersionDocument documentVersionDocument : documentList) {
			DocumentVersioningResponse documentVersioning = new DocumentVersioningResponse();
			documentVersioning.setEnvelopeId(documentVersionDocument.getEnvelopeId());
			documentVersioning.setOrder(documentVersionDocument.getDocVersion());
			documentVersioning.setDocumentVersion(documentVersionDocument.getVersionOrder());
			documentVersioning.setCreatedTime(documentVersionDocument.getCreatedOn());
			documentVersioning.setUpdatedTime(documentVersionDocument.getUpdatedOn());
			if (currentEnvelopeLoa != null) {
				documentVersioning.setEnvelopeName(currentEnvelopeLoa.getProjectName());
			} else {
				log.info("added clm contract");
				documentVersioning
						.setEnvelopeName(loaContractDocument != null ? loaContractDocument.getContractName() : null);
			}
			response.add(documentVersioning);
		}
		Collections.sort(response, Comparator.comparingInt(DocumentVersioningResponse::getOrder).reversed());
		return new CommonResponse(HttpStatus.OK, new Response("Document Versioning Response", response),
				"Documents fetched successfully");
	}

	@Override
	public CommonResponse getLoaDocumentDetailsView(String envelopeId)
			throws DataValidationException, JsonProcessingException {
		LoaDocumentDetailsViewResponse detailsViewResponse = new LoaDocumentDetailsViewResponse();
		LoaDocument loaDocument = loaDocumentRepository.findByEnvelopeId(envelopeId);
		EnvelopeDocument envelopeDocument = envelopeRepository.findByenvelopeId(envelopeId);
		if (loaDocument == null || envelopeDocument == null) {
			throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
		}
		CreateTemplate template = createTemplateRepository.findByTemplateId(loaDocument.getTemplateId());
		if (template == null) {
			throw new DataValidationException("Provide valid Template Id", "400", HttpStatus.BAD_REQUEST);
		}
		String json1 = mapper.writeValueAsString(envelopeDocument.getEnvelope());
		JsonNode rootNode = mapper.readTree(json1);
		detailsViewResponse.setTemplateName(template.getTemplateName());
		detailsViewResponse.setEmailSubject(rootNode.get("emailSubject").asText());
		detailsViewResponse.setEmailMessage(rootNode.get("emailBlurb").asText());
		detailsViewResponse.setProjectId(loaDocument.getProjectId());
		detailsViewResponse.setProjectName(loaDocument.getProjectName());
		detailsViewResponse.setVendorName(loaDocument.getVendorName());
		detailsViewResponse.setCommencementDate(loaDocument.getCommencementDate());
		detailsViewResponse.setCompletionDate(loaDocument.getCompletionDate());
		detailsViewResponse.setContractValue(loaDocument.getCurrency());
		detailsViewResponse.setContractTenure(loaDocument.getContractTenure());
		detailsViewResponse.setStatus(loaDocument.getStatus());
		detailsViewResponse.setVendors(loaDocument.getVendors());
		detailsViewResponse.setSubsidiary(loaDocument.getSubsidiary());
		detailsViewResponse.setAllSigners(loaDocument.getAllSigners());
		detailsViewResponse.setVendorSigningOrder(loaDocument.isVendorSigningOrder());
		detailsViewResponse.setSignerSigningOrder(loaDocument.getSignerSigningOrder());
		detailsViewResponse.setLhdnStampers(loaDocument.getLhdnStampers());
		detailsViewResponse.setLhdnSigningOrder(loaDocument.getLhdnSigningOrder());
		detailsViewResponse.setContractNumber(loaDocument.getContractNumber());
		detailsViewResponse.setRemark(loaDocument.getRemark());
		detailsViewResponse.setCategory(loaDocument.getCategory());
		return new CommonResponse(HttpStatus.OK, new Response("LOA Document Response", detailsViewResponse),
				"LOA Document details Retrieved Successfully");
	}

	public String generateVersion(int orderCount) {
		int major = (orderCount / 10) + 1;
		int minor = orderCount % 10;
		if (minor == 0) {
			return major + ".0";
		} else {
			return major + "." + minor;
		}
	}

	public String generateRandomString(int length) {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(characters.length());
			sb.append(characters.charAt(randomIndex));
		}
		return sb.toString();
	}

	public String generateProjectId(String value) {
		while (value.length() < 6) {
			value = '0' + value;
		}
		return value;
	}

	@Override
	public CommonResponse getLoaDocumentDetail(String envelopeId)
			throws DataValidationException, JsonProcessingException {
		LoaDocumentDetailResponse documentDetailResponse = new LoaDocumentDetailResponse();
		LoaDocument loaDocument = loaDocumentRepository.findByEnvelopeId(envelopeId);
		EnvelopeDocument envelopeDocument = envelopeRepository.findByenvelopeId(envelopeId);
		if (loaDocument == null || envelopeDocument == null) {
			throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
		}
		CreateTemplate template = createTemplateRepository.findByTemplateId(loaDocument.getTemplateId());
		if (template == null) {
			throw new DataValidationException("Provide valid Template Id", "400", HttpStatus.BAD_REQUEST);
		}
		String json1 = mapper.writeValueAsString(envelopeDocument.getEnvelope());
		JsonNode rootNode = mapper.readTree(json1);
		documentDetailResponse.setTemplateName(template.getTemplateName());
		documentDetailResponse.setEmailSubject(rootNode.get("emailSubject").asText());
		documentDetailResponse.setEmailMessage(rootNode.get("emailBlurb").asText());
		documentDetailResponse.setProjectId(loaDocument.getProjectId());
		documentDetailResponse.setProjectName(loaDocument.getProjectName());
		documentDetailResponse.setVendorName(loaDocument.getVendorName());
		documentDetailResponse.setCommencementDate(loaDocument.getCommencementDate());
		documentDetailResponse.setCompletionDate(loaDocument.getCompletionDate());
		documentDetailResponse.setContractValue(loaDocument.getCurrency());
		documentDetailResponse.setContractTenure(loaDocument.getContractTenure());
		documentDetailResponse.setStatus(loaDocument.getStatus());
		documentDetailResponse.setEnvelopeId(envelopeId);
		documentDetailResponse.setContractNumber(loaDocument.getContractNumber());
		return new CommonResponse(HttpStatus.OK, new Response("LOA Document Response", documentDetailResponse),
				"LOA Document details Retrieved Successfully");
	}

	public CommonResponse getCreateListForLoa(HttpServletRequest request, int page, int limit, String searchText,
			String order, String orderBy, String subsidiary, String status, String category)
			throws DataValidationException, UnsupportedEncodingException {
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
		if (!(currentRole.equalsIgnoreCase("LOA_CREATOR") || currentRole.equalsIgnoreCase("CONTRACT_CREATOR")
				|| currentRole.contains("ADMIN") || currentRole.equalsIgnoreCase("PCD_USER"))) {
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		}
		List<LoaDocument> list;

		if (order == null) {
			totalCount = loaDocumentRepository.findByCreatedByCount(email);
			list = loaDocumentRepository.findByCreatedBy(email, pageable);
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
			Criteria criteria = new Criteria();
			if (!searchText.isEmpty() && searchText != null) {
				searchText = URLDecoder.decode(searchText, "UTF-8");
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("projectName").regex(pattern),
						Criteria.where("projectId").regex(pattern), Criteria.where("contractNumber").regex(pattern));
				query.addCriteria(criteria);
			}
			log.info("Query {}", query);
			Pageable pageable01 = pageable;
			totalCount = mongoTemplate.count(query, LoaDocument.class);
			query.with(pageable01);
			list = mongoTemplate.find(query, LoaDocument.class).stream().collect(Collectors.toList());
		}
		List<String> listOfEnvelopeIds = list.stream().map(document -> document.getEnvelopeId())
				.collect(Collectors.toList());
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		for (LoaDocument loaDocument : list) {
			List<String> reviewers = new ArrayList<>();
			List<ReviewerDocument> reviewerslist = reviewerList.stream()
					.filter(document -> document.getEnvelopeId().equals(loaDocument.getEnvelopeId()))
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
			DocumentCreateResponse resp = DocumentCreateResponse.builder().projectName(loaDocument.getProjectName())
					.createdOn(loaDocument.getCreatedOn()).projectId(loaDocument.getProjectId())
					.ownersEmail(loaDocument.getCreatedBy()).senderName(loaDocument.getSenderName())
					.envelopeId(loaDocument.getEnvelopeId()).status(loaDocument.getStatus()).pendingWith(reviewers)
					.commencementDate(loaDocument.getCommencementDate()).completionDate(loaDocument.getCompletionDate())
					.subsidiary(loaDocument.getSubsidiary()).isFilesUploaded(loaDocument.isFilesUploaded())
					.vendorName(loaDocument.getVendorName()).contractNumber(loaDocument.getContractNumber()).build();
			createResponses.add(resp);
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", createResponses);
		return new CommonResponse(HttpStatus.OK, new Response("getCreateListForLoaResponse", commonResponse),
				"LOA details fetched successfully");
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

	public String convertBlobInputStreamToBase64(InputStream inputStream) throws Exception {
		try {
			byte[] bytes = IOUtils.toByteArray(inputStream);
			return Base64.getEncoder().encodeToString(bytes);
		} finally {
			inputStream.close();
		}
	}

	@Override
	public CommonResponse newaddLoaDocument(String json, String templateId, String profile, String name,
			String fileCount) throws IOException, TemplateException, MessagingException, DataValidationException {
	try {
		LoaDocumentRequest jsonrequest = mapper.readValue(json, LoaDocumentRequest.class);
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + profile);
		String buildurl = null;
		jsonrequest.setUserEmail(profile);
		LoaDocument loaContractDocument = new LoaDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(LoaDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> envelopeIdmap = new HashMap<>();
		loaContractDocument.setId(id);
		loaContractDocument.setFlowCompleted(false);
		loaContractDocument.setCompleted(false);
		loaContractDocument.setTemplateId(templateId);
		loaContractDocument.setProjectId(projectId);
		loaContractDocument.setProjectName(jsonrequest.getProjectName());
		loaContractDocument.setVendorName(jsonrequest.getVendorName());
		loaContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		loaContractDocument.setCompletionDate(jsonrequest.getCompletionDate());
		loaContractDocument.setBuID("BUID");
		loaContractDocument.setOpID("SAASPE");
		loaContractDocument.setCreatedOn(new Date());
		loaContractDocument.setCreatedBy(profile);
		loaContractDocument.setContractTenure(jsonrequest.getContractTenure());
		loaContractDocument.setStatus(jsonrequest.getStatus());
		loaContractDocument.setVersion("1.0");
		loaContractDocument.setOrder(0);
		loaContractDocument.setUniqueString(generateRandomString(6));
		loaContractDocument.setReferenceId(templateId);
		loaContractDocument.setReferenceType("Template");
		loaContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		loaContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		Currency currency = new Currency(jsonrequest.getCurrency().getCurrencyCode(),
				jsonrequest.getCurrency().getTotalCost(), jsonrequest.getCurrency().getTax());
		loaContractDocument.setCurrency(currency);
		loaContractDocument.setVendors(jsonrequest.getVendors());
		loaContractDocument.setFilesUploaded(false);
		loaContractDocument.setTotalFiles(Integer.valueOf(fileCount));
		loaContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		loaContractDocument.setRemark(jsonrequest.getRemark());
		List<AllSigners> allSignersList = new ArrayList<>();
		for (AllSigners s : jsonrequest.getAllSigners()) {
			s.setSigningStatus("sent");
			allSignersList.add(s);
		}
		loaContractDocument.setIsSignersCompleted("sent");
		loaContractDocument.setIsVendorsCompleted("sent");
		loaContractDocument.setIsLhdnSignersCompleted("sent");
		loaContractDocument.setAllSigners(allSignersList);
		loaContractDocument.setLhdnStampers(jsonrequest.getLhdnStampers());
		loaContractDocument.setLhdnSigningOrder(jsonrequest.getLhdnSigningOrder());
		loaContractDocument.setContractNumber(jsonrequest.getContractNumber());
		loaContractDocument.setCategory(jsonrequest.getCategory());
        loaContractDocument.setWatcherEmailStatus(false);
        jsonrequest.setModuleName(Constant.LOA_DOCUMENT_REPOSITORY);
        jsonrequest.setContractName(jsonrequest.getProjectName());	
        buildurl = getDousignUrl().getNewCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
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
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, Constant.INTERNAL_SERVER_ERROR),
					"LOA document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		log.info("Success in envelope create API");
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String envelopeId = rootNode.get("envelopeId").asText();
		envelopeIdmap.put("envelopeId", envelopeId);
		System.out.println(envelopeId);
		loaContractDocument.setEnvelopeId(envelopeId);
		log.info("Saving loaDocument");
		loaDocumentRepository.save(loaContractDocument);
		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		documentVersion.setEnvelopeId(envelopeId);
		documentVersion.setVersionOrder("1.0");
		documentVersion.setDocVersion(0);
		documentVersion.setCreatedOn(new Date());
		documentVersion.setPath(containerName + envelopeId + "/V" + (documentVersion.getDocVersion() + 1));
		log.info("Saving Document version document");
		documentVersionDocumentRepository.save(documentVersion);
		if (envelopeId != null) {
			log.info("Calling Get EnvelopeById API");
			buildurl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
			String envelopeDataUrl = buildurl + envelopeId;
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, Constant.INTERNAL_SERVER_ERROR),
						"LOA Document Creation Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
			}
			log.info("Success in getEnvelopeById API");
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(envelopeId);
			Date createDate = new Date();
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(envelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getProjectName());
			creator.setFlowType("LOA_Create");
			creator.setProjectId(projectId);
			creator.setStatus(jsonrequest.getStatus());
			creator.setSubsidiary(jsonrequest.getSubsidiary());
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(profile);
			boolean hasSameRoutingOrder = jsonrequest.getReviewers().stream().map(Reviewers::getRoutingOrder).distinct()
					.count() == 1;
			int firstRoutingOrder = jsonrequest.getReviewers().stream().map(Reviewers::getRoutingOrder).sorted()
					.findFirst().get();
			for (Reviewers reviewer : jsonrequest.getReviewers()) {
				ReviewerDocument reviewerDocument = new ReviewerDocument();
				reviewerDocument.setId(sequenceGeneratorService.generateSequence(ReviewerDocument.SEQUENCE_NAME));
				reviewerDocument.setEmail(reviewer.getEmail());
				reviewerDocument.setDocVersion(0);
				reviewerDocument.setCompleted(false);
				reviewerDocument.setEnvelopeId(envelopeId);
				reviewerDocument.setCreatedOn(createDate);
				reviewerDocument.setCreatorName(name);
				reviewerDocument.setCreatedBy(profile);
				reviewerDocument.setContractName(jsonrequest.getProjectName());
				reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				reviewerDocument.setFlowType("LOA_Review");
				reviewerDocument.setReviewerName(reviewer.getName());
				reviewerDocument.setStatus(jsonrequest.getStatus());
				reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
				reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
				reviewerDocument.setCompletionDate(jsonrequest.getCompletionDate());
				reviewerDocument.setProjectId(projectId);
				reviewerDocument.setVendorName(jsonrequest.getVendorName());
				reviewerDocument.setContractNumber(jsonrequest.getContractNumber());
				if (reviewer.getRoutingOrder() == firstRoutingOrder) {
					pendingWith.add(reviewer.getEmail());
					mailSenderService.sendRequestForReviewMail(envelopeId, reviewer.getEmail(), reviewer.getName(),
							jsonrequest.getProjectName(), loaContractDocument.getVersion(),
							loaContractDocument.getCreatedBy(), "pcd", Constant.LOA_MODULE);
				}
				reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
				log.info("Saving Reviewer Document");
				reviewerDocumentRepository.save(reviewerDocument);
			}
			creator.setPendingWith(pendingWith);
			log.info("Saving WorkFlow creator Document");
			workFlowCreatorDocumentRespository.save(creator);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setStartDate(new Date());
			log.info("Saving Envelope Document");
			envelopeRepository.save(envelopeDocument);
			loaContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			loaContractDocument.setSenderName(rootNode2.path(Constant.SENDER).path(Constant.USERNAME).asText());
			log.info("Saving loaDocument");
			loaDocumentRepository.save(loaContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(envelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOA_DOCUMENT_REPOSITORY);
			log.info("Saving CentralRepo Document");
			centralRepoDocumentRepository.save(centralRepoDocument);
		} else {
			throw new DataValidationException("LOA creation failed, try again!", "400", HttpStatus.BAD_REQUEST);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.LOA_DOCUMENT_RESPONSE, envelopeIdmap),
				"Loa document details submitted successfully");
	}
	   catch (Exception e) {
		  e.printStackTrace();
		  if (e instanceof MismatchedInputException) {
			 throw new DataValidationException("Fields mismatch or malformed payload. Please try again!", "400", HttpStatus.BAD_REQUEST);
	         }
		  throw new DataValidationException(e.getMessage(), "400", HttpStatus.BAD_REQUEST);
	       }

	}

	@Override
	@Transactional
	public CommonResponse updateLoaDocument(MultipartFile createDocumentFiles, String create_id, String envelopeId,
			int versionOrder, String email) throws DataValidationException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("document-file", createDocumentFiles.getResource());
		body.add("envelopeId", envelopeId);
		body.add("documentId", create_id);
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		if (versionOrder <= 10) {
			try {
				log.info("Calling Update Document API");
				ResponseEntity<Object> responseEntity = restTemplate.exchange(docusignHost + "/updateDocument",
						HttpMethod.POST, requestEntity, Object.class);
				if (responseEntity.getStatusCode() == HttpStatus.OK) {
					DocumentVersionDocument documentVersionDocument = documentVersionDocumentRepository
							.findEnvelopeIdAndDocVersion(envelopeId, versionOrder);
					if (documentVersionDocument == null) {
						log.info("Inside If loop");
						uploadFilesIntoBlob(createDocumentFiles, envelopeId, "V" + (versionOrder + 1), create_id);
						DocumentVersionDocument newdoc = new DocumentVersionDocument();
						newdoc.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
						newdoc.setVersionOrder(generateVersion(versionOrder + 1));
						newdoc.setEnvelopeId(envelopeId);
						newdoc.setDocVersion(versionOrder);
						newdoc.setPath(containerName + envelopeId + "/V" + (versionOrder + 1));
						newdoc.setComments(null);
						newdoc.setCreatedOn(new Date());
						newdoc.setUpdatedOn(new Date());
						log.info("Saving Document Version Document : {}", newdoc.getId());
						documentVersionDocumentRepository.save(newdoc);
						if (createDocumentFiles.getOriginalFilename() != null) {
							log.info("Inside fileUpload true condition");
							getFileUpload(envelopeId, create_id, createDocumentFiles.getOriginalFilename());
						} else {
							log.info("Inside fileUpload false condition");
							getFileUpload(envelopeId, create_id, "Document-" + create_id);
						}
					} else {
						log.info("Inside Else loop");
						uploadFilesIntoBlob(createDocumentFiles, envelopeId, "V" + (versionOrder + 1), create_id);
						if (createDocumentFiles.getOriginalFilename() != null) {
							log.info("Inside fileUpload true condition");
							getFileUpload(envelopeId, create_id, createDocumentFiles.getOriginalFilename());
						} else {
							log.info("Inside fileUpload false condition");
							getFileUpload(envelopeId, create_id, "Document-" + create_id);
						}
					}
				}
				return new CommonResponse(HttpStatus.OK,
						new Response("Document Upload Response", "Document Update Successful"),
						"Document upload successful");
			} catch (Exception e) {
				log.info("Inside Catch block");
				e.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Document Upload Response", Constant.INTERNAL_SERVER_ERROR),
						"Document upload Unsuccessful");
			}
		} else {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Upload Response", "Document versions cannot exceeds more than 10Times!"),
					"Document upload Unsuccessful");
		}
	}

	private void uploadFilesIntoBlob(MultipartFile multipartFiles, String envelopeId, String version, String documentId)
			throws URISyntaxException, StorageException, IOException {
		String path = envelopeId;
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		CloudBlockBlob blob = container.getBlockBlobReference(containerName + path + "/" + version + "/" + documentId);
		blob.getProperties().setContentType("application/pdf");
		try (InputStream inputStream = multipartFiles.getInputStream()) {
			blob.upload(inputStream, multipartFiles.getSize());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CommonResponse getFileUpload(String envelopeId, String documentId, String name)
			throws DataValidationException {
		DocumentVersionDocument documentVersionList = documentVersionDocumentRepository
				.findTopByEnvelopeIdOrderByDocVersionDesc(envelopeId);
		if (documentVersionList != null) {
			log.info("Id : {}", documentVersionList.getId());
			System.out.println("Inside getFileUpload");
			saaspe.clm.model.Document document = new saaspe.clm.model.Document();
			document.setDocumentId(documentId);
			document.setUploaded(true);
			document.setName(name);
			if (documentVersionList.getDocuments() != null) {
				System.out.println("Inside getFileUpload 1st Loop");
				documentVersionList.getDocuments().add(document);
			} else {
				System.out.println("Inside getFileUpload 2nd Loop");
				documentVersionList.setDocuments(Arrays.asList(document));
			}
			System.out.println("Saving Document Version");
			documentVersionDocumentRepository.save(documentVersionList);
			return new CommonResponse(HttpStatus.OK, new Response("File Upload Status Response", null),
					"File Details Retrieved Successfully");
		} else {
			throw new DataValidationException("No DocumentVersionDocument found with envelopeId: " + envelopeId, "400",
					HttpStatus.BAD_REQUEST);
		}

	}

	@Override
	@Transactional
	public CommonResponse deleteandUploadOldDocToBlob(String[] delete_id, String[] existing_id,
			List<String> existingNames, String envelopeId, int versionOrder, String email)
			throws DataValidationException, JsonProcessingException, URISyntaxException, StorageException {
		List<String> deleteIdsList = delete_id != null ? Arrays.asList(delete_id) : new ArrayList<>();

		if (versionOrder <= 10) {
			if (!deleteIdsList.isEmpty()) {
				UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getDousignUrl()
						.getDeleteEnvelopeDocuments().replace(Constant.DOCUSIGN_HOST, docusignHost.trim()));
				JSONObject requestBody = new JSONObject();
				requestBody.put("envelopeId", envelopeId);
				requestBody.put("delete_id", deleteIdsList);
				deleteIdsList.forEach(e -> System.out.println(e));
				String url = builder.toUriString();
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
				try {
					restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
				} catch (HttpClientErrorException.BadRequest ex) {
					ex.printStackTrace();
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response(Constant.LOA_DOCUMENT_RESPONSE, Constant.INTERNAL_SERVER_ERROR),
							"LOA document Creation Failed");
				} catch (RestClientException ex) {
					ex.printStackTrace();
					if (ex instanceof ResourceAccessException) {
						return new CommonResponse(HttpStatus.NOT_FOUND,
								new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
								"Unable to access remote resources.Please check the connectivity");
					}
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
				}
			}
			DocumentVersionDocument documentVersionDocument = documentVersionDocumentRepository
					.findEnvelopeIdAndDocVersion(envelopeId, versionOrder);
			if (documentVersionDocument != null) {
				for (int i = 0; i < existing_id.length; i++) {
					String oldId = existing_id[i];
					CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
					CloudBlockBlob source = container.getBlockBlobReference(containerName + envelopeId + "/V"
							+ (documentVersionDocument.getDocVersion()) + "/" + oldId);
					CloudBlockBlob destination = null;
					if (!container
							.getBlockBlobReference(
									containerName + envelopeId + "/V" + documentVersionDocument.getDocVersion())
							.exists()) {
						destination = container.getBlockBlobReference(containerName + envelopeId + "/V"
								+ (documentVersionDocument.getDocVersion() + 1) + "/" + oldId);
					}
					destination.startCopy(source);
					saaspe.clm.model.Document document = new saaspe.clm.model.Document();
					document.setDocumentId(oldId);
					document.setUploaded(true);
					document.setName(existingNames.get(i));
					if (documentVersionDocument.getDocuments() != null) {
						documentVersionDocument.getDocuments().add(document);
					} else {
						documentVersionDocument.setDocuments(Arrays.asList(document));
					}
					documentVersionDocumentRepository.save(documentVersionDocument);
				}
			} else {
				DocumentVersionDocument newdoc = new DocumentVersionDocument();
				newdoc.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
				newdoc.setVersionOrder(generateVersion(versionOrder));
				newdoc.setEnvelopeId(envelopeId);
				newdoc.setDocVersion(versionOrder);
				newdoc.setPath(containerName + envelopeId + "/V" + (versionOrder + 1));
				newdoc.setComments(null);
				newdoc.setCreatedOn(new Date());
				newdoc.setUpdatedOn(new Date());
				List<saaspe.clm.model.Document> documentList = new ArrayList<>();
				for (int i = 0; i < existing_id.length; i++) {
					String oldId = existing_id[i];
					CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
					CloudBlockBlob source = container.getBlockBlobReference(
							containerName + envelopeId + "/V" + (newdoc.getDocVersion()) + "/" + oldId);
					CloudBlockBlob destination = null;
					if (!container.getBlockBlobReference(containerName + envelopeId + "/V" + newdoc.getDocVersion())
							.exists()) {
						destination = container.getBlockBlobReference(
								containerName + envelopeId + "/V" + (newdoc.getDocVersion() + 1) + "/" + oldId);
					}
					destination.startCopy(source);
					saaspe.clm.model.Document document = new saaspe.clm.model.Document();
					document.setDocumentId(oldId);
					document.setUploaded(true);
					document.setName(existingNames.get(i));
					documentList.add(document);
				}
				newdoc.setDocuments(documentList);
				documentVersionDocumentRepository.save(newdoc);
			}
			return new CommonResponse(HttpStatus.OK,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, "Document deleted Successfully"),
					"LOA document Deletion Successful");
		} else {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Upload Response", "Document versions cannot exceeds more than 10Times!"),
					"Document upload Unsuccessful");
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
		List<LoaDocument> expiringDocuments = loaDocumentRepository.findByCreatedOnBeforeAndStatus(endDate, "created");
		log.info("End Date :{}", endDate);
		if (!expiringDocuments.isEmpty()) {
			expiringDocuments.stream().forEach(e -> System.out.println(e.getEnvelopeId()));
			for (LoaDocument document : expiringDocuments) {
				log.info("Updating LOA Document Status");
				document.setStatus("expired");
				envelopeList.add(document.getEnvelopeId());
				loaDocumentRepository.save(document);
			}
			return new CommonResponse(HttpStatus.OK, new Response("Document Update Response", envelopeList),
					"Document upload Successful");
		} else
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Update Response", "No Documents found to update"),
					"Document upload Unsuccessful");
	}
}
