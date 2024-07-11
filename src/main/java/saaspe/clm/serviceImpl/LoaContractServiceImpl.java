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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
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
import saaspe.clm.document.LoaContractDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
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
import saaspe.clm.model.LoaContractCreatedListPagination;
import saaspe.clm.model.LoaContractCreatedResponse;
import saaspe.clm.model.LoaContractDetailsViewResponse;
import saaspe.clm.model.LoaContractDocumentRequest;
import saaspe.clm.model.Response;
import saaspe.clm.model.Reviewers;
import saaspe.clm.model.UserDetails;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.DocumentVersionDocumentRepository;
import saaspe.clm.repository.EnvelopeRepository;
import saaspe.clm.repository.LoaContractDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.LoaContractService;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.utills.Base64ToMultipartFileConverter;
import saaspe.clm.utills.RedisUtility;

@Service
public class LoaContractServiceImpl implements LoaContractService {

	private static Logger log = LoggerFactory.getLogger(LoaContractServiceImpl.class);

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
	private LoaContractDocumentRepository loaContractDocumentRepository;

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private DocumentVersionDocumentRepository documentVersionDocumentRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository workFlowCreatorDocumentRespository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

	@Autowired
	private UserInfoRespository userInfoRespository;
	
	@Autowired
	private MailSenderService mailSenderService;

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

	@Autowired
	private CloudBlobClient cloudBlobClient;

	@Value("${azure.storage.container.name}")
	private String containerName;

	Random random = new Random();

	private final MongoTemplate mongoTemplate;

	public LoaContractServiceImpl(MongoTemplate mongoTemplate) {
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
	public CommonResponse addLoaContractDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String envelopeId, String profile, String name)
			throws DataValidationException, IOException, TemplateException, MessagingException {
		LoaContractDocumentRequest jsonrequest = mapper.readValue(json, LoaContractDocumentRequest.class);
		int minRange = 1000;
		int maxRange = 9999;
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();
		List<Document> versionDocDocuments = new ArrayList<>();
		List<DocumentResponse> BlobdocumentResponses = new ArrayList<>();
		List<MultipartFile> multipartList = new ArrayList<>();
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + profile);
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
					documentRequest.setCategory(Constant.CREATE_FILE);
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					documentRequest.setDocumentId(String.valueOf(randomNumber));
					documentRequests.add(documentRequest);
					DocumentResponse blobResponse = new DocumentResponse();
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
		LoaContractDocument loaContractDocument = new LoaContractDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(LoaContractDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> projectIdmap = new HashMap<>();
		projectIdmap.put("projectId", projectId);
		loaContractDocument.setId(id);
		loaContractDocument.setProjectId(projectId);
		loaContractDocument.setProjectName(jsonrequest.getProjectName());
		loaContractDocument.setContractName(jsonrequest.getContractName());
		loaContractDocument.setVendorName(jsonrequest.getVendorName());
		loaContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		loaContractDocument.setCompletionDate(jsonrequest.getCompletionDate());
		loaContractDocument.setIsFlowCompleted(false);
		loaContractDocument.setBuID("BUID");
		loaContractDocument.setOpID("SAASPE");
		loaContractDocument.setCreatedOn(new Date());
		loaContractDocument.setCreatedBy(profile);
		loaContractDocument.setContractTenure(jsonrequest.getContractTenure());
		loaContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		loaContractDocument.setTemplateId(envelopeId);
		loaContractDocument.setStatus(jsonrequest.getStatus());
		loaContractDocument.setVersion("1.0");
		loaContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		loaContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		Currency currency = new Currency(jsonrequest.getCurrency().getCurrencyCode(),
				jsonrequest.getCurrency().getTotalCost(), jsonrequest.getCurrency().getTax());
		loaContractDocument.setCurrency(currency);
		loaContractDocument.setVendors(jsonrequest.getVendors());
		loaContractDocument.setStampers(jsonrequest.getStampers());
		loaContractDocument.setVendorSigningOrder(jsonrequest.getVendorSigningOrder());
		loaContractDocument.setCvefSigningOrder(jsonrequest.getCvefSigningOrder());
		loaContractDocument.setCvefSigners(jsonrequest.getCvefSigners());
		loaContractDocument.setLhdnStampers(jsonrequest.getLhdnStampers());
		loaContractDocument.setLhdnSigningOrder(jsonrequest.getLhdnSigningOrder());
		loaContractDocument.setAllSigners(jsonrequest.getAllSigners());
		loaContractDocument.setContractNumber(jsonrequest.getContractNumber());
		String buildurl = getDousignUrl().getCreateLOAContractDocument().replace(Constant.DOCUSIGN_HOST,
				docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
		builder.queryParam(Constant.ENVELOPE_ID, envelopeId);
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
					new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE,Constant.INTERNAL_SERVER_ERROR),
					"LOA Contract Document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String newEnvelopeId = rootNode.get(Constant.ENVELOPE_ID).asText();
		if (newEnvelopeId != null) {
			buildurl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
			String envelopeDataUrl = buildurl + newEnvelopeId;
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE,Constant.INTERNAL_SERVER_ERROR),
						"LOA Document Creation Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()),
						ex.getLocalizedMessage());
			}
			JsonNode responseData = mapper.valueToTree(envelopeDataResponse.getBody());
			JsonNode documents = responseData.path("envelope").path("envelopeDocuments");
			if (documents.isArray()) {
				for (JsonNode document : documents) {
					String documentId = document.path("documentId").asText();
					String documentName = document.path("name").asText();
					String documentbase64 = null;
			//		CommonResponse base64Response = clmServiceImpl.getEnvelopeDocument(newEnvelopeId, documentId);
				//	documentbase64 = base64Response.getResponse().getData().toString();
					DocumentResponse blobResponse = new DocumentResponse();
					MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentbase64, documentName,
							"text/plain");
					multipartList.add(multipartFile);
					blobResponse.setDocumentBase64(documentbase64);
					blobResponse.setName(documentName);
					blobResponse.setDocumentId(documentId);
					BlobdocumentResponses.add(blobResponse);
					Document newDoc = new Document();
					newDoc.setDocumentId(documentId);
					newDoc.setName(documentName);
					versionDocDocuments.add(newDoc);
				}
			}
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(newEnvelopeId);
			Date createDate = new Date();
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(newEnvelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getProjectName());
			creator.setFlowType("CONTRACT_create");
			creator.setStatus(jsonrequest.getStatus());
			creator.setSubsidiary(jsonrequest.getSubsidiary());
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(profile);
			List<ReviewerDocument> reviewers = new ArrayList<>();
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
				reviewerDocument.setEnvelopeId(newEnvelopeId);
				reviewerDocument.setCreatedOn(createDate);
				reviewerDocument.setCreatorName(name);
				reviewerDocument.setCreatedBy(profile);
				reviewerDocument.setContractName(jsonrequest.getProjectName());
				reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				reviewerDocument.setFlowType("CONTRACT_review");
				reviewerDocument.setReviewerName(reviewer.getName());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
				reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
				reviewerDocument.setStatus(jsonrequest.getStatus());
				reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
				reviewerDocument.setCompletionDate(jsonrequest.getCompletionDate());
				reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
				reviewerDocument.setProjectId(projectId);
				reviewerDocument.setContractName(jsonrequest.getContractName());
				reviewerDocument.setVendorName(jsonrequest.getVendorName());
				if (reviewer.getRoutingOrder() == firstRoutingOrder) {
					pendingWith.add(reviewer.getEmail());
					mailSenderService.sendRequestForReviewMail(envelopeId, reviewer.getEmail(), reviewer.getName(),
							jsonrequest.getProjectName(), loaContractDocument.getVersion(),
							loaContractDocument.getCreatedBy(), "pcd", Constant.CONTRACT_MODULE);
				}
				reviewerDocumentRepository.save(reviewerDocument);
				reviewers.add(reviewerDocument);
			}
			creator.setPendingWith(pendingWith);
			creator.setProjectId(projectId);
			workFlowCreatorDocumentRespository.save(creator);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
			DocumentVersionDocument documentVersion = new DocumentVersionDocument();
			documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
			documentVersion.setEnvelopeId(newEnvelopeId);
			documentVersion.setVersionOrder("1.0");
			documentVersion.setDocVersion(0);
			documentVersion.setCreatedOn(new Date());
			documentVersion.setPath(containerName + newEnvelopeId + "/V" + (documentVersion.getDocVersion() + 1));
			documentVersion.setDocuments(versionDocDocuments);
			documentVersionDocumentRepository.save(documentVersion);
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setStartDate(new Date());
			envelopeRepository.save(envelopeDocument);
			loaContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			loaContractDocument.setEnvelopeId(newEnvelopeId);
			log.info("LOA envelopId: {}", newEnvelopeId);
			loaContractDocumentRepository.save(loaContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(newEnvelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY);
			centralRepoDocumentRepository.save(centralRepoDocument);
			try {
				uploadFilesIntoBlob(multipartList, newEnvelopeId, BlobdocumentResponses,
						"V" + (documentVersion.getDocVersion() + 1), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			throw new DataValidationException("create LOA contract failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED,
				new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, projectIdmap),
				"Loa Contract document details submitted successfully");
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
	public CommonResponse getLoaContractDocumentDetailsView(String envelopeId) throws DataValidationException {

		LoaContractDocument loaContractDocument = loaContractDocumentRepository.findByEnvelopeId(envelopeId);
		if (loaContractDocument == null)
			throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
		LoaContractDetailsViewResponse detailsViewResponse = new LoaContractDetailsViewResponse();
		detailsViewResponse.setProjectName(loaContractDocument.getProjectName());
		detailsViewResponse.setEnvelopeId(loaContractDocument.getEnvelopeId());
		detailsViewResponse.setVendorName(loaContractDocument.getVendorName());
		detailsViewResponse.setContractName(loaContractDocument.getContractName());
		detailsViewResponse.setCommencementDate(loaContractDocument.getCommencementDate());
		detailsViewResponse.setCompletionDate(loaContractDocument.getCompletionDate());
		detailsViewResponse.setContractValue(loaContractDocument.getCurrency());
		detailsViewResponse.setContractTenure(loaContractDocument.getContractTenure());
		detailsViewResponse.setStatus(loaContractDocument.getStatus());
		detailsViewResponse.setCreatedOn(loaContractDocument.getCreatedOn());
		detailsViewResponse.setSenderEmail(loaContractDocument.getSenderEmail());
		detailsViewResponse.setVendors(loaContractDocument.getVendors());
		detailsViewResponse.setStampers(loaContractDocument.getStampers());
		detailsViewResponse.setCvefSigners(loaContractDocument.getCvefSigners());
		detailsViewResponse.setSubsidiary(loaContractDocument.getSubsidiary());
		detailsViewResponse.setLhdnStampers(loaContractDocument.getLhdnStampers());
		detailsViewResponse.setLhdnSigningOrder(loaContractDocument.getLhdnSigningOrder());
		detailsViewResponse.setAllSigners(loaContractDocument.getAllSigners());
		detailsViewResponse.setContractNumber(loaContractDocument.getContractNumber());
		detailsViewResponse.setProjectId(loaContractDocument.getProjectId());
		detailsViewResponse.setRemark(loaContractDocument.getContractName());
		detailsViewResponse.setCategory(loaContractDocument.getCategory());
		return new CommonResponse(HttpStatus.OK,
				new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, detailsViewResponse),
				"LOA Contract Document details Retrieved Successfully");
	}

	@Override
	public CommonResponse getListOfLoaContractCreated(int page, int limit) {

		Pageable pageable = PageRequest.of(page, limit);
		List<LoaContractDocument> createdContractList = loaContractDocumentRepository.findByIsFlowCompleted(pageable,
				true);
		List<LoaContractCreatedResponse> contractListResponse = new LinkedList<>();
		for (LoaContractDocument contractDocument : createdContractList) {
			LoaContractCreatedResponse documentDetail = new LoaContractCreatedResponse();
			documentDetail.setEnvelopeId(contractDocument.getEnvelopeId());
			documentDetail.setProjectName(contractDocument.getProjectName());
			documentDetail.setVendorName(contractDocument.getVendorName());
			documentDetail.setCommencementDate(contractDocument.getCommencementDate());
			documentDetail.setCompletionDate(contractDocument.getCompletionDate());
			documentDetail.setContractValue(contractDocument.getCurrency());
			documentDetail.setContractTenure(contractDocument.getContractTenure());
			documentDetail.setStatus(contractDocument.getStatus());
			documentDetail.setCreatedOn(contractDocument.getCreatedOn());
			documentDetail.setSenderEmail(contractDocument.getSenderEmail());
			documentDetail.setVendors(contractDocument.getVendors());
			documentDetail.setStampers(contractDocument.getStampers());
			documentDetail.setLhdnStampers(contractDocument.getLhdnStampers());
			documentDetail.setContractNumber(contractDocument.getContractNumber());
			contractListResponse.add(documentDetail);
		}
		LoaContractCreatedListPagination paginatedResponse = new LoaContractCreatedListPagination();
		paginatedResponse.setRecords(contractListResponse);
		paginatedResponse.setTotal(loaContractDocumentRepository.findByFlowCompleted(true));
		return new CommonResponse(HttpStatus.OK, new Response("LoaContract Created list response", paginatedResponse),
				"LoaContract Created list details retrieved successfully");
	}

	@Override
	public CommonResponse getCreateListForContract(HttpServletRequest request, int page, int limit, String searchText,
			String order, String orderBy, String subsidiary, String status, String signingStatus,String category)
			throws DataValidationException, UnsupportedEncodingException, JsonProcessingException {

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
		List<LoaContractDocument> list;
		UsersInfoDocument usersInfoDocument=userInfoRespository.findByEmailAndActive(email, true);
		String currentRole=usersInfoDocument.getCurrentRole();
		if (!(currentRole.equalsIgnoreCase("CONTRACT_CREATOR") ||  
				(currentRole.equalsIgnoreCase("PCD_ADMIN") || currentRole.equalsIgnoreCase("SUPER_ADMIN")||currentRole.equalsIgnoreCase("LEGAL_ADMIN")) 
					|| currentRole.equalsIgnoreCase("LEGAL_USER")|| currentRole.equalsIgnoreCase("PCD_USER"))) {
			    throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
			}
		if (order == null) {
			totalCount = loaContractDocumentRepository.findAll().size();
			list = loaContractDocumentRepository.findAll(pageable).getContent();
		} else {
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			Query query = new Query();
			query.collation(collation);
			if (subsidiary != null && !subsidiary.isBlank()) {
				Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
				query.addCriteria(Criteria.where("subsidiary").regex(pattern));
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
				signingStatus=URLDecoder.decode(signingStatus,"UTF-8");
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
				searchText= URLDecoder.decode(searchText, "UTF-8");
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("projectName").regex(pattern),
						Criteria.where("projectId").regex(pattern), Criteria.where("contractNumber").regex(pattern));
				query.addCriteria(criteria);
			}
			log.info("Query {}", query);
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, LoaContractDocument.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query, LoaContractDocument.class).stream().collect(Collectors.toList());
		}
		List<String> listOfEnvelopeIds = list.stream().map(document -> document.getEnvelopeId())
				.collect(Collectors.toList());
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		for (LoaContractDocument loaContractDocument : list) {
			List<String> reviewers = new ArrayList<>();
			List<ReviewerDocument> reviewerslist = reviewerList.stream()
					.filter(document -> document.getEnvelopeId().equals(loaContractDocument.getEnvelopeId()))
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
			DocumentCreateResponse resp = DocumentCreateResponse.builder()
					.projectName(loaContractDocument.getProjectName()).createdOn(loaContractDocument.getCreatedOn())
					.projectId(loaContractDocument.getProjectId()).ownersEmail(loaContractDocument.getSenderEmail())
					.senderName(loaContractDocument.getSenderName())
					.envelopeId(loaContractDocument.getEnvelopeId()).status(loaContractDocument.getStatus())
					.pendingWith(reviewers).commencementDate(loaContractDocument.getCommencementDate())
					.completionDate(loaContractDocument.getCompletionDate())
					.subsidiary(loaContractDocument.getSubsidiary())
					.contractNumber(loaContractDocument.getContractNumber())
					.vendorName(loaContractDocument.getVendorName()).build();
			createResponses.add(resp);
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", createResponses);
		return new CommonResponse(HttpStatus.OK, new Response("CreateListForContractResponse", commonResponse),
				"Contract details fetched successfully");
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
	public CommonResponse newAddLoaContractDocument(String json, String envelopeId, String email, String name)
			throws IOException, TemplateException, MessagingException, DataValidationException {
		
		try {
		LoaContractDocumentRequest jsonrequest = mapper.readValue(json, LoaContractDocumentRequest.class);
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		jsonrequest.setUserEmail(email);
		LoaContractDocument loaContractDocument = new LoaContractDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(LoaContractDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> envelopeIdmap = new HashMap<>();
		loaContractDocument.setId(id);
		loaContractDocument.setProjectId(projectId);
		loaContractDocument.setProjectName(jsonrequest.getProjectName());
		loaContractDocument.setVendorName(jsonrequest.getVendorName());
		loaContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		loaContractDocument.setCompletionDate(jsonrequest.getCompletionDate());
		loaContractDocument.setIsFlowCompleted(false);
		loaContractDocument.setBuID("BUID");
		loaContractDocument.setOpID("SAASPE");
		loaContractDocument.setCreatedOn(new Date());
		loaContractDocument.setCreatedBy(email);
		loaContractDocument.setContractTenure(jsonrequest.getContractTenure());
		loaContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		loaContractDocument.setTemplateId(envelopeId);
		loaContractDocument.setStatus(jsonrequest.getStatus());
		loaContractDocument.setVersion("1.0");
		loaContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		loaContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		Currency currency = new Currency(jsonrequest.getCurrency().getCurrencyCode(),
				jsonrequest.getCurrency().getTotalCost(), jsonrequest.getCurrency().getTax());
		loaContractDocument.setCurrency(currency);
		loaContractDocument.setVendors(jsonrequest.getVendors());
		loaContractDocument.setStampers(jsonrequest.getStampers());
		loaContractDocument.setVendorSigningOrder(jsonrequest.getVendorSigningOrder());
		loaContractDocument.setCvefSigningOrder(jsonrequest.getCvefSigningOrder());
		loaContractDocument.setCvefSigners(jsonrequest.getCvefSigners());
		loaContractDocument.setLhdnStampers(jsonrequest.getLhdnStampers());
		loaContractDocument.setLhdnSigningOrder(jsonrequest.getLhdnSigningOrder());
		loaContractDocument.setRemark(jsonrequest.getRemark());
		List<AllSigners> allSignersList = new ArrayList<>();
		for (AllSigners s : jsonrequest.getAllSigners()) {
				s.setSigningStatus("sent");
				allSignersList.add(s);
		}
		loaContractDocument.setAllSigners(allSignersList);
		loaContractDocument.setIsSignersCompleted("sent");
		loaContractDocument.setIsCvefSignersCompleted("sent");
		loaContractDocument.setIsStampersCompleted("sent");
		loaContractDocument.setIsVendorsCompleted("sent");
		loaContractDocument.setIsLhdnSignersCompleted("sent");
		loaContractDocument.setContractNumber(jsonrequest.getContractNumber());
		loaContractDocument.setCategory(jsonrequest.getCategory());
		loaContractDocument.setWatcherEmailStatus(false);
		jsonrequest.setModuleName(Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY);
		jsonrequest.setContractName(jsonrequest.getProjectName());	
		String buildurl = getDousignUrl().getNewContractDocument().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		log.info("Calling create envelope API");
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
		builder.queryParam(Constant.ENVELOPE_ID, envelopeId);
		builder.queryParam("senderUserId", userId.getUserId());
		String url = builder.toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		ResponseEntity<Object> response = null;
		try {
			response = restTemplate.postForEntity(url, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			log.info("Entering catch block");
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE,Constant.INTERNAL_SERVER_ERROR),
					"LOA Contract Document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		log.info("Success in Create Envelope API");
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String newEnvelopeId = rootNode.get(Constant.ENVELOPE_ID).asText();
		envelopeIdmap.put(Constant.ENVELOPE_ID, newEnvelopeId);
		if (newEnvelopeId != null) {
			log.info("Calling Envelope By Id");
			buildurl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
			String envelopeDataUrl = buildurl + newEnvelopeId;
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				log.info("Inside catch block");
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE,Constant.INTERNAL_SERVER_ERROR),
						"LOA Document Creation Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, new ArrayList<>()),
						ex.getLocalizedMessage());
			}
			log.info("Success in Envelope By Id");
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(newEnvelopeId);
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(newEnvelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getProjectName());
			creator.setFlowType("CONTRACT_create");
			creator.setStatus(jsonrequest.getStatus());
			creator.setSubsidiary(jsonrequest.getSubsidiary());
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(email);
			List<ReviewerDocument> reviewers = new ArrayList<>();
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
				reviewerDocument.setEnvelopeId(newEnvelopeId);
				reviewerDocument.setCreatedOn(new Date());
				reviewerDocument.setCreatorName(name);
				reviewerDocument.setCreatedBy(email);
				reviewerDocument.setContractName(jsonrequest.getProjectName());
				reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				reviewerDocument.setFlowType("CONTRACT_review");
				reviewerDocument.setReviewerName(reviewer.getName());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
				reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
				reviewerDocument.setStatus(jsonrequest.getStatus());
				reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
				reviewerDocument.setCompletionDate(jsonrequest.getCompletionDate());
				reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
				reviewerDocument.setProjectId(projectId);
				reviewerDocument.setContractNumber(jsonrequest.getContractNumber());
				reviewerDocument.setVendorName(jsonrequest.getVendorName());
				if (reviewer.getRoutingOrder() == firstRoutingOrder) {
					pendingWith.add(reviewer.getEmail());
					mailSenderService.sendRequestForReviewMail(newEnvelopeId, reviewer.getEmail(), reviewer.getName(),
							jsonrequest.getProjectName(), loaContractDocument.getVersion(),
							loaContractDocument.getCreatedBy(), "pcd", Constant.CONTRACT_MODULE);
				}
				log.info("Saving Reviewer Document");
				reviewerDocumentRepository.save(reviewerDocument);
				reviewers.add(reviewerDocument);
			}
			creator.setPendingWith(pendingWith);
			creator.setProjectId(projectId);
			log.info("Saving workFlow Document");
			workFlowCreatorDocumentRespository.save(creator);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
			DocumentVersionDocument documentVersion = new DocumentVersionDocument();
			documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
			documentVersion.setEnvelopeId(newEnvelopeId);
			documentVersion.setVersionOrder("1.0");
			documentVersion.setDocVersion(0);
			documentVersion.setCreatedOn(new Date());
			documentVersion.setPath(containerName + newEnvelopeId + "/V" + (documentVersion.getDocVersion() + 1));
			log.info("Saving DocumentVersion Document");
			documentVersionDocumentRepository.save(documentVersion);
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setStartDate(new Date());
			log.info("Saving Envelope Document");
			envelopeRepository.save(envelopeDocument);
			loaContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			loaContractDocument.setSenderName(name);
			loaContractDocument.setEnvelopeId(newEnvelopeId);
			log.info("Contract envelopId: {}", newEnvelopeId);
			log.info("Saving LoaDocument");
			loaContractDocumentRepository.save(loaContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(newEnvelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY);
			log.info("Saving centralRepo Document");
			centralRepoDocumentRepository.save(centralRepoDocument);
		} else {
			throw new DataValidationException("create LOA contract failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED,
				new Response(Constant.LOA_CONTRACT_DOCUMENT_RESPONSE, envelopeIdmap),
				"Loa Contract document details submitted successfully");
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
	public CommonResponse setExpiringEnvelope() {

		List<String> envelopeList = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, -29);
		Date endDate = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, -12);
		List<LoaContractDocument> expiringDocuments = loaContractDocumentRepository.findByCreatedOnBeforeAndStatus(endDate,"created");
		log.info("End Date :{}", endDate);
		if (!expiringDocuments.isEmpty()) {
			expiringDocuments.stream().forEach(e->System.out.println(e.getEnvelopeId()));
			for (LoaContractDocument document : expiringDocuments) {
				log.info("Updating Contract Document Status :{}", document.getEnvelopeId());
				document.setStatus("expired");
				envelopeList.add(document.getEnvelopeId());
				loaContractDocumentRepository.save(document);
			}
			return new CommonResponse(HttpStatus.OK, new Response("Document Update Response", envelopeList),
					"Document upload Successful");
		} else
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Update Response", "No Documents found to update"),
					"Document upload Unsuccessful");
	}
}
