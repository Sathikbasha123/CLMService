package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Data;
import saaspe.clm.configuration.mongo.SequenceGeneratorService;
import saaspe.clm.constant.Constant;
import saaspe.clm.custom.CustomUserDetails;
import saaspe.clm.document.CentralRepoDocument;
import saaspe.clm.document.ClmContractDocument;
import saaspe.clm.document.CreateTemplate;
import saaspe.clm.document.DocumentVersionDocument;
import saaspe.clm.document.EnvelopeDocument;
import saaspe.clm.document.EnvelopeLockDocument;
import saaspe.clm.document.LoaContractDocument;
import saaspe.clm.document.LoaDocument;
import saaspe.clm.document.LooContractDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.TADocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
import saaspe.clm.docusign.model.DocumentVersioningResponse;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.ClmContractListPagination;
import saaspe.clm.model.ClmContractListResponce;
import saaspe.clm.model.ClmContractRequest;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.ConsoleViewErrorResponse;
import saaspe.clm.model.CreateTemplateModel;
import saaspe.clm.model.Currency;
import saaspe.clm.model.DashboardViewResponce;
import saaspe.clm.model.DocumentResponse;
import saaspe.clm.model.DocusignUrls;
import saaspe.clm.model.DocusignUserCache;
import saaspe.clm.model.EnvelopeResponse;
import saaspe.clm.model.ExpiringContractResponce;
import saaspe.clm.model.LatestContractResponce;
import saaspe.clm.model.LoaDocumentRequest;
import saaspe.clm.model.PostCommentMailBody;
import saaspe.clm.model.Response;
import saaspe.clm.model.Reviewers;
import saaspe.clm.model.SendDocumentPdfMail;
import saaspe.clm.model.TemplateListPagination;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.ClmContractDocumentRepository;
import saaspe.clm.repository.CreateTemplateRepository;
import saaspe.clm.repository.DocumentVersionDocumentRepository;
import saaspe.clm.repository.EnvelopeLockDocumentRepository;
import saaspe.clm.repository.EnvelopeRepository;
import saaspe.clm.repository.LoaContractDocumentRepository;
import saaspe.clm.repository.LoaDocumentRepository;
import saaspe.clm.repository.LooContractDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.TADocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.CLMService;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.utills.FlowType;
import saaspe.clm.utills.RedisUtility;

@Service
public class CLMServiceImpl implements CLMService {

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RedisUtility redisUtility;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private MailSenderService mailSenderService;

	@Value("${redirecturl.path}")
	private String redirectUrl;

	@Autowired
	private Configuration config;

	@Value("${sendgrid.domain.name}")
	private String mailDomainName;

	@Autowired
	private LoaDocumentRepository loaDocumentRepository;

	@Value("${sendgrid.domain.orgname}")
	private String senderName;

	@Value("${commercial.folder.id}")
	private String commercialFolderId;

	@Value("${pcd.folder.id}")
	private String pcdFolderId;

	@Value("${azure.storage.container.name}")
	private String containerName;

	@Value("${sendgrid.domainname}")
	private String domainName;

	@Autowired
	private EnvelopeRepository envelopeRepository;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Autowired
	private ClmContractDocumentRepository clmContractDocumentRepository;

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
	private EnvelopeLockDocumentRepository envelopeLockDocumentRepository;

	@Autowired
	private LoaContractDocumentRepository loaContractDocumentRepository;

	@Autowired
	private LooContractDocumentRepository looContractDocumentRepository;

	@Autowired
	private TADocumentRepository taDocumentRepository;

	@Autowired
	private UserInfoRespository infoRespository;

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

	Random random = new Random();

	private final MongoTemplate mongoTemplate;

	private static final Logger log = LoggerFactory.getLogger(CLMServiceImpl.class);

	public CLMServiceImpl(MongoTemplate mongoTemplate) {
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
	public CommonResponse addClmContract(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId, String profile, String name)
			throws DataValidationException, JsonProcessingException, IOException {
		ClmContractRequest jsonrequest = mapper.readValue(json, ClmContractRequest.class);
		int minRange = 1000;
		int maxRange = 9999;
		List<String> createIdsList = createId != null ? Arrays.asList(createId) : new ArrayList<>();
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();
		if (createId == null && createDocumentFiles == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.CLM_CONTRACT_RESPONSE, new ArrayList<>()),
					"Provide existing documentId or upload document to send envelope");
		}
		String buildurl = getDousignUrl().getListTemplateById().replace(Constant.DOCUSIGN_HOST, docusignHost);
		UriComponentsBuilder builderTemplate = UriComponentsBuilder.fromHttpUrl(buildurl);
		builderTemplate.path(templateId);
		String urlTemplate = builderTemplate.toUriString();
		HttpHeaders headersTemplate = new HttpHeaders();
		headersTemplate.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<Object> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(urlTemplate, HttpMethod.GET, null, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.CLM_CONTRACT_RESPONSE, errorResponse), Constant.CONTRACT_CREATION_FAILED);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.CLM_CONTRACT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.CLM_CONTRACT_RESPONSE, null),
					Constant.CONTRACT_CREATION_FAILED);
		}
		if (responseEntity.getBody() == null) {
			throw new DataValidationException("Template id provided is not in the existing list", null, null);
		}
		ArrayList<saaspe.clm.model.Document> documentRequests = new ArrayList<>();
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
				documentRequest.setCategory(Constant.DELETE);
				documentRequests.add(documentRequest);
			}
		}
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(profile);
		ClmContractDocument clmContractDocument = new ClmContractDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		clmContractDocument.setId(sequenceGeneratorService.generateSequence(ClmContractDocument.SEQUENCE_NAME));
		clmContractDocument.setTemplateId(templateId);
		clmContractDocument.setContractName(jsonrequest.getContractName());
		clmContractDocument.setContractStartDate(jsonrequest.getContractStartDate());
		clmContractDocument.setContractEndDate(jsonrequest.getContractEndDate());
		clmContractDocument.setBuID("BUID");
		clmContractDocument.setOpID("SAASPE");
		clmContractDocument.setCreatedOn(new Date());
		clmContractDocument.setCreatedBy(profile);
		clmContractDocument.setRenewalReminderNotification(jsonrequest.getRenewalReminderNotification());
		clmContractDocument.setContractPeriod(jsonrequest.getContractPeriod());
		clmContractDocument.setStatus("sent");
		clmContractDocument.setVersion("1.0");
		clmContractDocument.setOrder(0);
		clmContractDocument.setUniqueString(generateRandomString(6));
		clmContractDocument.setReferenceId(templateId);
		clmContractDocument.setReferenceType("Template");
		clmContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		clmContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());

		clmContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		clmContractDocument.setCompletionDate(jsonrequest.getCompletionDate());

		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		buildurl = getDousignUrl().getCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
		builder.queryParam(Constant.TEMPLATE_ID, templateId);
		String url = builder.toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		ResponseEntity<Object> response = null;
		try {
			response = restTemplate.postForEntity(url, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.CLM_CONTRACT_RESPONSE, errorResponse), Constant.CONTRACT_CREATION_FAILED);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.CLM_CONTRACT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.CLM_CONTRACT_RESPONSE, null),
					Constant.CONTRACT_CREATION_FAILED);
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String envelopeId = rootNode.get(Constant.ENVELOPE_ID).asText();
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
				String responseBody = ex.getResponseBodyAsString();
				Object errorResponse = mapper.readValue(responseBody, Object.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.CLM_CONTRACT_RESPONSE, errorResponse), Constant.CONTRACT_CREATION_FAILED);
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.CLM_CONTRACT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.CLM_CONTRACT_RESPONSE, null),
						ex.getLocalizedMessage());
			}
			documentVersion.setEnvelopeId(envelopeId);
			documentVersion.setVersionOrder("1.0");
			documentVersion.setDocVersion(1);
			documentVersion.setPath(containerName + "/" + envelopeId + "/V" + documentVersion.getDocVersion() + 1);
			documentVersionDocumentRepository.save(documentVersion);
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(envelopeId);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
			Date createDate = new Date();
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(envelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getContractName());
			creator.setFlowType("ContractFlow");
			List<String> pendingWith = new ArrayList<>();
			creator.setEmail(profile);
			List<ReviewerDocument> reviewers = new ArrayList<>();
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
				reviewerDocument.setContractName(jsonrequest.getContractName());
				reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == 1 ? true : false);
				if (reviewer.getRoutingOrder() == 1) {
					pendingWith.add(reviewer.getEmail());
				}
				reviewerDocumentRepository.save(reviewerDocument);
				reviewers.add(reviewerDocument);
			}
			creator.setPendingWith(pendingWith);
			workFlowCreatorDocumentRespository.save(creator);
			List<DocumentResponse> documentResponses = new ArrayList<>();
			for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
				DocumentResponse docResponse = new DocumentResponse();
				docResponse.setDocumentId(documentResponse.getDocumentId());
				docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
				docResponse.setName(documentResponse.getName());
//				docResponse.setDocumentBase64(documentResponse.getDocumentBase64());
				documentResponses.add(documentResponse);
			}
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setDocuments(documentResponses);
			envelopeDocument.setStartDate(new Date());
			envelopeRepository.save(envelopeDocument);
			clmContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			clmContractDocument.setEnvelopeId(envelopeId);
			clmContractDocumentRepository.save(clmContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(envelopeId);
			centralRepoDocument.setRepositoryName(Constant.CMU_DOCUMENT_REPOSITORY);
			centralRepoDocumentRepository.save(centralRepoDocument);
		} else {
			throw new DataValidationException("create contract failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.CLM_CONTRACT_RESPONSE, envelopeId),
				"Contract details submitted successfully");
	}

	@Override
	public CommonResponse getListOfClmContract(int page, int limit, HttpServletRequest request, String status,
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
		String xAuthProvider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		String email = null;
		long totalCount = 0;
		if (xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
		}

		List<ClmContractListResponce> clMContractEntityList = new ArrayList<>();
		if (status != null) {
			if (Constant.CLM_ENVELOPE_STATUS.stream().noneMatch(status::equalsIgnoreCase)) {
				throw new DataValidationException(
						"Status should be in the following values completed, created, declined, delivered, sent, signed, voided",
						null, null);
			}

			List<ClmContractDocument> list;
			if (order == null) {
				pageable = PageRequest.of(page, limit);
				totalCount = clmContractDocumentRepository.countByStatusAndSenderEmail(status, email);
				list = clmContractDocumentRepository.findAllContractsByStatus(pageable, status, email);
			} else {
				Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
				if (searchText != null && !searchText.isEmpty()) {
					totalCount = clmContractDocumentRepository.countByCollatedFieldByStatus(orderBy, "^" + searchText,
							email, status, collation);
					list = clmContractDocumentRepository.findByCollatedFieldByStatus(orderBy, "^" + searchText, email,
							status, pageable, collation);
				} else {
					totalCount = clmContractDocumentRepository.countByCollatedFieldByStatus(orderBy, "^", email, status,
							collation);
					list = clmContractDocumentRepository.findByCollatedFieldByStatus(orderBy, "^", email, status,
							pageable, collation);
				}

			}

			for (ClmContractDocument contractDocument : list) {

				EnvelopeDocument envelope = envelopeRepository.findByEnvelopeId(contractDocument.getEnvelopeId());
				String json1 = mapper.writeValueAsString(envelope.getEnvelope());
				JsonNode rootNode = mapper.readTree(json1);
				ClmContractListResponce contractresponce = new ClmContractListResponce();
				contractresponce.setContractId(contractDocument.getId());
				contractresponce.setTemplateId(contractDocument.getTemplateId());
				contractresponce.setContractName(contractDocument.getContractName());
				contractresponce.setContractStartDate(contractDocument.getContractStartDate());
				contractresponce.setContractEndDate(contractDocument.getContractEndDate());
				contractresponce.setRenewalReminderNotification(contractDocument.getRenewalReminderNotification());
				contractresponce.setTemplateId(contractDocument.getTemplateId());
				contractresponce.setEnvelopeId(contractDocument.getEnvelopeId());
				contractresponce.setContractPeriod(contractDocument.getContractPeriod());
				contractresponce.setSenderName(rootNode.path(Constant.SENDER).path(Constant.USER_NAME).asText());
				contractresponce.setSenderMail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
				contractresponce.setStatus(rootNode.path(Constant.STATUS).asText());
				contractresponce.setStartDate(rootNode.path(Constant.CREATED_DATE_TIME).asText());
				if (!rootNode.get(Constant.COMPLETED_DATE_TIME).isNull()) {
					contractresponce.setCompeletedDate(rootNode.get(Constant.COMPLETED_DATE_TIME).asText());
				} else {
					contractresponce.setCompeletedDate(null);
				}
				clMContractEntityList.add(contractresponce);
			}

		} else {
			List<ClmContractDocument> list;
			if (order == null) {
				pageable = PageRequest.of(page, limit);
				totalCount = clmContractDocumentRepository.countBySenderEmail(email);
				list = clmContractDocumentRepository.findAllContracts(email, pageable);
			} else {
				Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());

				if (searchText != null && !searchText.isEmpty()) {
					totalCount = clmContractDocumentRepository.countByCollatedField(orderBy, "^" + searchText, email,
							collation);
					list = clmContractDocumentRepository.findByCollatedField(orderBy, "^" + searchText, email, pageable,
							collation);

				} else {
					totalCount = clmContractDocumentRepository.countByCollatedField(orderBy, "^", email, collation);
					list = clmContractDocumentRepository.findByCollatedField(orderBy, "^", email, pageable, collation);
				}

			}
			for (ClmContractDocument clms : list) {
				EnvelopeDocument envelopedocuemnt = envelopeRepository.findByEnvelopeId(clms.getEnvelopeId());
				ClmContractListResponce contractresponce = new ClmContractListResponce();
				contractresponce.setContractId(clms.getId());
				contractresponce.setTemplateId(clms.getTemplateId());
				contractresponce.setContractName(clms.getContractName());
				contractresponce.setContractStartDate(clms.getContractStartDate());
				contractresponce.setContractEndDate(clms.getContractEndDate());
				contractresponce.setRenewalReminderNotification(clms.getRenewalReminderNotification());
				contractresponce.setTemplateId(clms.getTemplateId());
				contractresponce.setEnvelopeId(clms.getEnvelopeId());
				contractresponce.setContractPeriod(clms.getContractPeriod());

				if (envelopedocuemnt != null) {
					String json1 = mapper.writeValueAsString(envelopedocuemnt.getEnvelope());
					JsonNode rootNode = mapper.readTree(json1);

					contractresponce.setSenderName(rootNode.path(Constant.SENDER).path(Constant.USER_NAME).asText());
					contractresponce.setSenderMail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
					contractresponce.setStatus(rootNode.path(Constant.STATUS).asText());
					contractresponce.setStartDate(rootNode.path(Constant.CREATED_DATE_TIME).asText());
					clms.setSenderEmail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
					clmContractDocumentRepository.save(clms);

					if (!rootNode.get(Constant.COMPLETED_DATE_TIME).isNull()) {
						contractresponce.setCompeletedDate(rootNode.get(Constant.COMPLETED_DATE_TIME).asText());
					} else {
						contractresponce.setCompeletedDate(null);
					}
				} else {
					contractresponce.setSenderName(null);
					contractresponce.setSenderMail(null);
					contractresponce.setStatus(null);
					contractresponce.setStartDate(null);
					contractresponce.setCompeletedDate(null);
				}
				clMContractEntityList.add(contractresponce);
			}
		}
		ClmContractListPagination data = new ClmContractListPagination(totalCount, clMContractEntityList);
		Response responseData = new Response(Constant.CLM_CONTRACT_RESPONSE, data);
		return new CommonResponse(HttpStatus.OK, responseData, "Contract details fetched successfully");
	}

	@Override
	public CommonResponse getClmContractDetailsView(String envelopeId, Authentication authentication)
			throws DataValidationException, JsonProcessingException {
		CustomUserDetails profile = (CustomUserDetails) authentication.getPrincipal();
		String email = profile.getEmail();
		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		ContractDetailsViewResponse detailsViewResponse = new ContractDetailsViewResponse();
		ClmContractDocument contract = clmContractDocumentRepository.findByEnvelopeId(envelopeId);
		EnvelopeDocument envelopeDocument = envelopeRepository.findByenvelopeId(envelopeId);
		String json1 = mapper.writeValueAsString(envelopeDocument.getEnvelope());
		JsonNode rootNode = mapper.readTree(json1);
		if (contract == null) {
			throw new DataValidationException("Please provide valid Envelope Id", null, null);
		}
		detailsViewResponse.setContractName(contract.getContractName());
		detailsViewResponse.setContractStartDate(contract.getContractStartDate());
		detailsViewResponse.setContractEndDate(contract.getContractEndDate());
		detailsViewResponse.setRenewalReminderNotification(contract.getRenewalReminderNotification());
		detailsViewResponse.setContractPeriod(contract.getContractPeriod());
		detailsViewResponse.setSenderName(rootNode.get(Constant.SENDER).get(Constant.USER_NAME).asText());
		detailsViewResponse.setSenderEmail(rootNode.get(Constant.SENDER).get(Constant.EMAIL).asText());
		detailsViewResponse.setCreatedDate(rootNode.get(Constant.CREATED_DATE_TIME).asText());
		detailsViewResponse.setDeliveredDate(rootNode.get("deliveredDateTime").asText());
		detailsViewResponse.setExpiryDate(rootNode.get("expireDateTime").asText());
		detailsViewResponse.setEmailSubject(rootNode.get("emailSubject").asText());
		detailsViewResponse.setStatus(rootNode.get(Constant.STATUS).asText());
		if (rootNode.get(Constant.COMPLETED_DATE_TIME).asText() != null) {
			detailsViewResponse.setCompletedDate(rootNode.get(Constant.COMPLETED_DATE_TIME).asText());
		} else {
			detailsViewResponse.setCompletedDate(null);
		}
		return new CommonResponse(HttpStatus.OK, new Response(Constant.CLM_CONTRACT_RESPONSE, detailsViewResponse),
				"Contract Details Retrieved Successfully");
	}

	@Data
	public class ContractDetailsViewResponse {
		private String contractName;
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		private Date contractStartDate;
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		private Date contractEndDate;
		private int renewalReminderNotification;
		private String senderName;
		private String senderEmail;
		private String createdDate;
		private String deliveredDate;
		private String expiryDate;
		private String completedDate;
		private String emailSubject;
		private String status;
		private int contractPeriod;
	}

	@Override
	@Transactional
	public CommonResponse upCommingContractRenewalReminderEmail()
			throws IOException, TemplateException, DataValidationException, MessagingException {
		List<ClmContractDocument> clMContractEntities = clmContractDocumentRepository.findAll().stream()
				.filter(s -> s.getEnvelopeId().equalsIgnoreCase("20280ff9-6e86-4827-a277-7b709da5ae87"))
				.collect(Collectors.toList());
		if (clMContractEntities == null) {
			throw new DataValidationException("There is no contracts for renewal-reminder", null, null);
		}
		for (ClmContractDocument contractDetails : clMContractEntities) {
			if (contractDetails.getContractEndDate() != null) {
				long timeDifference = contractDetails.getContractEndDate().getTime() - new Date().getTime();
				long daysDifference = TimeUnit.MILLISECONDS.toDays(timeDifference) % 365;
				if (daysDifference <= contractDetails.getRenewalReminderNotification()) {
					sendReminderEmail(contractDetails.getContractName(), contractDetails.getCreatedBy(), daysDifference,
							contractDetails.getContractEndDate(), String.valueOf(contractDetails.getId()),
							contractDetails.getEnvelopeId());
				}
			}
		}
		return new CommonResponse(HttpStatus.OK, new Response("ContractRenewalReminderResponse", new ArrayList<>()),
				"Reminder mail had been sent successfully");
	}

	private void sendReminderEmail(String contractName, String emailAddress, long daysDifference, Date renewalDate,
			String contractId, String envelopeId) throws IOException, TemplateException, MessagingException {
		String toAddress = emailAddress;
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		String url = "{{host}}?search={{ContactId}}";
		url = url.replace("{{host}}", redirectUrl);
		url = url.replace("{{ContactId}}", contractId);
		String subject = Constant.CONTRACT_EMAIL_SUBJECT;
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-renewal-reminder.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{days}}", String.valueOf(daysDifference));
		content = content.replace("{{contractName}}", contractName);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		content = content.replace("{{renewalDate}}", String.valueOf(formatter.format(renewalDate)));
		content = content.replace("{{hostname}}", redirectUrl);
		content = content.replace("{{envelopeId}}", envelopeId);
		content = content.replace("{{supportEmail}}", supportEmail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		try {
			helper.setFrom(mailDomainName, domainName);
			helper.setTo(toAddress);
			helper.setSubject(subject);
			helper.setText(content, true);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException e) {
			throw new MessagingException(e.getMessage());
		}
		mailSender.send(message);
	}

	@Override
	public CommonResponse updateTemplate(String json, String templateId, MultipartFile[] createDocumentFiles,
			MultipartFile[] updateDocumentFiles, String[] updateId, String[] deleteId, HttpServletRequest request)
			throws JsonProcessingException, DataValidationException {
		CommonResponse commonresponce = new CommonResponse();
		Response respose = new Response();
		int minRange = 1000;
		int maxRange = 9999;
		Map<String, Object> responseData = new HashMap<>();
		UpdateTemplate jsonrequest = mapper.readValue(json, UpdateTemplate.class);

		String token = request.getHeader(Constant.HEADER_STRING);
		String xAuthProvider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		String email = null;
		String name = null;
		if (xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			name = jwt.getClaim("name").asString();
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
		}

		List<String> updateIdsList = updateId != null ? Arrays.asList(updateId) : new ArrayList<>();
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();

		if (updateId != null) {
			if (!updateId.equals(deleteId)) {
				responseData.put("update_id", updateId);
			} else {
				throw new DataValidationException("update_id should not be same as delete_id", "400",
						HttpStatus.BAD_REQUEST);
			}
		}

		if (deleteId != null) {
			responseData.put("delete_id", deleteId);
		}
		List<String> createdIds = new ArrayList<>();
		List<DocumentRequest> documentRequests = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				DocumentRequest documentRequest = new DocumentRequest();
				try {
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					if (documentRequests.stream()
							.filter(p -> p.getDocumentId().equalsIgnoreCase(String.valueOf(randomNumber)))
							.collect(Collectors.toList()).isEmpty()) {
						documentRequest
								.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
						documentRequest.setDocumentId(String.valueOf(randomNumber));
						documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
								createDocumentFile.getOriginalFilename().indexOf(".")));
						documentRequest.setCategory("create");
						documentRequests.add(documentRequest);
						createdIds.add(String.valueOf(randomNumber));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			responseData.put(Constant.CREATE_ID, createdIds);
		}
		if (updateDocumentFiles != null) {
			int j = 0;
			for (MultipartFile updateDocumentFile : updateDocumentFiles) {
				DocumentRequest documentRequest = new DocumentRequest();
				try {
					documentRequest
							.setDocumentBase64(Base64.getEncoder().encodeToString(updateDocumentFile.getBytes()));
					documentRequest.setDocumentId(updateIdsList.get(j));
					documentRequest.setName(updateDocumentFile.getOriginalFilename().substring(0,
							updateDocumentFile.getOriginalFilename().indexOf(".")));
					documentRequest.setCategory("update");
				} catch (IOException e) {
					e.printStackTrace();
				}
				documentRequests.add(documentRequest);
				j++;
			}
		}
		if (deleteIdsList != null) {
			for (String deleteid : deleteIdsList) {
				DocumentRequest documentRequest = new DocumentRequest();
				documentRequest.setDocumentId(deleteid);
				documentRequest.setCategory(Constant.DELETE);
				documentRequests.add(documentRequest);
			}

		}
		jsonrequest.setDocuments(documentRequests);
		List<String> errors = new ArrayList<>();
		String url = getDousignUrl().getUpdateTemplate().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.TEMPLATEID, templateId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Object.class);

		CreateTemplate oldTemplate = createTemplateRepository.findByTemplateId(templateId);
		oldTemplate.setTemplateName(jsonrequest.getTemplateName());
		oldTemplate.setUpdatedOn(new Date());
		oldTemplate.setOwnerEmail(email);
		oldTemplate.setUpdateByName(name);
		createTemplateRepository.save(oldTemplate);
		if (errors.isEmpty()) {
			respose.setAction(Constant.UPDATE_TEMPLATE_RESPONSE);
			commonresponce.setStatus(HttpStatus.OK);
			commonresponce.setResponse(respose);
			commonresponce.setMessage("Template Updated Sucessfully");
			respose.setData(responseData);
			responseData.put(Constant.TEMPLATE_NAME, jsonrequest.getTemplateName());
		} else {
			commonresponce.setMessage("Update Template Failed");
			commonresponce.setResponse(new Response(Constant.UPDATE_TEMPLATE_RESPONSE, errors));
			commonresponce.setStatus(HttpStatus.BAD_REQUEST);
			respose.setAction(Constant.UPDATE_TEMPLATE_RESPONSE);
			respose.setData(new ArrayList<>());
			responseData.put(Constant.TEMPLATE_NAME, jsonrequest.getTemplateName());
		}
		return commonresponce;
	}

	@Data
	public static class UpdateTemplate {
		private String templateName;
		private String templateDescritpion;
		private String emailSubject;
		private String emailMessage;
		private Boolean signerCanSignONMobile;
		private List<SignerRequest> signers;
		private List<DocumentRequest> documents;
		private RemainderRequest reminders;
		private ExpirationRequest expirations;
		private Boolean allowComments;
		private Boolean enforceSignerVisibility;
		private Boolean recipientLock;
		private Boolean messageLock;
		private Boolean signingOrder;
		private String status;
	}

	@Data
	public static class SignerRequest {
		private String name;
		private String email;
		private String recipientType;
		private String recipientRole;
		private Boolean canSignOffline;
		private String routingOrder;

	}

	@Data
	public static class DocumentRequest {
		private String name;
		private String documentId;
		private String documentBase64;
		private String category;
	}

	@Data
	public static class RemainderRequest {
		private Boolean reminderEnabled;
		private String reminderDelay;
		private String reminderFrequency;
	}

	@Data
	public static class ExpirationRequest {
		private Boolean expiryEnabled;
		private String expiryAfter;
		private String expiryWarn;
	}

	@Override
	public CommonResponse createTemplate(String json, MultipartFile[] createDocumentFiles, HttpServletRequest request,
			String flowType) throws JsonProcessingException, DataValidationException {

		String token = request.getHeader(Constant.HEADER_STRING);
		String xAuthProvider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		String email = null;
		String name = null;
		if (xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			name = jwt.getClaim("name").asString();
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
		}
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		CommonResponse commonresponce = new CommonResponse();
		Response respose = new Response();
		int minRange = 1000;
		int maxRange = 9999;
		Map<String, Object> responseData = new HashMap<>();
		UpdateTemplate jsonrequest = mapper.readValue(json, UpdateTemplate.class);

		List<DocumentRequest> documentRequests = new ArrayList<>();
		List<String> createdIds = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				DocumentRequest documentRequest = new DocumentRequest();
				try {
					int randomNumber = random.nextInt(maxRange - minRange + 1) + minRange;
					if (documentRequests.stream()
							.filter(p -> p.getDocumentId().equalsIgnoreCase(String.valueOf(randomNumber)))
							.collect(Collectors.toList()).isEmpty()) {
						documentRequest
								.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));

						documentRequest.setDocumentId(String.valueOf(randomNumber));
						documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
								createDocumentFile.getOriginalFilename().indexOf(".")));
						documentRequest.setCategory("create");
						documentRequests.add(documentRequest);
						createdIds.add(String.valueOf(randomNumber));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			responseData.put(Constant.CREATE_ID, createdIds);
		}
		jsonrequest.setDocuments(documentRequests);
		String folderId = null;

		if (flowType.equalsIgnoreCase(Constant.PCD)) {
			folderId = pcdFolderId;
		} else if ((flowType.equalsIgnoreCase(Constant.COMMERCIAL))) {
			folderId = commercialFolderId;
		}
		List<String> errors = new ArrayList<>();
		String url = getDousignUrl().getCreateTemplate().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam(Constant.EMAIL, email);
		builder.queryParam("folderId", folderId);
		builder.queryParam(Constant.USER_ID_ERROR_KEY, userId.getUserId());
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		ResponseEntity<Object> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.CLM_CONTRACT_RESPONSE, errorResponse), Constant.CONTRACT_CREATION_FAILED);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.CLM_CONTRACT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.CLM_CONTRACT_RESPONSE, null),
					Constant.CONTRACT_CREATION_FAILED);
		}
		if (responseEntity.getBody() != null) {
			CreateTemplate templateCreate = new CreateTemplate();
			templateCreate.setId(sequenceGeneratorService.generateSequence(CreateTemplate.SEQUENCE_NAME));
			templateCreate.setTemplateId(extractTemplateId(responseEntity.getBody().toString()));
			templateCreate.setTemplateName(jsonrequest.getTemplateName());
			// add the new fields
			templateCreate.setOwnerEmail(email);
			templateCreate.setOwnerName(name);
			templateCreate.setCreatedOn(new Date());
			templateCreate.setUpdatedOn(new Date());
			FlowType flowTypeEnum;
			flowTypeEnum = FlowType.valueOf(flowType.toUpperCase());
			if (flowTypeEnum == FlowType.PCD || flowTypeEnum == FlowType.COMMERCIAL) {
				templateCreate.setFlowType(flowType);
			} else {
				throw new DataValidationException("Invalid flowType!! FlowType should be PCD/Commercial", null, null);
			}
			createTemplateRepository.save(templateCreate);
		}

		if (errors.isEmpty()) {
			respose.setAction(Constant.CREATE_TEMPLATE_RESPONSE);
			commonresponce.setStatus(HttpStatus.OK);
			commonresponce.setResponse(respose);
			commonresponce.setMessage("Template Created Sucessfully");
			respose.setData(responseData);
			responseData.put(Constant.TEMPLATE_NAME, jsonrequest.getTemplateName());
		} else {
			commonresponce.setMessage("Create Template Failed");
			commonresponce.setResponse(new Response(Constant.CREATE_TEMPLATE_RESPONSE, errors));
			commonresponce.setStatus(HttpStatus.BAD_REQUEST);
			respose.setAction(Constant.CREATE_TEMPLATE_RESPONSE);
			respose.setData(new ArrayList<>());
		}
		return commonresponce;
	}

	public static CreateTemplateModel fromString(String input) {
		CreateTemplateModel yourObject = new CreateTemplateModel();
		Pattern pattern = Pattern.compile("(\\w+)=(\\w+|\"[^\"]+\")|templateId=(.*?)(, |$)");
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = matcher.group(2);

			if ("name".equals(key)) {
				yourObject.setName("null".equals(value) ? null : value);
			}

			if (Constant.TEMPLATE_ID.equals(key)) {
				yourObject.setTemplateId("null".equals(value) ? null : value);

				if (value == null) {
					value = matcher.group(3); // Capture the full templateId value
					yourObject.setTemplateId("null".equals(value) ? null : value);
				}
			}
		}
		return yourObject;
	}

	public static String extractTemplateId(String input) {
		Pattern pattern = Pattern.compile("templateId=([^,]+)");
		Matcher matcher = pattern.matcher(input);

		if (matcher.find()) {
			return matcher.group(1);
		}

		return null; // Return null if templateId is not found
	}

	@Override
	public CommonResponse createEnvelope(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId) throws JsonProcessingException {
		CommonResponse commonresponce = new CommonResponse();
		Response respose = new Response();
		Map<String, Object> responseData = new HashMap<>();
		EsignaturePojo jsonrequest = mapper.readValue(json, EsignaturePojo.class);
		int minRange = 1000; // Minimum 4-digit number (inclusive)
		int maxRange = 9999; // Maximum 4-digit number (inclusive)

		List<String> createIdsList = createId != null ? Arrays.asList(createId) : new ArrayList<>();
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();

		if (createId != null) {
			responseData.put(Constant.CREATE_ID, createId);
		}
		if (deleteId != null) {
			responseData.put("delete_id", deleteId);
		}

		ArrayList<Document> documentRequests = new ArrayList<>();
		if (createDocumentFiles != null) {
			for (MultipartFile createDocumentFile : createDocumentFiles) {
				Document documentRequest = new Document();
				try {
					documentRequest
							.setDocumentBase64(Base64.getEncoder().encodeToString(createDocumentFile.getBytes()));
					documentRequest.setName(createDocumentFile.getOriginalFilename().substring(0,
							createDocumentFile.getOriginalFilename().indexOf(".")));
					documentRequest.setCategory(Constant.CREATE_FILE);
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
				Document documentRequest = new Document();
				documentRequest.setDocumentId(createIdsList.get(i));
				documentRequest.setCategory("createId");
				documentRequests.add(documentRequest);
				i++;
			}
		}
		jsonrequest.setDocuments(documentRequests);
		if (deleteIdsList != null) {
			for (String deleteid : deleteIdsList) {
				Document documentRequest = new Document();
				documentRequest.setDocumentId(deleteid);
				documentRequest.setCategory(Constant.DELETE);
				documentRequests.add(documentRequest);
			}
		}
		jsonrequest.setDocuments(documentRequests);
		String baseUrl = getDousignUrl().getCreateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST,
				docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
		builder.queryParam(Constant.TEMPLATE_ID, templateId);
		String url = builder.toUriString();
		List<String> errors = new ArrayList<>();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		try {
			restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.CLM_CONTRACT_RESPONSE, errorResponse), Constant.CONTRACT_CREATION_FAILED);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("CreateEnvelopeResponse", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("CreateEnvelopeResponse", null),
					"Create Envelope Failed");
		}
		if (errors.isEmpty()) {
			respose.setAction("CreateEnvelopeResponse");
			commonresponce.setStatus(HttpStatus.OK);
			commonresponce.setResponse(respose);
			commonresponce.setMessage("Create Envelope Completed Sucessfully");
			respose.setData(responseData);
		} else {
			commonresponce.setMessage("Create Envelope Failed");
			commonresponce.setResponse(new Response("CreateEnvelopeResponse", errors));
			commonresponce.setStatus(HttpStatus.BAD_REQUEST);
			respose.setData(new ArrayList<>());
		}
		return commonresponce;
	}

	@Override
	public CommonResponse dashboardView(HttpServletRequest request) throws JsonProcessingException {
		String token = request.getHeader(Constant.HEADER_STRING);
		String xAuthProvider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		String email = null;
		if (xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
		}
		DashboardViewResponce dashboardview = new DashboardViewResponce();
		List<ClmContractDocument> totalContracts = clmContractDocumentRepository.findAllBySenderEmail(email);
		int signCompletedCount = 0;
		int signInProgressCount = 0;
		for (ClmContractDocument clms : totalContracts) {
			EnvelopeDocument envelopedocuemnt = envelopeRepository.findByenvelopeId(clms.getEnvelopeId());
			String envelopeJson = mapper.writeValueAsString(envelopedocuemnt.getEnvelope());
			JsonNode rootNode = mapper.readTree(envelopeJson);
			if (rootNode.path(Constant.STATUS).asText().equalsIgnoreCase("Completed")) {
				signCompletedCount = signCompletedCount + 1;
				dashboardview.setSignCompleted(signCompletedCount);
			}
			if (rootNode.path(Constant.STATUS).asText().equalsIgnoreCase("sent")) {
				signInProgressCount = signInProgressCount + 1;
				dashboardview.setSignInProgress(signInProgressCount);
			}

		}
		dashboardview.setTotalContracts(totalContracts.size());
		List<ClmContractDocument> latest10envelopes = clmContractDocumentRepository
				.findTop10BySenderEmailOrderByCreatedOnDesc(email, Sort.by(Sort.Direction.DESC, "createdOn"));
		if (latest10envelopes.size() > 10)
			latest10envelopes = latest10envelopes.subList(0, 10);
		List<LatestContractResponce> latestContractsList = new ArrayList<>();
		for (ClmContractDocument contract : latest10envelopes) {
			System.out.println(contract.getEnvelopeId());
			EnvelopeDocument envelopedocuemnt = envelopeRepository.findByenvelopeId(contract.getEnvelopeId());
			if (envelopedocuemnt != null) {
				String envelopeJson = mapper.writeValueAsString(envelopedocuemnt.getEnvelope());
				JsonNode rootNode = mapper.readTree(envelopeJson);
				LatestContractResponce latestcontract = new LatestContractResponce();
				latestcontract.setContractId(contract.getId());
				latestcontract.setTemplateId(contract.getTemplateId());
				latestcontract.setContractName(contract.getContractName());
				latestcontract.setContractStartDate(contract.getContractStartDate());
				latestcontract.setContractEndDate(contract.getContractEndDate());
				latestcontract.setRenewalReminderNotification(contract.getRenewalReminderNotification());
				latestcontract.setTemplateId(contract.getTemplateId());
				latestcontract.setEnvelopeId(contract.getEnvelopeId());
				latestcontract.setContractPeriod(contract.getContractPeriod());
				latestcontract.setSenderName(rootNode.path(Constant.SENDER).path(Constant.USER_NAME).asText());
				latestcontract.setSenderMail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
				latestcontract.setStatus(rootNode.path(Constant.STATUS).asText());
				latestcontract.setStartDate(rootNode.path(Constant.CREATED_DATE_TIME).asText());

				if (!rootNode.get(Constant.COMPLETED_DATE_TIME).isNull()) {
					latestcontract.setCompeletedDate(rootNode.get(Constant.COMPLETED_DATE_TIME).asText());
				} else {
					latestcontract.setCompeletedDate(null);
				}
				latestContractsList.add(latestcontract);
			}
		}
		Date today = new Date();
		List<ClmContractDocument> latest10expiredcontracts = clmContractDocumentRepository
				.findTop10ByContractEndDateLessThanAndSenderEmailOrderByContractEndDateDesc(today, email);
		List<ExpiringContractResponce> expiringContractsList = new ArrayList<>();
		int count = 0;
		for (ClmContractDocument contract : latest10expiredcontracts) {
			if (count >= 10) {
				break;
			}
			EnvelopeDocument envelopedocuemnt = envelopeRepository.findByenvelopeId(contract.getEnvelopeId());
			if (envelopedocuemnt != null) {
				String envelopeJson = mapper.writeValueAsString(envelopedocuemnt.getEnvelope());
				JsonNode rootNode = mapper.readTree(envelopeJson);
				ExpiringContractResponce expiringcontract = new ExpiringContractResponce();
				expiringcontract.setContractId(contract.getId());
				expiringcontract.setTemplateId(contract.getTemplateId());
				expiringcontract.setContractName(contract.getContractName());
				expiringcontract.setContractStartDate(contract.getContractStartDate());
				expiringcontract.setContractEndDate(contract.getContractEndDate());
				expiringcontract.setRenewalReminderNotification(contract.getRenewalReminderNotification());
				expiringcontract.setTemplateId(contract.getTemplateId());
				expiringcontract.setEnvelopeId(contract.getEnvelopeId());
				expiringcontract.setContractPeriod(contract.getContractPeriod());
				expiringcontract.setSenderName(rootNode.path(Constant.SENDER).path(Constant.USER_NAME).asText());
				expiringcontract.setSenderMail(rootNode.path(Constant.SENDER).path(Constant.EMAIL).asText());
				expiringcontract.setStatus(rootNode.path(Constant.STATUS).asText());
				expiringcontract.setStartDate(rootNode.path(Constant.CREATED_DATE_TIME).asText());

				if (!rootNode.get(Constant.COMPLETED_DATE_TIME).isNull()) {
					expiringcontract.setCompeletedDate(rootNode.get(Constant.COMPLETED_DATE_TIME).asText());
				} else {
					expiringcontract.setCompeletedDate(null);
				}
				expiringContractsList.add(expiringcontract);
			}
			count++;
		}
		List<ExpiringContractResponce> sortedContracts = expiringContractsList.stream()
				.sorted(Comparator.comparing(ExpiringContractResponce::getContractEndDate))
				.collect(Collectors.toList());
		dashboardview.setExpiredContracts(sortedContracts);
		dashboardview.setLatestContracts(latestContractsList);
		Response responseData = new Response("Dashboardview Response", dashboardview);
		return new CommonResponse(HttpStatus.OK, responseData, "Dashboard details fetched successfully");
	}

	@Data
	public static class EsignaturePojo {
		private List<Document> documents;
		private String emailSubject;
		private String emailMessage;
		private String allowComments;
		private Boolean enforceSignerVisibility;
		private String recipientLock;
		private String messageLock;
		private Reminders reminders;
		private Expiration expirations;
		private Recipients recipients;
		private String status;
		private String useAccountDefaults;
		private Boolean signerCanSignOnMobile;
	}

	@Data
	public static class Reminders {
		private String reminderDelay;
		private String reminderFrequency;
		private Boolean reminderEnabled;
	}

	@Data
	public static class Expiration {
		private String expireAfter;
		private String expireEnabled;
		private String expireWarn;
	}

	@Data
	public static class Recipients {
		private List<Signer> signers;
		private List<Carboncopy> cc;
	}

	@Data
	public static class Document {
		private String documentBase64;
		private String documentId;
		private String fileExtension;
		private String name;
		private String category;
	}

	@Data
	public static class Carboncopy {
		private String email;
		private String name;
		private String routingOrder;
	}

	@Data
	public static class Signer {
		private String email;
		private String name;
		private String routingOrder;
		private String recipientType;
		private Tabs tabs;
		private String roleName;
	}

	@Data
	public static class Tabs {
		private List<SignHereTabs> signHereTabs;
	}

	@Data
	public static class SignHereTabs {

		private String xPosition;
		private String yPosition;
		private String documentId;
		private String pageNumber;
	}

	@Override
	public CommonResponse envelopeAudit(String envelopeid) {
		String url = getDousignUrl().getAuditLogs().replace(Constant.DOCUSIGN_HOST, docusignHost.trim()) + envelopeid;
		ResponseEntity<Object> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
		} catch (HttpServerErrorException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("DocusignAuditLogs", new ArrayList<>()),
					"Audit details failed");
		}
		return new CommonResponse(HttpStatus.OK, new Response("AuditdetailsResponce", responseEntity.getBody()),
				"Audit details fetched successfully");
	}

	@Override
	public CommonResponse getEnvelopeDocument(String envelopeId, String documentId) throws DataValidationException {
		String url = getDousignUrl().getEnvelopeDocuments().replace(Constant.DOCUSIGN_HOST, docusignHost);
		url = url.replace(Constant.ENVELOPEID, envelopeId);
		url = url.replace("{documentId}", documentId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.ENVELOPE_DOCUMENT_RESPONSE, response.getBody()),
						"EnvelopeDocument Fetched Successfully");
			} else {
				return new CommonResponse(response.getStatusCode(),
						new Response(Constant.ENVELOPE_DOCUMENT_RESPONSE, null),
						Constant.HTTP_STATUS_CODE + response.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.ENVELOPE_DOCUMENT_RESPONSE, errorResponse), "EnvelopeDocument Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.ENVELOPE_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.ENVELOPE_DOCUMENT_RESPONSE, new ArrayList<>()),
					Constant.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public CommonResponse getlistEnvelopeRecipients(String envelopeId, String email) throws DataValidationException {
		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		String url = getDousignUrl().getListEnvelopeRecipients().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.ENVELOPEID, envelopeId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					Object.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.LIST_ENVELOPE_RESPONSE, response.getBody()),
						"ListEnvelope Recipients Fetched Successfully");
			} else {
				return new CommonResponse(response.getStatusCode(), new Response(Constant.LIST_ENVELOPE_RESPONSE, null),
						Constant.HTTP_STATUS_CODE + response.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LIST_ENVELOPE_RESPONSE, errorResponse),
					"ListEnvelope Recipients Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LIST_ENVELOPE_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.LIST_ENVELOPE_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public CommonResponse getEnvelopeDocumentDetails(String envelopeId, String email) throws DataValidationException {
		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		String url = getDousignUrl().getEnvelopeDocumentDetails().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.ENVELOPEID, envelopeId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					Object.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.ENVELOPE_DOCUMENT_DETAILS_RESPONSE, response.getBody()),
						"EnvelopeDocumentdetails Fetched Successfully");
			} else {
				return new CommonResponse(response.getStatusCode(),
						new Response(Constant.ENVELOPE_DOCUMENT_DETAILS_RESPONSE, null),
						Constant.HTTP_STATUS_CODE + response.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.ENVELOPE_DOCUMENT_DETAILS_RESPONSE, errorResponse),
					"EnvelopeDocumentdetails Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.ENVELOPE_DOCUMENT_DETAILS_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.ENVELOPE_DOCUMENT_DETAILS_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public CommonResponse listTemplateById(String templateId) {
		String url = getDousignUrl().getListTemplateById().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.TEMPLATEID, templateId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<Object> responceEntity = null;
		try {
			responceEntity = restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
			if (responceEntity.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.TEMPLATE_LIST_RESPONSE, responceEntity.getBody()),
						"TemplateList Fetched Successfully");
			} else {
				return new CommonResponse(responceEntity.getStatusCode(),
						new Response(Constant.TEMPLATE_LIST_RESPONSE, null),
						Constant.HTTP_STATUS_CODE + responceEntity.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponce Response", errorResponse), "TemplateListResponce Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.TEMPLATE_LIST_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.TEMPLATE_LIST_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public CommonResponse listTemplates(String count, String start, String order, String orderBy, String searchText,
			String flowType) {
		String url = getDousignUrl().getTemplates().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		String folderId = null;
		if (flowType.equalsIgnoreCase("PCD")) {
			folderId = pcdFolderId;
		} else if ((flowType.equalsIgnoreCase("commercial"))) {
			folderId = commercialFolderId;
		}
		builder.queryParam("count", count);
		builder.queryParam("start", start);
		builder.queryParam("order", order);
		builder.queryParam("orderBy", orderBy);
		builder.queryParam("searchText", searchText);
		builder.queryParam("folderId", folderId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<Object> responceEntity = null;
		try {
			responceEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null, Object.class);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("TemplateResponse", ""), e.getMessage());
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND, new Response("TemplateResponse", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.TEMPLATE_LIST_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
		}
		return new CommonResponse(HttpStatus.OK, new Response("", responceEntity.getBody()),
				"Template details fetched successfully");
	}

	@Override
	public CommonResponse getAllTemplates(String flowType) {
		List<CreateTemplate> templates = null;
		if (flowType != null) {
			templates = createTemplateRepository.findByCustomFlowTypeQuery(flowType);
		} else {
			templates = createTemplateRepository.findAll();
		}
		return new CommonResponse(HttpStatus.OK, new Response("TemplateDetailsResponse", templates),
				"Template details fetched successfully");
	}

	@Override
	public CommonResponse getTemplateDocument(String templateId, String documentId) {
		String url = getDousignUrl().getTemplateDocuments().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.TEMPLATEID, templateId);
		url = url.replace("{documentId}", documentId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					byte[].class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.TEMPLATE_DOCUMENT_RESPONSE, response.getBody()),
						"TemplateDocument Fetched Successfully");
			} else {
				return new CommonResponse(response.getStatusCode(),
						new Response(Constant.TEMPLATE_DOCUMENT_RESPONSE, null),
						Constant.HTTP_STATUS_CODE + response.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.TEMPLATE_DOCUMENT_RESPONSE, errorResponse), "TemplateDocument Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.TEMPLATE_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.TEMPLATE_DOCUMENT_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public CommonResponse getEnvelopeComments(String envelopeId, String email) throws DataValidationException {
		DocusignUserCache docuObject = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);

		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);

		String url = getDousignUrl().getTemplateComments().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.ENVELOPEID, envelopeId);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam(Constant.EMAIL, email);
		builder.queryParam(Constant.USER_ID_ERROR_KEY, docuObject.getUserId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			ResponseEntity<byte[]> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
					new HttpEntity<>(headers), byte[].class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.ENVELOPE_COMMENTS_RESPONSE, response.getBody()),
						"EnvelopeComments Fetched Successfully");
			} else {
				return new CommonResponse(response.getStatusCode(),
						new Response(Constant.ENVELOPE_COMMENTS_RESPONSE, null),
						Constant.HTTP_STATUS_CODE + response.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.ENVELOPE_COMMENTS_RESPONSE, errorResponse), "TemplateDocument Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.ENVELOPE_COMMENTS_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.ENVELOPE_COMMENTS_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public CommonResponse updateContractDocument(MultipartFile[] createDocumentFiles,
			MultipartFile[] updateDocumentFiles, String[] updateId, String[] deleteId, String envId, String email)
			throws DataValidationException, JsonProcessingException {
		ClmContractRequest jsonrequest = new ClmContractRequest();
		int minRange = 1000;
		int maxRange = 9999;
		List<String> deleteIdsList = deleteId != null ? Arrays.asList(deleteId) : new ArrayList<>();
		List<String> updatedIdsList = updateId != null ? Arrays.asList(updateId) : new ArrayList<>();
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
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!deleteIdsList.isEmpty()) {
			for (String deleteid : deleteIdsList) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				documentRequest.setDocumentId(deleteid);
				documentRequest.setCategory(Constant.DELETE);
				documentRequests.add(documentRequest);
			}
		}
		if (!updatedIdsList.isEmpty() && (updateDocumentFiles != null)) {
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
					documentRequests.add(documentRequest);
				} catch (IOException e) {
					e.printStackTrace();
				}
				i++;
			}

		}
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(email);
		ClmContractDocument oldEnvelope = clmContractDocumentRepository.findByEnvelopeId(envId);
		ClmContractDocument clmContractDocument = new ClmContractDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		clmContractDocument.setId(sequenceGeneratorService.generateSequence(ClmContractDocument.SEQUENCE_NAME));
		clmContractDocument.setContractName(oldEnvelope.getContractName());
		clmContractDocument.setContractStartDate(oldEnvelope.getContractStartDate());
		clmContractDocument.setContractEndDate(oldEnvelope.getContractEndDate());
		clmContractDocument.setBuID("BUID");
		clmContractDocument.setOpID("SAASPE");
		clmContractDocument.setCreatedOn(new Date());
		clmContractDocument.setCreatedBy(email);
		clmContractDocument.setRenewalReminderNotification(oldEnvelope.getRenewalReminderNotification());
		clmContractDocument.setContractPeriod(oldEnvelope.getContractPeriod());
		clmContractDocument.setStatus("sent");
		clmContractDocument.setReferenceId(envId);
		clmContractDocument.setReferenceType("Envelope");
		clmContractDocument.setReviewerSigningOrder(oldEnvelope.getReviewerSigningOrder());
		clmContractDocument.setSignerSigningOrder(oldEnvelope.getSignerSigningOrder());
		DocusignUserCache redis = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		clmContractDocument.setTemplateId(oldEnvelope.getTemplateId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
				getDousignUrl().getUpdateEnvelopeMultiple().replace(Constant.DOCUSIGN_HOST, docusignHost.trim()));
		builder.queryParam(Constant.ENVELOPE_ID, envId);
		builder.queryParam("userId", redis.getUserId());
		String url = builder.toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonrequest, headers);
		ResponseEntity<Object> response = null;
		try {
			response = restTemplate.postForEntity(url, requestEntity, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("CLM Contract Update Response", errorResponse), Constant.CONTRACT_CREATION_FAILED);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.ENVELOPE_COMMENTS_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("CLM Contract Update Response", ex.getMessage()), Constant.CONTRACT_CREATION_FAILED);
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String envelopeId = rootNode.get(Constant.ENVELOPE_ID).asText();
		// new envelope id saving in old

		oldEnvelope.setNewEnvelopeId(envelopeId);
		oldEnvelope.setUpdatedOn(new Date());
		clmContractDocumentRepository.save(oldEnvelope);
		if (envelopeId != null) {
			String envelopeDataUrl = getDousignUrl().getGetEnvelopeById().replace(Constant.DOCUSIGN_HOST,
					docusignHost.trim()) + envelopeId;
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(envelopeDataUrl, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				String responseBody = ex.getResponseBodyAsString();
				Object errorResponse = mapper.readValue(responseBody, Object.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.CLM_CONTRACT_RESPONSE, errorResponse), "Contract Update Failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response(Constant.CLM_CONTRACT_RESPONSE, new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.CLM_CONTRACT_RESPONSE, ex.getMessage()), "Contract Update Failed");
			}
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(envelopeId);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
			List<DocumentResponse> documentResponses = new ArrayList<>();
			for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
				DocumentResponse docResponse = new DocumentResponse();
				docResponse.setDocumentId(documentResponse.getDocumentId());
				docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
				docResponse.setName(documentResponse.getName());
				docResponse.setDocumentBase64(documentResponse.getDocumentBase64());
				documentResponses.add(documentResponse);
			}
			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode2 = mapper.readTree(json2);
			envelopeDocument.setDocuments(documentResponses);
			envelopeDocument.setStartDate(new Date());
			envelopeRepository.save(envelopeDocument);
			clmContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
			clmContractDocument.setEnvelopeId(envelopeId);
			clmContractDocument.setUniqueString(oldEnvelope.getUniqueString());
			clmContractDocument.setOrder(oldEnvelope.getOrder() + 1);
			clmContractDocument.setVersion(generateVersion(oldEnvelope.getOrder() + 1));
			clmContractDocumentRepository.save(clmContractDocument);
		} else {
			throw new DataValidationException("update contract failed, try again!", null, null);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response("CLM Contract Update Response", envelopeId),
				"Contract details updated successfully");
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

	@Override
	public CommonResponse getDocumentVersions(String envelopeId, String email) throws DataValidationException {

		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		List<DocumentVersioningResponse> response = new ArrayList<>();
		ClmContractDocument currentEnvelope = clmContractDocumentRepository.findByEnvelopeId(envelopeId);
		if (currentEnvelope == null) {
			throw new DataValidationException("Envelope Does Not Exist", null, null);
		}
		List<ClmContractDocument> documentList = clmContractDocumentRepository
				.findByUniqueString(currentEnvelope.getUniqueString());

		for (ClmContractDocument contractDocument : documentList) {
			DocumentVersioningResponse documentVersioning = new DocumentVersioningResponse();
			documentVersioning.setEnvelopeId(contractDocument.getEnvelopeId());
			documentVersioning.setEnvelopeName(contractDocument.getContractName());
			documentVersioning.setOrder(contractDocument.getOrder());
			if (contractDocument.getVersion() != null) {
				documentVersioning.setDocumentVersion(contractDocument.getVersion());
			}
			if (contractDocument.getCreatedOn() != null) {
				documentVersioning.setCreatedTime(contractDocument.getCreatedOn());
			}
			if (contractDocument.getUpdatedOn() != null) {
				documentVersioning.setUpdatedTime(contractDocument.getUpdatedOn());
			}
			response.add(documentVersioning);
		}
		Collections.sort(response, Comparator.comparingInt(DocumentVersioningResponse::getOrder).reversed());
		return new CommonResponse(HttpStatus.OK, new Response("CLM Document Versioning Response", response),
				"Documents data fetched successfully");
	}

	@Override
	public CommonResponse getEnvelopeDocumentAsPdf(SendDocumentPdfMail mailRequest)
			throws DataValidationException, MessagingException, UnsupportedEncodingException {
		Long contractId = mailRequest.getContractId();
		List<String> toMailAddress = mailRequest.getToMailAddress();
		List<String> ccMailAddress = mailRequest.getCcMailAddress();
		Optional<ClmContractDocument> optionalContract = clmContractDocumentRepository.findById(contractId);
		if (optionalContract.isPresent()) {
			ClmContractDocument contract = optionalContract.get();
			String envelopeId = contract.getEnvelopeId();
			EnvelopeDocument envelopeDocumentDetailsResponse = envelopeRepository.findByenvelopeId(envelopeId);
			if (envelopeDocumentDetailsResponse != null) {
				List<DocumentResponse> documents = envelopeDocumentDetailsResponse.getDocuments();
				Map<String, byte[]> attachmentsMap = new HashMap<>();
				for (DocumentResponse document : documents) {
					String documentName = document.getName();
					String documentBase64 = document.getDocumentBase64();
					byte[] decodedDocument = Base64.getDecoder().decode(documentBase64);
					attachmentsMap.put(documentName, decodedDocument);
				}
				sendEmailWithAttachment(toMailAddress, ccMailAddress, attachmentsMap);
			} else {
				throw new DataValidationException("Envelope Details Does Not Exist", null, null);
			}
		} else {
			throw new DataValidationException("ContractDocument Does Not Exist", null, null);
		}

		return new CommonResponse(HttpStatus.OK, null, "Email sent successfully");
	}

	private void sendEmailWithAttachment(List<String> toMailAddresses, List<String> ccMailAddresses,
			Map<String, byte[]> attachmentsMap) throws MessagingException, UnsupportedEncodingException {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(mailDomainName, "MAILSENDER");
			String[] toAddressesArray = toMailAddresses.toArray(new String[0]);
			helper.setTo(toAddressesArray);
			String[] ccAddressesArray = ccMailAddresses.toArray(new String[0]);
			helper.setCc(ccAddressesArray);
			helper.setSubject("THIS is a test SUBJECT mail");
			Multipart multipart = new MimeMultipart();
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setText("Please find the documents attached in this mail");
			multipart.addBodyPart(textPart);
			for (Map.Entry<String, byte[]> entry : attachmentsMap.entrySet()) {
				String documentName = entry.getKey();
				byte[] attachmentBytes = entry.getValue();
				MimeBodyPart attachmentPart = new MimeBodyPart();
				DataSource source = new ByteArrayDataSource(attachmentBytes, "application/pdf");
				attachmentPart.setDataHandler(new DataHandler(source));
				attachmentPart.setFileName(documentName + ".pdf");
				multipart.addBodyPart(attachmentPart);
			}
			message.setContent(multipart);
			mailSender.send(message);
		} catch (MailException e) {
			e.printStackTrace();
		}
	}

	@Override
	public CommonResponse updateEnvelopeStatus(HttpServletRequest request, String envelopeId, String status)
			throws IllegalStateException, JsonProcessingException, DataValidationException {

		String token = request.getHeader(Constant.HEADER_STRING);
		String x_auth_provider = request.getHeader("X-Auth-Provider");
		String email = null;
		if (x_auth_provider.equalsIgnoreCase("azure")) {
			DecodedJWT jwt = JWT.decode(token.replace("Bearer ", ""));
			if (jwt.getClaim("email").asString() != null) {
				email = jwt.getClaim("email").asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
		}
		Map<String, String> envelopeIdmap = new HashMap<>();
		envelopeIdmap.put(Constant.ENVELOPE_ID, envelopeId);
		DocusignUserCache redis = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		String userId = redis.getUserId();

		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		String url = getDousignUrl().getUpdateEnvelopeStatus().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		HttpHeaders headers = new HttpHeaders();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam(Constant.ENVELOPE_ID, envelopeId);
		builder.queryParam(Constant.USER_ID_ERROR_KEY, userId);
		builder.queryParam(Constant.STATUS, status);
		HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
		try {
			restTemplate.exchange(builder.toUriString(), HttpMethod.POST, requestEntity, String.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeStatus Update Response", errorResponse), "EnvelopeStatus Update Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("EnvelopeStatus Update Response", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeStatus Update Response", ex.getMessage()), "EnvelopeStatus Update Failed");
		}
		String repositoryName = centralRepoDocumentRepository.findByEnvelopeId(envelopeId).getRepositoryName();
		log.info("");
		switch (repositoryName) {
		case Constant.LOA_DOCUMENT_REPOSITORY:
			LoaDocument loaDocument = loaDocumentRepository.findByEnvelopeId(envelopeId);
			if (loaDocument != null) {
				log.info("Updating Flag");
				loaDocument.setIsSignersCompleted(Constant.IN_PROGRESS);
				loaDocument.setStatus(Constant.SENT);
				log.info("Saving LOA Repository");
				loaDocumentRepository.save(loaDocument);
			}
			break;
		case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
			LoaContractDocument contractDocument = loaContractDocumentRepository.findByEnvelopeId(envelopeId);

			if (contractDocument != null) {
				log.info("Updating Flag");
				contractDocument.setIsCvefSignersCompleted(Constant.IN_PROGRESS);
				contractDocument.setStatus(Constant.SENT);
				log.info("Saving Contract Repository");
				loaContractDocumentRepository.save(contractDocument);
			}
			break;
		case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
			LooContractDocument looDocument = looContractDocumentRepository.findByEnvelopeId(envelopeId);
			if (looDocument != null) {
				log.info("Updating Flag");
				looDocument.setIsSignersCompleted(Constant.IN_PROGRESS);
				looDocument.setStatus(Constant.SENT);
				log.info("Saving LOO Repository");
				looContractDocumentRepository.save(looDocument);
			}
			break;
		case Constant.TA_DOCUMENT_REPOSITORY:
			TADocument taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
			if (taDocument != null) {
				log.info("Updating Flag");
				taDocument.setIsSignersCompleted(Constant.IN_PROGRESS);
				taDocument.setStatus(Constant.SENT);
				log.info("Saving TA Repository");
				taDocumentRepository.save(taDocument);
			}
			break;
		}
		return new CommonResponse(HttpStatus.OK, new Response("EnvelopeStatus Update Response", envelopeIdmap),
				"EnvelopeStatus Update successfully");
	}

	public CommonResponse getConsoleView(String envelopeId, String returnUrl, String action)
			throws DataValidationException {

		String url = getDousignUrl().getGetConsoleViewURL().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam("envelopeId", envelopeId);
		builder.queryParam("returnUrl", returnUrl);
		if (action != null)
			builder.queryParam("action", action);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.POST,
					new HttpEntity<>(headers), String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK, new Response("Console View Response", response.getBody()),
						"EnvelopeView URL Fetched Successfully");
			} else {
				return new CommonResponse(response.getStatusCode(), new Response("Console View Response", null),
						Constant.HTTP_STATUS_CODE + response.getStatusCodeValue());
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Console View Response", errorResponse),
					"ConsoleView URL Fetch Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				throw new DataValidationException("Unable to access remote resources.Please check the connectivity",
						"404", HttpStatus.NOT_FOUND);
			}
			throw new DataValidationException("Provide valid envelopeId or returnURL", "400", HttpStatus.BAD_REQUEST);
		}

	}

	@Override
	@Transactional
	public CommonResponse createConsoleView(String email, String envelopeId, String returnUrl, String action)
			throws DataValidationException {

		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		EnvelopeLockDocument envelopeLockDocument = envelopeLockDocumentRepository
				.findByEnvelopeIdAndExpiryAtAfter(envelopeId, new Date());
		if (envelopeLockDocument == null) {
			System.out.println("Inside Null Condition");
			EnvelopeLockDocument newEnvelopeLockDocument = new EnvelopeLockDocument();
			Long id = sequenceGeneratorService.generateSequence(EnvelopeLockDocument.SEQUENCE_NAME);
			newEnvelopeLockDocument.setId(id);
			newEnvelopeLockDocument.setEnvelopeId(envelopeId);
			newEnvelopeLockDocument.setUserEmail(email);
			newEnvelopeLockDocument.setCreatedOn(new Date());
			newEnvelopeLockDocument.setUpdatedOn(new Date());
			newEnvelopeLockDocument.setExpiryAt(Date.from(Instant.now().plus(Duration.ofMinutes(25))));
			try {
				CommonResponse response = getConsoleView(envelopeId, returnUrl, action);
				envelopeLockDocumentRepository.save(newEnvelopeLockDocument);
				return response;
			} catch (Exception e) {
				throw new DataValidationException(e.getMessage(), "400", HttpStatus.BAD_REQUEST);
			}
		} else {
			System.out.println("Inside else Condition");
			if (envelopeLockDocument.getUserEmail().equalsIgnoreCase(email)) {
				System.out.println("Inside Same User Condition");
				envelopeLockDocument.setUpdatedOn(new Date());
				envelopeLockDocument.setExpiryAt(Date.from(Instant.now().plus(Duration.ofMinutes(25))));
				try {
					CommonResponse response = getConsoleView(envelopeId, returnUrl, action);
					envelopeLockDocumentRepository.save(envelopeLockDocument);
					return response;
				} catch (Exception e) {
					throw new DataValidationException(e.getMessage(), "400", HttpStatus.BAD_REQUEST);
				}
			} else {
				System.out.println("Inside Different User Condition");
				ConsoleViewErrorResponse error = ConsoleViewErrorResponse.builder()
						.userEmail(envelopeLockDocument.getUserEmail()).currentTime(new Date())
						.expiryTime(envelopeLockDocument.getExpiryAt()).build();
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Console View Response", error),
						"ConsoleView URL Fetch Failed");
			}
		}
	}

	@Override
	public CommonResponse getEnvelopeByEnvelopeId(String envelopeId, String email) throws DataValidationException {
		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		String url = getDousignUrl().getEnvelopeByEnvelopeId().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.ENVELOPEID, envelopeId);
		ResponseEntity<Object> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeDetailResponse", Constant.INTERNAL_SERVER_ERROR), "Envelope details failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("EnvelopeDetailResponse", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("EnvelopeDetailResponse", new ArrayList<>()),
					"Envelope details failed");
		}
		return new CommonResponse(HttpStatus.OK, new Response("EnvelopeDetailResponse", responseEntity.getBody()),
				"Envelope details fetched successfully");
	}

	@Override
	public CommonResponse getEnvelopeNotificationByEnvelopeId(String envelopeId) {
		String url = getDousignUrl().getGetEnvelopeNotification().replace(Constant.DOCUSIGN_HOST, docusignHost.trim());
		url = url.replace(Constant.ENVELOPEID, envelopeId);
		ResponseEntity<Object> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeNotificationDetailResponse", Constant.INTERNAL_SERVER_ERROR),
					"Envelope Notification details failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("EnvelopeNotificationDetailResponse", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeNotificationDetailResponse", new ArrayList<>()),
					"Envelope Notification details failed");
		}
		return new CommonResponse(HttpStatus.OK,
				new Response("EnvelopeNotificationDetailResponse", responseEntity.getBody()),
				"Envelope Notification details fetched successfully");
	}

	@Override
	public CommonResponse getUsage() {
		Map<String, String> outputMap = new HashMap<>();

		Runtime runtime = Runtime.getRuntime();
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

		outputMap.put("Free -", String.valueOf((long) (runtime.freeMemory() / (1024 * 1024))));
		outputMap.put("Total -", String.valueOf((long) (runtime.totalMemory() / (1024 * 1024))));
		outputMap.put("HeapUsed -", String.valueOf((long) (memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024))));
		outputMap.put("ExceptUsed -",
				String.valueOf((long) (memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024))));
		outputMap.put("Maximum Memory -", String.valueOf((long) (runtime.maxMemory() / (1024 * 1024))));
		return new CommonResponse(HttpStatus.OK, new Response("Memeory response", outputMap), "USAGE RESPONSE");
	}

	public CommonResponse getListOfTemplate(int page, int limit, String searchText, String order, String orderBy,
			String flowType) throws UnsupportedEncodingException {
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
		long totalCount = 0;
		List<CreateTemplate> list;
		if (order == null) {
			pageable = PageRequest.of(page, limit);
			list = createTemplateRepository.findByCustomFlowTypeQuery(flowType);
			totalCount = list.size();
		} else {
//			if (searchText != null && !searchText.isEmpty()) {
//				list = createTemplateRepository.findByTemplate(searchText, pageable, flowType);
//				totalCount = createTemplateRepository.findByTemplateNameCount(searchText, flowType);
//			} else {
//				list = createTemplateRepository.findByCustomFlowTypePageable(flowType, pageable);
//				totalCount = createTemplateRepository.findByCustomFlowTypePageableCount(flowType);
//			}
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			Query query = new Query();
			query.collation(collation);
			if (!flowType.equalsIgnoreCase("ALL")) {
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			Criteria criteria = new Criteria();
			if (searchText != null && !searchText.isEmpty()) {
				searchText = URLDecoder.decode(searchText, "UTF-8");
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("templateName").regex(pattern));
				query.addCriteria(criteria);
			}

			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, CreateTemplate.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query, CreateTemplate.class).stream().collect(Collectors.toList());
		}

		TemplateListPagination templateListPagination = new TemplateListPagination(totalCount, list);
		if (list.isEmpty()) {
			return new CommonResponse(HttpStatus.OK,
					new Response(Constant.TEMPLATE_LIST_RESPONSE, templateListPagination),
					Constant.NO_TEMPLATE_LIST_RESPONSE);
		}
		return new CommonResponse(HttpStatus.OK, new Response(Constant.TEMPLATE_LIST_RESPONSE, templateListPagination),
				"List of Templates fetched successfully");
	}

	@Override
	public CommonResponse addClmContract(String json, MultipartFile[] createDocumentFiles, String profile, String name)
			throws JsonMappingException, JsonProcessingException {
		LoaDocumentRequest jsonrequest = mapper.readValue(json, LoaDocumentRequest.class);
		int minRange = 1000;
		int maxRange = 9999;
		DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + profile);
//		UriComponentsBuilder builderTemplate = UriComponentsBuilder.fromHttpUrl(buildurl);
//		builderTemplate.path(templateId);
//		String urlTemplate = builderTemplate.toUriString();
		List<Document> versionDocDocuments = new ArrayList<>();

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
					versionDocDocuments.add(newDoc);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(profile);
		LoaDocument loaContractDocument = new LoaDocument();
		EnvelopeDocument envelopeDocument = new EnvelopeDocument();
		Long id = sequenceGeneratorService.generateSequence(LoaDocument.SEQUENCE_NAME);
		loaContractDocument.setId(id);
		loaContractDocument.setFlowCompleted(false);
		loaContractDocument.setProjectName(jsonrequest.getProjectName());
		loaContractDocument.setVendorName(jsonrequest.getVendorName());
		loaContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		loaContractDocument.setCompletionDate(jsonrequest.getCompletionDate());
		loaContractDocument.setBuID("BUID");
		loaContractDocument.setOpID("SAASPE");
		loaContractDocument.setCreatedOn(new Date());
		loaContractDocument.setCreatedBy(profile);
		loaContractDocument.setContractTenure(jsonrequest.getContractTenure());
//		if (jsonrequest.getStatus().equalsIgnoreCase("sent") && jsonrequest.getStatus().equalsIgnoreCase("completed")) {
//			throw new DataValidationException("Status should only be either 'sent' or 'completed'", null, null);
//		}
		loaContractDocument.setStatus(jsonrequest.getStatus());
		loaContractDocument.setVersion("1.0");
		loaContractDocument.setOrder(0);
		loaContractDocument.setUniqueString(generateRandomString(6));
		loaContractDocument.setReferenceType("Onedocument");
		loaContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		loaContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		Currency currency = new Currency(jsonrequest.getCurrency().getCurrencyCode(),
				jsonrequest.getCurrency().getTotalCost(), jsonrequest.getCurrency().getTax());
		loaContractDocument.setCurrency(currency);
		loaContractDocument.setVendors(jsonrequest.getVendors());
		loaContractDocument.setFilesUploaded(false);
		loaContractDocument.setTotalFiles(versionDocDocuments.size());
		loaContractDocument.setSubsidiary(jsonrequest.getSubsidiary());

		String buildurl = docusignHost + "/addTestLoaDocument";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildurl);
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
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = mapper.readValue(responseBody, Object.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, errorResponse), "LOA document Creation Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.LOA_DOCUMENT_RESPONSE, new ArrayList<>()), "LOA document Creation Failed");
		}
		String json1 = mapper.writeValueAsString(response.getBody());
		JsonNode rootNode = mapper.readTree(json1);
		String envelopeId = rootNode.get("envelopeId").asText();
		System.out.println(envelopeId);

		loaContractDocument.setEnvelopeId(envelopeId);
		loaDocumentRepository.save(loaContractDocument);

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
				String responseBody = ex.getResponseBodyAsString();
				Object errorResponse = mapper.readValue(responseBody, Object.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response(Constant.LOA_DOCUMENT_RESPONSE, errorResponse), "LOA Document Creation Failed");
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
			envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
			envelopeDocument.setEnvelopeId(envelopeId);
			Date createDate = new Date();
			WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
			creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
			creator.setEnvelopeId(envelopeId);
			creator.setCreatedOn(new Date());
			creator.setContractName(jsonrequest.getProjectName());
			creator.setFlowType("LOA_Create");
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
				reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
				reviewerDocumentRepository.save(reviewerDocument);
			}
			creator.setPendingWith(pendingWith);
			workFlowCreatorDocumentRespository.save(creator);
			envelopeDocument.setCreatedOn(new Date());
			envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());

			// List<DocumentResponse> documentResponses = new ArrayList<>();
			DocumentVersionDocument documentVersion = new DocumentVersionDocument();
			documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
			// List<Document> versionDocDocuments = new ArrayList<>();
			documentVersion.setEnvelopeId(envelopeId);
			documentVersion.setVersionOrder("1.0");
			documentVersion.setDocVersion(0);
			documentVersion.setCreatedOn(new Date());
			documentVersion.setPath(containerName + envelopeId + "/V" + (documentVersion.getDocVersion() + 1));
//			for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
//				Document newDoc = new Document();
//				newDoc.setDocumentId(documentResponse.getDocumentId());
//				newDoc.setName(documentResponse.getName());
//				versionDocDocuments.add(newDoc);
////				DocumentResponse docResponse = new DocumentResponse();
////				docResponse.setDocumentId(documentResponse.getDocumentId());
////				docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
////				docResponse.setName(documentResponse.getName());
////				docResponse.setDocumentBase64(documentResponse.getDocumentBase64());
//				MultipartFile multipartFile = Base64ToMultipartFileConverter
//						.convert(documentResponse.getDocumentBase64(), documentResponse.getName(), "text/plain");
//				multipartFiles.add(multipartFile);
//				documentResponses.add(documentResponse);
//			}
			// documentVersion.setDocuments(versionDocDocuments);
			documentVersionDocumentRepository.save(documentVersion);
//			String json2 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
//			JsonNode rootNode2 = mapper.readTree(json2);
			// envelopeDocument.setDocuments(BlobdocumentResponses);
			envelopeDocument.setStartDate(new Date());
			envelopeRepository.save(envelopeDocument);
//			loaContractDocument.setSenderEmail(rootNode2.path(Constant.SENDER).path(Constant.EMAIL).asText());
//			loaContractDocument.setSenderName(rootNode2.path(Constant.SENDER).path(Constant.USERNAME).asText());
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
			centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
			centralRepoDocument.setEnvelopeId(envelopeId);
			centralRepoDocument.setRepositoryName(Constant.LOA_DOCUMENT_REPOSITORY);
			centralRepoDocumentRepository.save(centralRepoDocument);
		}
		return new CommonResponse(HttpStatus.CREATED, new Response(Constant.LOA_DOCUMENT_RESPONSE, "Tested"),
				"Loa document details submitted successfully");
	}

	@Override
	public Object uploadDocuments(String envelopeId, String documentId, MultipartFile createDocumentFiles,
			String flowType, String email)
			throws IOException, URISyntaxException, StorageException, DataValidationException {
		UsersInfoDocument usersInfoDocument = infoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		if(createDocumentFiles.isEmpty())
		       throw new DataValidationException("File is empty.Please try again!", "400", HttpStatus.BAD_REQUEST);
		if (flowType.equalsIgnoreCase("external")) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			System.out.println(createDocumentFiles.getBytes());
			body.add("document-file", createDocumentFiles.getResource());
			body.add(Constant.ENVELOPE_ID, envelopeId);
			body.add("documentId", documentId);
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
			try {
				ResponseEntity<Object> responseEntity = restTemplate.exchange(docusignHost + "/updateDocument",
						HttpMethod.POST, requestEntity, Object.class);
				if (responseEntity.getStatusCode() == HttpStatus.OK) {
					log.info("Success in Upload Documents");
					uploadFilesIntoBlob(createDocumentFiles, envelopeId, "V1", documentId);
					if (createDocumentFiles.getOriginalFilename() != null) {
						getFileUpload(envelopeId, documentId, createDocumentFiles.getOriginalFilename());
					} else {
						getFileUpload(envelopeId, documentId, "Document-" + documentId);
					}
					return new CommonResponse(HttpStatus.OK, new Response("Document Upload Response", new ArrayList<>()),
							"Document upload successfull");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Document Upload Response",responseEntity.getBody()),
						"Document upload Unsuccessfull");
			    } catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				String responseBody = ex.getResponseBodyAsString();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Document Upload Response", "Internal Server Error"), responseBody);
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response("Document Upload Response", new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Document Upload Response", new ArrayList<>()), ex.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Document Upload Response", new ArrayList<>()), "Document upload Unsuccessfull");

			}
		} else if (flowType.equalsIgnoreCase("internal")) {
			try {
				uploadFilesIntoBlob(createDocumentFiles, envelopeId, "V1", documentId);
				if (createDocumentFiles.getOriginalFilename() != null) {
					getFileUpload(envelopeId, documentId, createDocumentFiles.getOriginalFilename());
				} else {
					getFileUpload(envelopeId, documentId, "Document-" + documentId);
				}
				return new CommonResponse(HttpStatus.OK, new Response("Document Upload Response", ""),
						"Document upload successfull");
			} catch (Exception e) {
				e.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Document Upload Response", "Internal Server Error"),
						"Document upload Unsuccessfull");
			}
		}
		return "Enter valid flowType";
	}

	@Override
	public CommonResponse templateDocumentUploadToEnvelope(String envelopeId, String templateId, String docRequest,
			String envelopeDocumentId, String documentType, String flowType)
			throws DataValidationException, JsonMappingException, JsonProcessingException {
		DocumentResponse jsonrequest = mapper.readValue(docRequest, DocumentResponse.class);
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl(getDousignUrl().getUploadTemplateFileToEnvelope().replace(Constant.DOCUSIGN_HOST,
						docusignHost.trim()))
				.queryParam(Constant.ENVELOPE_ID, envelopeId)
				.queryParam("templateDocument", mapper.writeValueAsString(jsonrequest))
				.queryParam("templateId", templateId).queryParam("envelopeDocumentId", envelopeDocumentId)
				.queryParam("documentType", documentType).queryParam("flowType", flowType);
		try {
			ResponseEntity<?> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null,
					String.class);
			System.out.println(responseEntity.getBody());
			getFileUpload(envelopeId, envelopeDocumentId, jsonrequest.getName());
			return new CommonResponse(HttpStatus.OK,
					new Response("TemplateFileUploadToEnvelopeResponse", responseEntity.getBody()),
					"File Uploaded Successfully");
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateFileUploadToEnvelopeResponse", new ArrayList<>()), responseBody);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("TemplateFileUploadToEnvelopeResponse", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateFileUploadToEnvelopeResponse", new ArrayList<>()), ex.getLocalizedMessage());
		}
	}

	public void uploadFilesIntoBlob(MultipartFile multipartFiles, String envelopeId, String version, String documentId)
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
	public ResponseEntity<byte[]> downloadDocuments(String envelopeId) {
		log.info("Building URL");
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl(
						getDousignUrl().getDownloadDocuments().replace(Constant.DOCUSIGN_HOST, docusignHost.trim()))
				.queryParam(Constant.ENVELOPE_ID, envelopeId);
		log.info("URL : {}", builder.toUriString());
		try {
			log.info("Calling Download Document API");
			return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null, byte[].class);
		} catch (HttpClientErrorException.BadRequest ex) {
			log.error("Inside catch-bloack Download Document API");
			ex.printStackTrace();
			return null;
		}
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

	@Override
	public void documentVersionEmail(String envelopeId, String versionOrder) throws Exception {
		CentralRepoDocument centralRepoDocument = centralRepoDocumentRepository.findByEnvelopeId(envelopeId);
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeId(envelopeId);
		String moduleName = "", flowType = "clm";
		try {
			if (centralRepoDocument == null) {
				throw new Exception("Cannot find module for specified envelopeId");
			}
				switch (centralRepoDocument.getRepositoryName()) {
				case Constant.LOA_DOCUMENT_REPOSITORY:
					moduleName = Constant.LOA_MODULE;
					flowType = Constant.PCD;
					break;
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					moduleName = Constant.CONTRACT_MODULE;
					flowType = Constant.PCD;
					break;
				case Constant.CMU_DOCUMENT_REPOSITORY:
					moduleName = Constant.CMU_MODULE;
					flowType = Constant.COMMERCIAL;
					break;
				case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
					moduleName = Constant.LOO_MODULE;
					flowType = Constant.COMMERCIAL;
					break;
				case Constant.TA_DOCUMENT_REPOSITORY:
					moduleName = Constant.TA_MODULE;
					flowType = Constant.COMMERCIAL;
					break;
				}
			if (!reviewerList.isEmpty()) {
				for (ReviewerDocument r : reviewerList) {
					PostCommentMailBody mailBody = new PostCommentMailBody(r.getEmail(), r.getReviewerName(), null,
							r.getContractName(), null, flowType, envelopeId, moduleName, String.valueOf(versionOrder));
					mailSenderService.sendDocumentVersionCommentMail(mailBody);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
}
