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
import saaspe.clm.document.CmuContractDocument;
import saaspe.clm.document.DocumentVersionDocument;
import saaspe.clm.document.EnvelopeDocument;
import saaspe.clm.document.LooContractDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
import saaspe.clm.docusign.model.DocumentVersioningResponse;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.AllSigners;
import saaspe.clm.model.ApprovedLOOContractResponse;
import saaspe.clm.model.CmuDocumentDetailResponse;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Document;
import saaspe.clm.model.DocumentCreateResponse;
import saaspe.clm.model.DocumentResponse;
import saaspe.clm.model.DocusignUrls;
import saaspe.clm.model.DocusignUserCache;
import saaspe.clm.model.EnvelopeResponse;
import saaspe.clm.model.LooContractDocumentRequest;
import saaspe.clm.model.Response;
import saaspe.clm.model.Reviewers;
import saaspe.clm.model.UserDetails;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.CmuContractDocumentRepository;
import saaspe.clm.repository.DocumentVersionDocumentRepository;
import saaspe.clm.repository.EnvelopeRepository;
import saaspe.clm.repository.LooContractDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.LOOService;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.utills.AllRoleFlowMapper;
import saaspe.clm.utills.Base64ToMultipartFileConverter;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

@Service
public class LOOServiceImpl implements LOOService {

	private static Logger log = LoggerFactory.getLogger(LOOServiceImpl.class);

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
	private LooContractDocumentRepository looContractDocumentRepository;

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private DocumentVersionDocumentRepository documentVersionDocumentRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository workFlowCreatorDocumentRespository;

	@Autowired
	private CmuContractDocumentRepository cmuDocumentRepository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

	@Autowired
	private UserInfoRespository userInfoRespository;
	
	@Autowired
	private MailSenderService mailSenderService;
	
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
	private CloudBlobClient cloudBlobClient;

	@Value("${azure.storage.container.name}")
	private String documentVersionblob;

	Random random = new Random();

	private final MongoTemplate mongoTemplate;

    public LOOServiceImpl(MongoTemplate mongoTemplate) {
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

	public CommonResponse addLooContractDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String envelopeId, String profile, String name) throws Exception {
		LooContractDocumentRequest jsonrequest = mapper.readValue(json, LooContractDocumentRequest.class);
		List<DocumentVersionDocument> documentVersionDocumentList = documentVersionDocumentRepository
				.findByEnvelopeId(envelopeId);
		int minRange = 1000;
		int maxRange = 9999;
		List<String> createIdsList = createId != null ? Arrays.asList(createId) : new ArrayList<>();
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();
		if (createId == null && createDocumentFiles == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()),
					"Provide existing documentId or upload document to send envelope");

		}
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + profile);
		ArrayList<saaspe.clm.model.Document> documentRequests = new ArrayList<>();
		List<MultipartFile> multipartFiles = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				try {
					documentRequest
							.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
					documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
							createDocumentFile.getOriginalFilename().indexOf(".")));
					documentRequest.setCategory(Constant.CREATE_FILE);
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					documentRequest.setDocumentId(String.valueOf(randomNumber));
					multipartFiles.add(Base64ToMultipartFileConverter.convert(documentRequest.getDocumentBase64(),
							documentRequest.getName(), "text/plain"));
					documentRequests.add(documentRequest);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		jsonrequest.setDocuments(documentRequests);

		DocumentVersionDocument documentVersionDocument = documentVersionDocumentList.stream()
				.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion)).get();
		List<Document> documentList = new ArrayList<>();
		documentList = getCMUDocumentFromBlob(documentVersionDocument, createIdsList);
		jsonrequest.setDocuments(documentRequests);
//		if (createId != null) {
//			int i = 0;
//			for (String createid : createIdsList) {
//				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
//				documentRequest.setDocumentId(createIdsList.get(i));
//				documentRequest.setCategory("createId");
//
//				documentRequests.add(documentRequest);
//				i++;
//			}
//		}
		for (Document document : documentList) { // load document from cmu blob
			if (createIdsList.contains(document.getDocumentId()) || createIdsList.isEmpty()) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				documentRequest.setDocumentBase64(document.getDocumentBase64());
				documentRequest.setName(document.getName());
				documentRequest.setCategory("createFile");
				documentRequest.setDocumentId(document.getDocumentId());
				multipartFiles.add(Base64ToMultipartFileConverter.convert(document.getDocumentBase64(),
						document.getName(), "text/plain"));
				documentRequests.add(documentRequest);
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
		jsonrequest.setDocuments(documentRequests);
		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));

		List<Document> versionDocDocuments = new ArrayList<>();

		for (Document document : documentRequests) {
			Document newDoc = new Document();
			newDoc.setDocumentId(document.getDocumentId());
			newDoc.setName(document.getName());
			versionDocDocuments.add(newDoc);
		}
		documentVersion.setDocuments(versionDocDocuments);

		jsonrequest.setUserEmail(profile);
		LooContractDocument looContractDocument = new LooContractDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();

		Long id = sequenceGeneratorService.generateSequence(LooContractDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> projectIdmap = new HashMap<>();
		projectIdmap.put("projectId", projectId);
		looContractDocument.setId(id);
		looContractDocument.setProjectId(projectId);
		looContractDocument.setBuID("BUID");
		looContractDocument.setOpID("SAASPE");
		looContractDocument.setCreatedOn(new Date());
		looContractDocument.setCreatedBy(profile);
		looContractDocument.setIsFlowCompleted(false);
		looContractDocument.setStatus(jsonrequest.getStatus());
		looContractDocument.setVersion("1.0");
		looContractDocument.setOrder(0);
		looContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		looContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		looContractDocument.setContractTitle(jsonrequest.getContractTitle());
		looContractDocument.setTenant(jsonrequest.getTenant());
		looContractDocument.setTenderNo(jsonrequest.getTenderNo());
		looContractDocument.setReferenceNo(jsonrequest.getReferenceNo());
		looContractDocument.setTradingName(jsonrequest.getTradingName());
		looContractDocument.setTenancyTerm(jsonrequest.getTenancyTerm());
		looContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		looContractDocument.setCommencementBusinessDate(jsonrequest.getCommencementBusinessDate());
		looContractDocument.setExpiryDate(jsonrequest.getExpiryDate());
		looContractDocument.setLotNumber(jsonrequest.getLotNumber());
		looContractDocument.setLocation(jsonrequest.getLocation());
		looContractDocument.setArea(jsonrequest.getArea());
		looContractDocument.setAirport(jsonrequest.getAirport());
		looContractDocument.setTemplateId(envelopeId);
		looContractDocument.setVendors(jsonrequest.getVendors());
		looContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		looContractDocument.setAllSigners(jsonrequest.getAllSigners());

		String buildurl = getDousignUrl().getCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST,
				docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
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
					new Response(Constant.LOO_DOCUMENT_RESPONSE, Constant.INTERNAL_SERVER_ERROR),
					"LOO Contract Document Creation Failed");
		}
		catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String newEnvelopeId = rootNode.get(Constant.ENVELOPE_ID).asText();
		log.info("new EnvelopeId : {} :: LOO create response: {}", newEnvelopeId, response);
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
						new Response(Constant.LOO_DOCUMENT_RESPONSE, "Internal Server Error"),
						"LOO Document Creation Failed");
			}
			catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
			}
			documentVersion.setEnvelopeId(newEnvelopeId);
			documentVersion.setVersionOrder("1.0");
			documentVersion.setDocVersion(0);
			documentVersion.setPath(containerName + newEnvelopeId + "/V1");
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
			creator.setFlowType("LOO_Create");
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(profile);
			creator.setTenantName(jsonrequest.getTenant());
			creator.setProjectId(projectId);
			creator.setStartDate(jsonrequest.getCommencementDate());
			creator.setExpiryDate(jsonrequest.getExpiryDate());
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
				reviewerDocument.setContractName(jsonrequest.getContractTitle());
				reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				reviewerDocument.setFlowType("LOO_Review");
				reviewerDocument.setReviewerName(reviewer.getName());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
				reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
				reviewerDocument.setExpiryDate(jsonrequest.getExpiryDate());
				reviewerDocument.setProjectId(projectId);
				if (reviewer.getRoutingOrder() == firstRoutingOrder) {
					pendingWith.add(reviewer.getEmail());
				}
				reviewerDocumentRepository.save(reviewerDocument);
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
			try {
				uploadFilesIntoBlob(multipartFiles, newEnvelopeId, documentRequests,
						"V" + (documentVersion.getDocVersion() + 1), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setDocuments(documentResponses);
			envelopeDocument.setStartDate(new Date());
			envelopeRepository.save(envelopeDocument);
			looContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			looContractDocument.setSenderName(rootNode2.path(Constant.SENDER).path(Constant.USERNAME).asText());
			looContractDocument.setEnvelopeId(newEnvelopeId);
			looContractDocument.setTenants(jsonrequest.getTenants());
			looContractDocument.setTenantSigningOrder(jsonrequest.getTenantSigningOrder());
			looContractDocument.setVendors(jsonrequest.getVendors());
			looContractDocumentRepository.save(looContractDocument);

			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(newEnvelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY);
			centralRepoDocumentRepository.save(centralRepoDocument);
		} else {
			throw new DataValidationException("create LOO contract failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.LOO_DOCUMENT_RESPONSE, projectIdmap),
				"LOO Contract document details submitted successfully");
	}

	@Override
	@Transactional
	public CommonResponse updateLooDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles,
			String[] update_id, String[] delete_id, String envId, String email) throws Exception {
		List<MultipartFile> multipartFiles = new ArrayList<>();
		List<String> documentIds = new ArrayList<>();
		LooContractDocumentRequest jsonrequest = new LooContractDocumentRequest();
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
					documentIds.add(String.valueOf(randomNumber));
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
						documentIds.add(updatedIdsList.get(i));
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
			if (!deleteIdsList.contains(document.getDocumentId())
					&& !updatedIdsList.contains(document.getDocumentId())) {
				String path = currentDocument.getPath() + "/" + document.getDocumentId();
				CloudBlockBlob blob = container.getBlockBlobReference(path);
				InputStream inputStream = blob.openInputStream();
				MultipartFile multipartFile = Base64ToMultipartFileConverter
						.convert(convertBlobInputStreamToBase64(inputStream), document.getName(), "text/plain");
				multipartFiles.add(multipartFile);
				documentIds.add(document.getDocumentId());
			}
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
					new Response("LOO Document Update Response", "Internal Server Error"),
					"LOO Document Creation Failed");
		}
		catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		try {
			uploadFilesIntoBlob(multipartFiles, envId, null, "V" + (newdoc.getDocVersion() + 1), documentIds);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponse(HttpStatus.OK, new Response(Constant.LOO_DOCUMENT_RESPONSE, null),
				"LOO Document updated successfully");
	}

	@Override
	public CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId)
			throws DataValidationException {
		List<DocumentVersioningResponse> response = new ArrayList<>();

		String email = getEmailFromToken(request).getUserEmail();
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		String flowType = (cacheValue != null) ? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()) : "null";

		LooContractDocument looContractDocument = null;
		if (flowType.equalsIgnoreCase("LOO_Create") || flowType.equalsIgnoreCase("LOO_Review")) {
			looContractDocument = looContractDocumentRepository.findByEnvelopeId(envelopeId);
			if (looContractDocument == null) {
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
			documentVersioning.setEnvelopeName(looContractDocument!=null?looContractDocument.getContractTitle():null);
			response.add(documentVersioning);
		}
		Collections.sort(response, Comparator.comparingInt(DocumentVersioningResponse::getOrder).reversed());
		return new CommonResponse(HttpStatus.OK, new Response("Document Versioning Response", response),
				"Documents fetched successfully");
	}

	public String generateProjectId(String value) {
		while (value.length() < 6) {
			value = '0' + value;
		}
		return value;
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
	public CommonResponse getApprovedLOOContractList(HttpServletRequest request) {

		List<LooContractDocument> approvedLoaList = looContractDocumentRepository.findApprovedLOOContractList(true,
				true);
		List<ApprovedLOOContractResponse> list = new ArrayList<>();
		for (LooContractDocument looDocument : approvedLoaList) {
			ApprovedLOOContractResponse response = new ApprovedLOOContractResponse();
			response.setEnvelopeId(looDocument.getEnvelopeId());
			response.setContractName(looDocument.getContractTitle());
			list.add(response);
		}
		return new CommonResponse(HttpStatus.OK, new Response("LOO Contract ApprovedList Response", list),
				"LOO Contract Approved list fetched successfully");
	}

	@Override
	public CommonResponse getLooDocumentDetailsView(String envelopeId)
			throws DataValidationException, JsonProcessingException {
		CmuDocumentDetailResponse documentDetailResponse = new CmuDocumentDetailResponse();
		LooContractDocument looDocument = looContractDocumentRepository.findByEnvelopeId(envelopeId);
		EnvelopeDocument envelopeDocument = envelopeRepository.findByenvelopeId(envelopeId);
		System.out.println(envelopeDocument);
		if (looDocument == null)
			throw new DataValidationException("Please provide the valid envelope ID", "400", HttpStatus.BAD_REQUEST);
		CmuContractDocument cmuDocument = cmuDocumentRepository.findByEnvelopeId(looDocument.getTemplateId());
		log.info("cmuDocument: {}", cmuDocument);
		String json1 = mapper.writeValueAsString(envelopeDocument.getEnvelope());
		JsonNode rootNode = mapper.readTree(json1);
		documentDetailResponse.setEmailSubject(rootNode.get("emailSubject").asText());
		documentDetailResponse.setEmailMessage(rootNode.get("emailBlurb").asText());
		documentDetailResponse.setReferenceNo(cmuDocument.getProjectId());
		documentDetailResponse.setContractTitle(looDocument.getContractTitle());
		documentDetailResponse.setTenderNo(looDocument.getTenderNo());
		documentDetailResponse.setTradingName(looDocument.getTradingName());
		documentDetailResponse.setTenant(looDocument.getTenant());
		documentDetailResponse.setCommencementDate(looDocument.getCommencementDate());
		documentDetailResponse.setCommencementBusinessDate(looDocument.getCommencementBusinessDate());
		documentDetailResponse.setExpiryDate(looDocument.getExpiryDate());
		documentDetailResponse.setLotNumber(looDocument.getLotNumber());
		documentDetailResponse.setLocation(looDocument.getLocation());
		documentDetailResponse.setArea(looDocument.getArea());
		documentDetailResponse.setAirport(looDocument.getLocation());
		documentDetailResponse.setStatus(looDocument.getStatus());
		documentDetailResponse.setTenancyTerm(looDocument.getTenancyTerm());
		documentDetailResponse.setEnvelopeId(looDocument.getEnvelopeId());
		documentDetailResponse.setSenderEmail(looDocument.getSenderEmail());
		documentDetailResponse.setSenderName(looDocument.getSenderName());
		documentDetailResponse.setTemplateName(cmuDocument != null ? cmuDocument.getContractTitle() : null);
		documentDetailResponse.setVendors(looDocument.getVendors());
		documentDetailResponse.setTenants(looDocument.getTenants());
		documentDetailResponse.setTenantSigningOrder(looDocument.getTenantSigningOrder());
		documentDetailResponse.setSubsidiary(looDocument.getSubsidiary());
		documentDetailResponse.setAllSigners(looDocument.getAllSigners());
		documentDetailResponse.setCategory(looDocument.getCategory());
		return new CommonResponse(HttpStatus.OK, new Response("LOO Document Response", documentDetailResponse),
				"LOO Document details Retrieved Successfully");
	}

	public CommonResponse getCreateListForLoo(HttpServletRequest request, int page, int limit, String searchText,
			String order, String orderBy, String subsidiary, String status,String category) throws DataValidationException, UnsupportedEncodingException {
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
		String email = getUserFromToken(request).getUserEmail();
		long totalCount = 0;
		UsersInfoDocument usersInfoDocument=userInfoRespository.findByEmailAndActive(email, true);
		String currentRole=usersInfoDocument.getCurrentRole();
		if (!(currentRole.equalsIgnoreCase("LOO_CREATOR") || 
			      (currentRole.equalsIgnoreCase("COMMERCIAL_ADMIN") || currentRole.equalsIgnoreCase("SUPER_ADMIN"))
			      || currentRole.equalsIgnoreCase("COMMERCIAL_USER"))) {
			    throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
			}
		List<LooContractDocument> list;
		if (order == null) {
			totalCount = looContractDocumentRepository.findAll().size();
			list = looContractDocumentRepository.findAll(pageable).getContent();
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
			if(searchText!=null&&!searchText.isEmpty()) {
				searchText= URLDecoder.decode(searchText, "UTF-8");
				Pattern pattern = Pattern.compile(Pattern.quote(searchText),Pattern.CASE_INSENSITIVE);
				criteria.orOperator(
						Criteria.where("contractTitle").regex(pattern),
						Criteria.where("projectId").regex(pattern));
				query.addCriteria(criteria);
			}
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, LooContractDocument.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query,LooContractDocument.class).stream().collect(Collectors.toList());
		}
		List<String> listOfEnvelopeIds = list.stream().map(document -> document.getEnvelopeId())
				.collect(Collectors.toList());

		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		for (LooContractDocument looContractDocument : list) {
			List<String> reviewers = new ArrayList<>();
			List<ReviewerDocument> reviewerslist = reviewerList.stream()
					.filter(document -> document.getEnvelopeId().equals(looContractDocument.getEnvelopeId()))
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
					.contractTitle(looContractDocument.getContractTitle()).createdOn(looContractDocument.getCreatedOn())
					.projectId(looContractDocument.getProjectId()).ownersEmail(email)
					.senderName(looContractDocument.getSenderName()).envelopeId(looContractDocument.getEnvelopeId())
					.status(looContractDocument.getStatus()).pendingWith(reviewers)
					.commencementDate(looContractDocument.getCommencementDate())
					.expiryDate(looContractDocument.getExpiryDate()).subsidiary(looContractDocument.getSubsidiary())
					.tenant(looContractDocument.getTenant())
					.build();

			createResponses.add(resp);
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", createResponses);
		return new CommonResponse(HttpStatus.OK, new Response("CreateListForLooResponse", commonResponse),
				"Loo details fetched successfully");
	}

	public List<Document> getCMUDocumentFromBlob(DocumentVersionDocument documentVersionDocument,
			List<String> createIdsList) throws Exception {
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		List<Document> documents = new ArrayList<>();
		for (Document document : documentVersionDocument.getDocuments()) {
			if (!createIdsList.isEmpty() && !createIdsList.contains(document.getDocumentId())) {
				continue;
			}
			Document newDocument = new Document();
			String path = documentVersionDocument.getPath() + "/" + document.getDocumentId();
			CloudBlockBlob blob = container.getBlockBlobReference(path);
			log.info("blob uri: {}", blob.getUri());
			InputStream inputStream = blob.openInputStream();
			newDocument.setDocumentBase64(convertBlobInputStreamToBase64(inputStream));
			newDocument.setDocumentId(document.getDocumentId());
			newDocument.setCategory(Constant.CREATE_FILE);
			newDocument.setName(document.getName());
			documents.add(newDocument);
		}
		return documents;
	}

	public String convertBlobInputStreamToBase64(InputStream inputStream) throws Exception {
		try {
			byte[] bytes = IOUtils.toByteArray(inputStream);
			return Base64.getEncoder().encodeToString(bytes);
		} finally {
			inputStream.close();
		}
	}

	private void uploadFilesIntoBlob(List<MultipartFile> multipartFiles, String envelopeId,
			List<Document> documentResponses, String version, List<String> documentIds)
			throws URISyntaxException, StorageException, IOException {
		String path = envelopeId;
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		for (int i = 0; i < multipartFiles.size(); i++) {
			MultipartFile file = multipartFiles.get(i);
			String documentId = (version.equals("V1")) ? documentResponses.get(i).getDocumentId() : documentIds.get(i);
			CloudBlockBlob blob = container
					.getBlockBlobReference(containerName + path + "/" + version + "/" + documentId);
			log.info("blob:: {}", blob);
			blob.getProperties().setContentType("application/pdf");

			try (InputStream inputStream = file.getInputStream()) {
				blob.upload(inputStream, file.getSize());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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

	private UserDetails getUserFromToken(HttpServletRequest request) {
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
	public CommonResponse newAddLooContractDocument(String json, String templateId, String email, String name)
			throws DataValidationException, IOException, TemplateException, MessagingException {
	try {
		LooContractDocumentRequest jsonrequest = mapper.readValue(json, LooContractDocumentRequest.class);
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		jsonrequest.setUserEmail(email);
		LooContractDocument looContractDocument = new LooContractDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(LooContractDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		Map<String, String> envelopeIdmap = new HashMap<>();
		looContractDocument.setId(id);
		looContractDocument.setProjectId(projectId);
		looContractDocument.setBuID("BUID");
		looContractDocument.setOpID("SAASPE");
		looContractDocument.setCreatedOn(new Date());
		looContractDocument.setCreatedBy(email);
		looContractDocument.setIsFlowCompleted(false);
		looContractDocument.setStatus(jsonrequest.getStatus());
		looContractDocument.setVersion("1.0");
		looContractDocument.setOrder(0);
		looContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		looContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		looContractDocument.setContractTitle(jsonrequest.getContractTitle());
		looContractDocument.setTenant(jsonrequest.getTenant());
		looContractDocument.setTenderNo(jsonrequest.getTenderNo());
		looContractDocument.setReferenceNo(jsonrequest.getReferenceNo());
		looContractDocument.setTradingName(jsonrequest.getTradingName());
		looContractDocument.setTenancyTerm(jsonrequest.getTenancyTerm());
		looContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		looContractDocument.setCommencementBusinessDate(jsonrequest.getCommencementBusinessDate());
		looContractDocument.setExpiryDate(jsonrequest.getExpiryDate());
		looContractDocument.setLotNumber(jsonrequest.getLotNumber());
		looContractDocument.setLocation(jsonrequest.getLocation());
		looContractDocument.setArea(jsonrequest.getArea());
		looContractDocument.setAirport(jsonrequest.getAirport());
		looContractDocument.setTemplateId(templateId);
		looContractDocument.setVendors(jsonrequest.getVendors());
		looContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		looContractDocument.setCategory(jsonrequest.getCategory());
		List<AllSigners> allSignersList = new ArrayList<>();
		for (AllSigners s : jsonrequest.getAllSigners()) {
				s.setSigningStatus("sent");
				allSignersList.add(s);
		}
		looContractDocument.setIsSignersCompleted("sent");
		looContractDocument.setIsTenantsCompleted("sent");
		looContractDocument.setAllSigners(allSignersList);
		looContractDocument.setWatcherEmailStatus(false);
		jsonrequest.setModuleName(Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY);
		jsonrequest.setContractName(jsonrequest.getContractTitle());	
		String buildurl = getDousignUrl().getNewCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST,
				docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
		builder.queryParam("templateId", templateId);
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
					new Response(Constant.LOO_DOCUMENT_RESPONSE, "Internal Server Error"),
					"LOO Contract Document Creation Failed");
		}
		catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String newEnvelopeId = rootNode.get("envelopeId").asText();
		envelopeIdmap.put("envelopeId", newEnvelopeId);
		log.info("new EnvelopeId : {} :: LOO create response: {}", newEnvelopeId, response);
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
						new Response(Constant.LOO_DOCUMENT_RESPONSE, "Internal Server Error"),
						"LOO Document Creation Failed");
			}
			catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOO_DOCUMENT_RESPONSE, new ArrayList<>()), ex.getLocalizedMessage());
			}
			documentVersion.setEnvelopeId(newEnvelopeId);
			documentVersion.setVersionOrder("1.0");
			documentVersion.setDocVersion(0);
			documentVersion.setPath(containerName + newEnvelopeId + "/V1");
			documentVersion.setCreatedOn(new Date());
			log.info("Saving document Version Document");
			documentVersionDocumentRepository.save(documentVersion);
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(newEnvelopeId);
			Date createDate = new Date();
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(newEnvelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getContractTitle());
			creator.setFlowType("LOO_Create");
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(email);
			creator.setTenantName(jsonrequest.getTenant());
			creator.setProjectId(projectId);
			creator.setStartDate(jsonrequest.getCommencementDate());
			creator.setExpiryDate(jsonrequest.getExpiryDate());
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
				reviewerDocument.setCreatedBy(email);
				reviewerDocument.setContractName(jsonrequest.getContractTitle());
				reviewerDocument.setSenderName(rootNode.path(Constant.SENDER).path("userName").asText());
				reviewerDocument.setFlowType("LOO_Review");
				reviewerDocument.setReviewerName(reviewer.getName());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
				reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
				reviewerDocument.setExpiryDate(jsonrequest.getExpiryDate());
				reviewerDocument.setProjectId(projectId);
				reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
				reviewerDocument.setStatus(jsonrequest.getStatus());
				reviewerDocument.setTenant(jsonrequest.getTenant());
				if (reviewer.getRoutingOrder() == firstRoutingOrder) {
					pendingWith.add(reviewer.getEmail());
					mailSenderService.sendRequestForReviewMail(newEnvelopeId, reviewer.getEmail(), reviewer.getName(),
							jsonrequest.getContractTitle(), looContractDocument.getVersion(),
							looContractDocument.getCreatedBy(), "commercial", Constant.LOO_MODULE);
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
			envelopeRepository.save(envelopeDocument);
			looContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			looContractDocument.setSenderName(rootNode2.path(Constant.SENDER).path(Constant.USERNAME).asText());
			looContractDocument.setEnvelopeId(newEnvelopeId);
			looContractDocument.setTenants(jsonrequest.getTenants());
			looContractDocument.setTenantSigningOrder(jsonrequest.getTenantSigningOrder());
			looContractDocument.setVendors(jsonrequest.getVendors());
			log.info("Saving LOO Contract Document");
			looContractDocumentRepository.save(looContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(newEnvelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY);
			log.info("Saving Central Repo Document");
			centralRepoDocumentRepository.save(centralRepoDocument);
		} else {
			throw new DataValidationException("create LOO contract failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.LOO_DOCUMENT_RESPONSE, envelopeIdmap),
				"LOO Contract document details submitted successfully");
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
	public CommonResponse cmutemplateDocumentUploadToEnvelope(String envelopeId, String templateId, String docRequest,
			String envelopeDocumentId, String flowType,String email) throws Exception {

		try {
			UsersInfoDocument usersInfoDocument=userInfoRespository.findByEmailAndActive(email, true);
			if (!restrictionApi(usersInfoDocument, envelopeId))
				throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
			DocumentResponse jsonrequest = mapper.readValue(docRequest, DocumentResponse.class);
			List<Document> documentList = new ArrayList<>();
			List<DocumentVersionDocument> documentVersionDocumentList = documentVersionDocumentRepository
					.findByEnvelopeId(templateId);
			List<DocumentVersionDocument> LooVersionDocumentList = documentVersionDocumentRepository
					.findByEnvelopeId(envelopeId);
			if (!documentVersionDocumentList.isEmpty()) {
				DocumentVersionDocument documentVersionDocument = documentVersionDocumentList.stream()
						.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion)).get();
				
				DocumentVersionDocument LooVersionDocument = LooVersionDocumentList.stream()
						.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion)).get();
				
				documentList = getCMUDocumentFromBlob(documentVersionDocument,
						Arrays.asList(jsonrequest.getDocumentId()));
				MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(
						documentList.get(0).getDocumentBase64(), documentList.get(0).getName() + ".pdf", "text/plain");

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.MULTIPART_FORM_DATA);
				MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

				body.add("document-file", multipartFile.getResource());
				body.add("envelopeId", envelopeId);
				body.add("documentId", envelopeDocumentId);
				HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
				try {
					ResponseEntity<Object> responseEntity = restTemplate.exchange(docusignHost + "/updateDocument",
							HttpMethod.POST, requestEntity, Object.class);
					if (responseEntity.getStatusCode() == HttpStatus.OK) {
						CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
						CloudBlockBlob source = container.getBlockBlobReference(containerName + templateId + "/V"
								+ (documentVersionDocument.getDocVersion() + 1) + "/" + jsonrequest.getDocumentId());
						CloudBlockBlob destination = null;
						if (!container.getBlockBlobReference(
								containerName + envelopeId + "/V" + (documentVersionDocument.getDocVersion() + 1))
								.exists()) {
							destination = container.getBlockBlobReference(containerName + envelopeId + "/V"
									+ (LooVersionDocument.getDocVersion() + 1) + "/" + envelopeDocumentId);
						}
						destination.startCopy(source);
						if (multipartFile.getOriginalFilename() != null) {
							getFileUpload(envelopeId, envelopeDocumentId, multipartFile.getOriginalFilename());
						} else {
							getFileUpload(envelopeId, envelopeDocumentId, "Document-" + envelopeDocumentId);
						}
					}
					return new CommonResponse(HttpStatus.OK, new Response("Document Upload Response", "CMU Document uploaded successfully"),
							"Document upload successfull");

				} catch (Exception e) {
					e.printStackTrace();
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response("Document Upload Response", "Internal Server Error"),
							"Document upload Unsuccessfull");
				}
			} else {
				throw new DataValidationException("No Document foud for provided envelopeId", "400",
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Upload Response", e.getMessage()), "Document upload Unsuccessfull");
		}
	}

	public CommonResponse getFileUpload(String envelopeId, String documentId, String name)
			throws DataValidationException {
		List<DocumentVersionDocument> documentVersionList = documentVersionDocumentRepository
				.findByEnvelopeId(envelopeId);
		if (!documentVersionList.isEmpty()) {
			DocumentVersionDocument versionDocument = documentVersionList.get(0);
			saaspe.clm.model.Document document = new saaspe.clm.model.Document();
			document.setDocumentId(documentId);
			document.setUploaded(true);
			document.setName(name);
			if (versionDocument.getDocuments() != null) {
				versionDocument.getDocuments().add(document);
			} else {
				versionDocument.setDocuments(Arrays.asList(document));
			}
			documentVersionDocumentRepository.save(versionDocument);
			return new CommonResponse(HttpStatus.OK, new Response("File Upload Status Response", null),
					"File Details Retrieved Successfully");
		} else {
			throw new DataValidationException("No DocumentVersionDocument found with envelopeId: " + envelopeId, "400",
					HttpStatus.BAD_REQUEST);
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
		List<LooContractDocument> expiringDocuments = looContractDocumentRepository.findByCreatedOnBeforeAndStatus(endDate,"created");
		log.info("End Date :{}", endDate);
		if (!expiringDocuments.isEmpty()) {
			expiringDocuments.stream().forEach(e->System.out.println(e.getEnvelopeId()));
			for (LooContractDocument document : expiringDocuments) {
				log.info("Updating LOO Document Status :{}",document.getEnvelopeId());
				document.setStatus("expired");
				envelopeList.add(document.getEnvelopeId());
				looContractDocumentRepository.save(document);
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
		} 
		else if (commercialEnvelope.contains(reponame) && Constant.COMMERCIAL_ADMIN_ACCESS_LIST.contains(currentRole)) {
			flag = true;
		}
		return flag;
	}

}
