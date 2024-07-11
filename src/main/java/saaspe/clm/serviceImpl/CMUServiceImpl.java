package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import saaspe.clm.document.CreateTemplate;
import saaspe.clm.document.DocumentVersionDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
import saaspe.clm.docusign.model.DocumentVersioningResponse;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.ApprovedCMUContractResponse;
import saaspe.clm.model.ApprovedCmuPagination;
import saaspe.clm.model.CmuDocumentDetailResponse;
import saaspe.clm.model.CmuDocumentRequest;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Currency;
import saaspe.clm.model.Document;
import saaspe.clm.model.DocumentCreateResponse;
import saaspe.clm.model.DocumentResponse;
import saaspe.clm.model.DocumentVersionDocumentResponse;
import saaspe.clm.model.DocusignUrls;
import saaspe.clm.model.Response;
import saaspe.clm.model.Reviewers;
import saaspe.clm.model.UserDetails;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.CmuContractDocumentRepository;
import saaspe.clm.repository.CreateTemplateRepository;
import saaspe.clm.repository.DocumentVersionDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.CMUService;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.service.WorkflowService;
import saaspe.clm.utills.AllRoleFlowMapper;
import saaspe.clm.utills.Base64ToMultipartFileConverter;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

@Service
public class CMUServiceImpl implements CMUService {

	private static Logger log = LoggerFactory.getLogger(CMUServiceImpl.class);

	@Autowired
	private RedisUtility redisUtility;

	@Value("${redirecturl.path}")
	private String redirectUrl;

	@Value("${sendgrid.domain.name}")
	private String mailDomainName;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Autowired
	private CmuContractDocumentRepository cmuContractDocumentRepository;

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private DocumentVersionDocumentRepository documentVersionDocumentRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository workFlowCreatorDocumentRespository;

	@Autowired
	private CreateTemplateRepository createTemplateRepository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

    @Autowired
    private WorkflowService workflowService;

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

    public CMUServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public DocusignUrls getDousignUrl() {
		ClassPathResource resource = new ClassPathResource(docusignUrls);
		ObjectMapper objectMapper = new ObjectMapper();
		DocusignUrls docusignUrl = null;
		try {
			docusignUrl = objectMapper.readValue(resource.getInputStream(), DocusignUrls.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return docusignUrl;
	}

	public String generateProjectId(String value) {
		while (value.length() < 6) {
			value = '0' + value;
		}
		return value;
	}

	@Override
	public CommonResponse updateCmuDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles,
			String[] update_id, String[] delete_id, String envId, String email)
			throws Exception {
		CmuDocumentRequest jsonrequest = new CmuDocumentRequest();
		List<MultipartFile> multipartFiles = new ArrayList<>();
		List<String> documentIds = new ArrayList<>();
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
					documentRequests.add(documentRequest);
					documentIds.add(String.valueOf(randomNumber));
					MultipartFile multipartFile = Base64ToMultipartFileConverter
							.convert(documentRequest.getDocumentBase64(), documentRequest.getName(), "text/plain");
					multipartFiles.add(multipartFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!deleteIdsList.isEmpty()) {
			log.info("Entered into delete:");
			for (String deleteid : deleteIdsList) {
				saaspe.clm.model.Document documentRequest = new saaspe.clm.model.Document();
				log.info("deleteId :{}",deleteid);
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
						documentRequests.add(documentRequest);
						MultipartFile multipartFile = Base64ToMultipartFileConverter
								.convert(documentRequest.getDocumentBase64(), documentRequest.getName(), "text/plain");
						multipartFiles.add(multipartFile);
						documentIds.add(updatedIdsList.get(i));
					} catch (IOException e) {
						e.printStackTrace();
					}
					i++;
				}
			}
		}
		jsonrequest.setDocuments(documentRequests);
		jsonrequest.setUserEmail(email);
		String envelopeId = envId;
		Map<String, String> evnelopeIdmap = new HashMap<>(); // ?
		evnelopeIdmap.put("envelopeId", envelopeId);
		List<DocumentVersionDocument> documentVersions = documentVersionDocumentRepository.findByEnvelopeId(envId);
		System.out.println(documentVersions.size());
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
		List<Document> oldDocuments = getCMUDocumentFromBlob(currentDocument); // load it from blob
		for (Document document : oldDocuments) { // get old file store in multipart file
			if (updatedIdsList.contains(document.getDocumentId())) {
				continue;
			}
			MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(document.getDocumentBase64(),
					document.getName(), "text/plain");
			documentIds.add(document.getDocumentId());
			multipartFiles.add(multipartFile);
		}
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
				Iterator<Document> iterator = newdocuments.iterator();
				while (iterator.hasNext()) {
				    Document element = iterator.next();
				    if (element.getDocumentId().equalsIgnoreCase(document.getDocumentId())) {
				        iterator.remove();
				    }
				}
				Document updateDocument = new Document();
				updateDocument.setName(document.getName());
				updateDocument.setDocumentId(document.getDocumentId());
				newdocuments.add(updateDocument);
			}
		}
		newdoc.setDocuments(newdocuments);
		documentVersionDocumentRepository.save(newdoc);
		try {
			uploadFilesIntoBlob(multipartFiles, envId, null, "V" + (newdoc.getDocVersion() + 1), documentIds);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<ReviewerDocument> reviewersList = reviewerDocumentRepository.findByEnvelopeId(envId);
		boolean allSameRoutingOrder = reviewersList.stream().mapToInt(ReviewerDocument::getRoutingOrder).distinct()
				.count() == 1;
		reviewersList.forEach(reviewerdocument -> {
			reviewerdocument.setCompleted(false);
			reviewerdocument.setOrderFlag(allSameRoutingOrder || reviewerdocument.getRoutingOrder() == 1);
		});
		reviewerDocumentRepository.saveAll(reviewersList);
		return new CommonResponse(HttpStatus.CREATED, new Response("CMU Contract Update Response", evnelopeIdmap),
				"CMU Document updated successfully");
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

	@Override
	public CommonResponse getApprovedCMUContractList(HttpServletRequest request) {
	
		List<CmuContractDocument> approvedLoaList = cmuContractDocumentRepository.findApprovedCMUContractList(true);
		List<ApprovedCMUContractResponse> list = new ArrayList<>();
		for (CmuContractDocument cmuDocument : approvedLoaList) {
			ApprovedCMUContractResponse response = new ApprovedCMUContractResponse();
			response.setEnvelopeId(cmuDocument.getEnvelopeId());
			response.setContractTitle(cmuDocument.getContractTitle());
			list.add(response);
		}
		return new CommonResponse(HttpStatus.OK, new Response("CMU Contract ApprovedList Response", list),
				"CMU Contract Approved list fetched successfully");
	}

	@Override
	public CommonResponse getCMUContractDocumentDetailsView(String envelopeId,String email)
            throws DataValidationException, JsonProcessingException, URISyntaxException, StorageException {

		UsersInfoDocument usersInfoDocument=userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		CmuDocumentDetailResponse documentDetailResponse = new CmuDocumentDetailResponse();
		log.info("envelopeId: {}",envelopeId);
		CmuContractDocument cmuDocument = cmuContractDocumentRepository.findByEnvelopeId(envelopeId);
		log.info("cmuDocument: {}",cmuDocument);
		List<DocumentVersionDocument> documentVersionDocumentList = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
		if (cmuDocument == null)
			throw new DataValidationException("Please provide the valid envelope ID", "400", HttpStatus.BAD_REQUEST);
		CreateTemplate template = createTemplateRepository.findByTemplateId(cmuDocument.getTemplateId());
		if (template == null) {
			throw new DataValidationException("Provide valid Template Id", "400", HttpStatus.BAD_REQUEST);
		}
		documentDetailResponse.setTemplateName(template.getTemplateName());
		documentDetailResponse.setEmailSubject(cmuDocument.getEmailSubject());
		documentDetailResponse.setEmailMessage(cmuDocument.getEmailMessage());
		documentDetailResponse.setContractTitle(cmuDocument.getContractTitle());
		documentDetailResponse.setTenant(cmuDocument.getTenant());
		documentDetailResponse.setTenderNo(cmuDocument.getTenderNo());
		documentDetailResponse.setTradingName(cmuDocument.getTradingName());
		documentDetailResponse.setTenancyTerm(cmuDocument.getTenancyTerm());
		documentDetailResponse.setCommencementDate(cmuDocument.getCommencementDate());
		documentDetailResponse.setCommencementBusinessDate(cmuDocument.getCommencementBusinessDate());
		documentDetailResponse.setExpiryDate(cmuDocument.getExpiryDate());
		documentDetailResponse.setRent(cmuDocument.getRent());
		documentDetailResponse.setLotNumber(cmuDocument.getLotNumber());
		documentDetailResponse.setLocation(cmuDocument.getLocation());
		documentDetailResponse.setArea(cmuDocument.getArea());
		documentDetailResponse.setAirport(cmuDocument.getAirport());
		documentDetailResponse.setStatus(cmuDocument.getStatus());
		documentDetailResponse.setSenderEmail(cmuDocument.getSenderEmail());
		documentDetailResponse.setSenderName(cmuDocument.getSenderName());
		documentDetailResponse.setEnvelopeId(cmuDocument.getEnvelopeId());
		documentDetailResponse.setSubsidiary(cmuDocument.getSubsidiary());
		documentDetailResponse.setTemplateId(cmuDocument.getTemplateId());
		documentDetailResponse.setProjectId(cmuDocument.getProjectId());
		documentDetailResponse.setCategory(cmuDocument.getCategory());
		DocumentVersionDocument documentVersionDocument = documentVersionDocumentList.stream().
                max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion)).get();
        CommonResponse documentVersionResponse = workflowService.getDocumentVersionDocuments(documentVersionDocument.getEnvelopeId(),documentVersionDocument.getDocVersion());
        documentDetailResponse.setDocumentVersionDocumentResponses((List<DocumentVersionDocumentResponse>) documentVersionResponse.getResponse().getData());
		return new CommonResponse(HttpStatus.OK, new Response("CMU Document Response", documentDetailResponse),
				"CMU Document details Retrieved Successfully");
	}

	@Override
	public CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId)
			throws DataValidationException {
		List<DocumentVersioningResponse> response = new ArrayList<>();
		String email = getUserFromToken(request).getUserEmail();
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		 UsersInfoDocument userInfo=userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(userInfo, envelopeId))
				throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
//		 String userType = (cacheValue != null) ? cacheValue.getDisplayname():(userInfo.getCurrentRole()!=null?userInfo.getCurrentRole():null);
		String flowType = (cacheValue != null) ? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()) :(userInfo.getCurrentRole()!=null?userInfo.getCurrentRole():null) ;
		CmuContractDocument cmuContractDocument = null;
		log.info("Cache DisplayName :: {}",cacheValue.getDisplayname());
		log.info("flowType :: {}",flowType);
		if (flowType.equalsIgnoreCase("CMU_Create") || flowType.equalsIgnoreCase("CMU_Review")||flowType.equalsIgnoreCase("COMMERCIAL_ADMIN")) {
			cmuContractDocument = cmuContractDocumentRepository.findByEnvelopeId(envelopeId);
			if (cmuContractDocument == null) {
				throw new DataValidationException("Please provide valid Envelope Id", "400", HttpStatus.BAD_REQUEST);
			}
		}
		List<DocumentVersionDocument> documentList = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
		log.info("DocumentList :: {}",documentList.size());
		for (DocumentVersionDocument documentVersionDocument : documentList) {
			DocumentVersioningResponse documentVersioning = new DocumentVersioningResponse();
			documentVersioning.setEnvelopeId(documentVersionDocument.getEnvelopeId());
			documentVersioning.setOrder(documentVersionDocument.getDocVersion());
			documentVersioning.setDocumentVersion(documentVersionDocument.getVersionOrder());
			documentVersioning.setCreatedTime(documentVersionDocument.getCreatedOn());
			documentVersioning.setUpdatedTime(documentVersionDocument.getUpdatedOn());
			documentVersioning.setEnvelopeName(cmuContractDocument!=null?cmuContractDocument.getContractTitle():null);
			response.add(documentVersioning);
		}
		Collections.sort(response, Comparator.comparingInt(DocumentVersioningResponse::getOrder).reversed());
		return new CommonResponse(HttpStatus.OK, new Response("Document Versioning Response", response),
				"Documents fetched successfully");
	}

	@Override
	public CommonResponse getApprovedCmuAllList(HttpServletRequest request, int page, int size,String searchText, String order, String orderBy, String subsidiary, String status) {
		Sort sort = null;
		PageRequest pageable = null;
		if (order != null && !order.isEmpty()) {
			Sort.Direction sortDirection = Sort.Direction.ASC;
			if (order.equalsIgnoreCase("desc")) {
				sortDirection = Sort.Direction.DESC;
			}
			sort = Sort.by(sortDirection, orderBy);
			pageable = PageRequest.of(page, size, sort);
		}
		long totalCount = 0;
		List<CmuContractDocument> list = null;
		if (order == null) {
			pageable = PageRequest.of(page, size);
			totalCount = cmuContractDocumentRepository.countAllByIsFlowCompleted(true);
			list = cmuContractDocumentRepository.findByIsFlowCompleted(true,pageable);
		} else {
			Query query = new Query();
			query.addCriteria(Criteria.where("isFlowCompleted").is(true));
			if (subsidiary != null && !subsidiary.isBlank()) {
				Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
				query.addCriteria(Criteria.where("subsidiary").regex(pattern));
			}
			if (status != null && !status.isEmpty()) {
				query.addCriteria(Criteria.where("status").regex(status, "i"));
			}
			Criteria criteria = new Criteria();
			if(searchText!=null&&!searchText.isEmpty()) {
				Pattern pattern = Pattern.compile(Pattern.quote(searchText),Pattern.CASE_INSENSITIVE);
				criteria.orOperator(
						Criteria.where("contractTitle").regex(pattern),
						Criteria.where("projectId").regex(pattern)
				);
				query.addCriteria(criteria);
			}
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query,CmuContractDocument.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query,CmuContractDocument.class).stream().collect(Collectors.toList());
		}
		List<CmuContractDocument> approvedCmuList = list;
		List<ApprovedCMUContractResponse> approvedCmuListResponses = new ArrayList<>();
		for (CmuContractDocument cmuDocument : approvedCmuList) {
			ApprovedCMUContractResponse response = new ApprovedCMUContractResponse();
			response.setEnvelopeId(cmuDocument.getEnvelopeId());
			response.setProjectId(cmuDocument.getProjectId());
			response.setContractTitle(cmuDocument.getContractTitle());
			response.setStatus(cmuDocument.getStatus());
			response.setCommencementDate(cmuDocument.getCommencementDate());
			response.setExpiryDate(cmuDocument.getExpiryDate());
			response.setSubsidiary(cmuDocument.getSubsidiary());
			response.setCreatedOn(cmuDocument.getCreatedOn());
			response.setTenant(cmuDocument.getTenant());
			approvedCmuListResponses.add(response);
		}
		ApprovedCmuPagination data = new ApprovedCmuPagination(totalCount, approvedCmuListResponses);
		return new CommonResponse(HttpStatus.OK, new Response("CmuApprovedAllListResponse", data),
				"CmuApproved Alllist fetched successfully");
	}

	@Override
	public CommonResponse getCreateListForCmu(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy, String subsidiary, String status,String category) throws DataValidationException, UnsupportedEncodingException {
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
		if (!( currentRole.equalsIgnoreCase("CMU_CREATOR") || 
			      currentRole.equalsIgnoreCase("LOO_CREATOR") || 
			      (currentRole.equalsIgnoreCase("COMMERCIAL_ADMIN") || currentRole.equalsIgnoreCase("SUPER_ADMIN")||currentRole.equalsIgnoreCase("LEGAL_ADMIN")) 
					|| currentRole.equalsIgnoreCase("LEGAL_USER")|| currentRole.equalsIgnoreCase("COMMERCIAL_USER") )  ) {
			    throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
			}
		List<CmuContractDocument> list;
		if (order == null) {
			totalCount = cmuContractDocumentRepository.findAll().size();
			list = cmuContractDocumentRepository.findAll(pageable).getContent();
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
			Criteria criteria = new Criteria();
			if(searchText!=null&&!searchText.isEmpty()) {
				searchText= URLDecoder.decode(searchText, "UTF-8");
			Pattern pattern = Pattern.compile(Pattern.quote(searchText),Pattern.CASE_INSENSITIVE);
				criteria.orOperator(
						Criteria.where("contractTitle").regex(pattern),
						Criteria.where("projectId").regex(pattern)
				);
				query.addCriteria(criteria);
			}
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query,CmuContractDocument.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query,CmuContractDocument.class).stream().collect(Collectors.toList());
		}
		List<String> listOfEnvelopeIds = list.stream().map(document -> document.getEnvelopeId())
				.collect(Collectors.toList());
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		for (CmuContractDocument cmuContractDocument : list) {
			List<String> reviewers = new ArrayList<>();
			List<ReviewerDocument> reviewerslist = reviewerList.stream()
					.filter(document -> document.getEnvelopeId().equals(cmuContractDocument.getEnvelopeId()))
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
			DocumentCreateResponse resp = DocumentCreateResponse.builder().contractTitle(cmuContractDocument.getContractTitle())
					.createdOn(cmuContractDocument.getCreatedOn())
					.projectId(cmuContractDocument.getProjectId()).ownersEmail(email)
					.senderName(cmuContractDocument.getSenderName())
					.envelopeId(cmuContractDocument.getEnvelopeId())
					.status(cmuContractDocument.getStatus()).pendingWith(reviewers)
					.commencementDate(cmuContractDocument.getCommencementDate())
					.expiryDate(cmuContractDocument.getExpiryDate())
					.subsidiary(cmuContractDocument.getSubsidiary())
					.tenant(cmuContractDocument.getTenant()).build();
			createResponses.add(resp);
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", createResponses);
		return new CommonResponse(HttpStatus.OK, new Response("CreateListForCmuResponse", commonResponse),
				"Cmu List details fetched successfully");
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
					.getBlockBlobReference(documentVersionblob + path + "/" + version + "/" + documentId);
			log.info("blob: {}",blob);
			blob.getProperties().setContentType("application/pdf");
			try (InputStream inputStream = file.getInputStream()) {
				blob.upload(inputStream, file.getSize());
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	public List<Document> getCMUDocumentFromBlob(DocumentVersionDocument documentVersionDocument) throws Exception {
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		List<Document> documents = new ArrayList<>();
		for(Document document: documentVersionDocument.getDocuments()) {
			Document newDocument = new Document();
			String path = documentVersionDocument.getPath()+"/"+document.getDocumentId();
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

	@Override
	public CommonResponse newAddCmuDocument(String json, String templateId, String email, String name) throws IOException, TemplateException, MessagingException, DataValidationException 
	{
		try {
		ObjectMapper mapper = new ObjectMapper();
		CmuDocumentRequest jsonrequest = mapper.readValue(json, CmuDocumentRequest.class); // request model
		DocumentVersionDocument documentVersion = new DocumentVersionDocument();
		documentVersion.setId(sequenceGeneratorService.generateSequence(DocumentVersionDocument.SEQUENCE_NAME));
		jsonrequest.setUserEmail(email);
		CmuContractDocument cmuContractDocument = new CmuContractDocument();
		Long id = sequenceGeneratorService.generateSequence(CmuContractDocument.SEQUENCE_NAME);
		String projectId = generateProjectId(id + "");
		String envelopeId = UUID.randomUUID().toString(); // change from projectID to envelop id
		Map<String, String> envelopeIdmap = new HashMap<>();
		envelopeIdmap.put("envelopeId", envelopeId);   // envelopIdmap
		cmuContractDocument.setId(id);
		cmuContractDocument.setProjectId(projectId);
		cmuContractDocument.setIsFlowCompleted(false);
		cmuContractDocument.setTemplateId(templateId);
		cmuContractDocument.setBuID("BUID");
		cmuContractDocument.setOpID("SAASPE");
		cmuContractDocument.setCreatedOn(new Date());
		cmuContractDocument.setCreatedBy(email);
		cmuContractDocument.setStatus(jsonrequest.getStatus());
		cmuContractDocument.setVersion("1.0");
		cmuContractDocument.setOrder(0);
		cmuContractDocument.setReferenceId(templateId);
		cmuContractDocument.setReferenceType("Template");
		cmuContractDocument.setReviewerSigningOrder(jsonrequest.getReviewerSigningOrder());
		cmuContractDocument.setSignerSigningOrder(jsonrequest.getSignerSigningOrder());
		Currency currency = new Currency(jsonrequest.getRent().getCurrencyCode(), jsonrequest.getRent().getTotalCost(), jsonrequest.getRent().getTax());
		cmuContractDocument.setRent(currency);
		cmuContractDocument.setContractTitle(jsonrequest.getContractTitle());
		cmuContractDocument.setTenant(jsonrequest.getTenant());
		cmuContractDocument.setTenderNo(jsonrequest.getTenderNo());
		cmuContractDocument.setTradingName(jsonrequest.getTradingName());
		cmuContractDocument.setTenancyTerm(jsonrequest.getTenancyTerm());
		cmuContractDocument.setCommencementDate(jsonrequest.getCommencementDate());
		cmuContractDocument.setCommencementBusinessDate(jsonrequest.getCommencementBusinessDate());
		cmuContractDocument.setExpiryDate(jsonrequest.getExpiryDate());
		cmuContractDocument.setLotNumber(jsonrequest.getLotNumber());
		cmuContractDocument.setLocation(jsonrequest.getLocation());
		cmuContractDocument.setArea(jsonrequest.getArea());
		cmuContractDocument.setAirport(jsonrequest.getAirport());
		cmuContractDocument.setSubsidiary(jsonrequest.getSubsidiary());
		cmuContractDocument.setCompletionDate(jsonrequest.getCompletionDate());
		cmuContractDocument.setCategory(jsonrequest.getCategory());
		documentVersion.setEnvelopeId(envelopeId);
		documentVersion.setVersionOrder("1.0");
		documentVersion.setDocVersion(0);
		documentVersion.setCreatedOn(new Date());
		documentVersion.setPath(containerName + envelopeId + "/V" + (documentVersion.getDocVersion()+1));
		documentVersionDocumentRepository.save(documentVersion);
		Date createDate = new Date();
		WorkFlowCreatorDocument creator = new WorkFlowCreatorDocument();
		creator.setId(sequenceGeneratorService.generateSequence(WorkFlowCreatorDocument.SEQUENCE_NAME));
		creator.setEnvelopeId(envelopeId);
		creator.setProjectId(projectId);
		creator.setCreatedOn(new Date());
		creator.setContractName(jsonrequest.getContractTitle());
		creator.setFlowType("CMU_Create");
		List<String> pendingWith = new ArrayList<>();
		creator.setEmail(email);
		creator.setTenantName(jsonrequest.getTenant());
		creator.setStartDate(jsonrequest.getCommencementDate());
		creator.setExpiryDate(jsonrequest.getExpiryDate());
		creator.setSubsidiary(jsonrequest.getSubsidiary());
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
			reviewerDocument.setCreatedBy(email);
			reviewerDocument.setContractName(jsonrequest.getContractTitle());
			reviewerDocument.setSenderName(name);
			reviewerDocument.setFlowType("CMU_Review");
			reviewerDocument.setReviewerName(reviewer.getName());
			reviewerDocument.setSubsidiary(jsonrequest.getSubsidiary());
			reviewerDocument.setOrderFlag(reviewer.getRoutingOrder() == firstRoutingOrder || hasSameRoutingOrder);
			reviewerDocument.setCommencementDate(jsonrequest.getCommencementDate());
			reviewerDocument.setExpiryDate(jsonrequest.getExpiryDate());
			reviewerDocument.setProjectId(projectId);
			reviewerDocument.setStatus(jsonrequest.getStatus());
			reviewerDocument.setTenant(jsonrequest.getTenant());
			if (reviewer.getRoutingOrder() == firstRoutingOrder) {
				pendingWith.add(reviewer.getEmail());
				mailSenderService.sendRequestForReviewMail(envelopeId, reviewer.getEmail(), reviewer.getName(),
						jsonrequest.getContractTitle(), cmuContractDocument.getVersion(),
						cmuContractDocument.getCreatedBy(), "commercial", Constant.CMU_MODULE);
			}
			reviewerDocument.setRoutingOrder(reviewer.getRoutingOrder());
			log.info("Saving Reviewer Document");
			reviewerDocumentRepository.save(reviewerDocument);
		}
		creator.setPendingWith(pendingWith);
		workFlowCreatorDocumentRespository.save(creator);
			cmuContractDocument.setSenderEmail(email);
			cmuContractDocument.setSenderName(name);
			cmuContractDocument.setEnvelopeId(envelopeId);
			cmuContractDocument.setEmailSubject(jsonrequest.getEmailSubject());
			cmuContractDocument.setEmailMessage(jsonrequest.getEmailMessage());
			cmuContractDocumentRepository.save(cmuContractDocument);
			CentralRepoDocument centralRepoDocument = new CentralRepoDocument();
            centralRepoDocument.setId(sequenceGeneratorService.generateSequence(CentralRepoDocument.SEQUENCE_NAME));
            centralRepoDocument.setEnvelopeId(envelopeId);
            centralRepoDocument.setRepositoryName(Constant.CMU_DOCUMENT_REPOSITORY);
            centralRepoDocumentRepository.save(centralRepoDocument);
            return new CommonResponse(HttpStatus.CREATED, new Response(Constant.CMU_DOCUMENT_RESPONSE, envelopeIdmap),
				"CMU document details submitted successfully");
	}
      catch (Exception e) {
	  e.printStackTrace();
	  if (e instanceof MismatchedInputException) {
		 throw new DataValidationException("Fields mismatch or malformed payload. Please try again!", "400", HttpStatus.BAD_REQUEST);
         }
	  throw new DataValidationException(e.getMessage(), "400", HttpStatus.BAD_REQUEST);
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
		} 
		else if (commercialEnvelope.contains(reponame) && Constant.COMMERCIAL_ADMIN_ACCESS_LIST.contains(currentRole)) {
			flag = true;
		}
		return flag;
	}

}
