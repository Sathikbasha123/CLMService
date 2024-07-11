package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import freemarker.template.TemplateException;
import saaspe.clm.constant.Constant;
import saaspe.clm.document.*;
import saaspe.clm.docusign.model.Comments;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.*;
import saaspe.clm.repository.*;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.service.WorkflowService;
import saaspe.clm.utills.AllRoleFlowMapper;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

@Service
public class WorkflowServiceImpl implements WorkflowService {

	private static Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private DocumentVersionDocumentRepository documentVersionRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository creatorDocumentRespository;

	@Autowired
	private LoaDocumentRepository loaDocumentRepository;

	@Autowired
	private LoaContractDocumentRepository loacontractDocumentRepository;

	@Autowired
	private MailSenderService mailSenderService;

	@Autowired
	private CmuContractDocumentRepository cmuDocumentRepository;

	@Autowired
	private LooContractDocumentRepository looDocumentRepository;

	@Autowired
	private TADocumentRepository taDocumentRepository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Value("${spring.mail.username}")
	private String fromMail;

	@Value("${sendgrid.domain.orgname}")
	private String orgName;

	@Value("${sendgrid.domainname}")
	private String domainName;

	@Value("${sendgrid.domain.orgname}")
	private String senderName;

	@Value("${azure.storage.container.name}")
	private String containerName;

	@Value("${spring.image.key}")
	private String imageKey;

	@Value("${spring.media.host}")
	private String mediaHost;

	@Value("${sendgrid.domain.support}")
	private String supportMail;

	@Value("${docusign-urls-file}")
	private String docusignUrls;

	@Value("${docusign.host}")
	private String docusignHost;

	@Value("${spring.host}")
	private String host;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RedisUtility redisUtility;

	@Autowired
	private CloudBlobClient cloudBlobClient;

	private final MongoTemplate mongoTemplate;

	public WorkflowServiceImpl(MongoTemplate mongoTemplate) {
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
	public CommonResponse getListOfClmContract(HttpServletRequest request, int page, int limit, String searchText,
			String order, String orderBy, String status, String subsidiary, String customFlowType,String category)
			throws DataValidationException {
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
		List<ReviewerWorkflowListResponse> listResponse = new ArrayList<>();
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
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		System.out.println(cacheValue.getDisplayname());
		String flowType = (cacheValue != null) ? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()) : null;
		log.info("flow type: {}", flowType);
		List<ReviewerDocument> reviewerList;
		long totalCount = 0;
		if (order == null) {
			totalCount = reviewerDocumentRepository.findLatestDocumentsByEnvelopeAndEmail(email, true, false, flowType)
					.size();
			reviewerList = reviewerDocumentRepository.findLatestDocumentsByEnvelopeAndEmail(email, flowType, true,
					pageable, false);
		} else {
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			Query query = new Query();
			query.collation(collation);
			if (cacheValue.getDisplayname().equalsIgnoreCase(Constant.PCD_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.SUPER_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.PCD_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				if (customFlowType != null) {
					flowType = AllRoleFlowMapper.getFlowType(customFlowType);
					query.addCriteria(Criteria.where("email").is(email));
					query.addCriteria(Criteria.where("flowType").is(flowType));
				} else {
					List<String> flowTypeList = new ArrayList<>();
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER));
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER));
					query.addCriteria(Criteria.where("flowType").in(flowTypeList));
				}
			} else if (cacheValue.getDisplayname().equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.SUPER_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.COMMERCIAL_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				if (customFlowType != null) {
					flowType = AllRoleFlowMapper.getFlowType(customFlowType);
					query.addCriteria(Criteria.where("email").is(email));
					query.addCriteria(Criteria.where("flowType").is(flowType));
				} else {
					List<String> flowTypeList = new ArrayList<>();
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER));
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER));
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER));
					query.addCriteria(Criteria.where("flowType").in(flowTypeList));
				}
			} else {
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			query.addCriteria(Criteria.where("isCompleted").is(false));
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
			if (searchText != null && !searchText.isEmpty()) {
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("contractName").regex(pattern),
						Criteria.where("projectId").regex(pattern), Criteria.where("contractNumber").regex(pattern));
				query.addCriteria(criteria);
			}
			query.addCriteria(Criteria.where("orderFlag").is(true));
			log.info("Query {}", query);
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, ReviewerDocument.class);
			query.with(pageableObject);
			reviewerList = mongoTemplate.find(query, ReviewerDocument.class).stream().collect(Collectors.toList());
		}
		Map<String, Object> commonResponse = new HashMap<>();
		if (!reviewerList.isEmpty()) {
			List<String> listOfEnvelopeIds = reviewerList.stream().map(document -> document.getEnvelopeId())
					.collect(Collectors.toList());
			List<LoaDocument> loaDocumentsList = null;
			List<LoaContractDocument> loaContractDocumentsList = null;
			List<CmuContractDocument> cmuDocumentsList = null;
			List<LooContractDocument> looDocumentsList = null;
			List<TADocument> taDocumentList = null;
			if (flowType.equalsIgnoreCase("LOA_review")) {
				loaDocumentsList = loaDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
			}
			if (flowType.equalsIgnoreCase("CONTRACT_review")) {
				loaContractDocumentsList = loacontractDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
			}
			if (flowType.equalsIgnoreCase("CMU_review")) {
				cmuDocumentsList = cmuDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
			}
			if (flowType.equalsIgnoreCase("LOO_review")) {
				looDocumentsList = looDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
			}
			if (flowType.equalsIgnoreCase("TA_review")) {
				taDocumentList = taDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
			}
			for (ReviewerDocument reviewers : reviewerList) {
				if (loaDocumentsList != null) {
					LoaDocument loaDocument = loaDocumentsList.stream()
							.filter(document -> document.getEnvelopeId().equals(reviewers.getEnvelopeId())).findFirst()
							.orElse(null);
					ReviewerWorkflowListResponse response = ReviewerWorkflowListResponse.builder()
							.projectId(reviewers.getProjectId()).contractName(reviewers.getContractName())
							.envelopeId(reviewers.getEnvelopeId()).senderEmail(reviewers.getCreatedBy())
							.creatorName(reviewers.getCreatorName()).createdOn(reviewers.getCreatedOn())
							.status(loaDocument != null ? loaDocument.getStatus() : null)
							.commencementDate(reviewers.getCommencementDate())
							.completionDate(reviewers.getCompletionDate())
							.subsidiary(reviewers.getSubsidiary()).contractNumber(reviewers.getContractNumber())
							.vendorName(reviewers.getVendorName()).build();
					listResponse.add(response);
				}
				if (loaContractDocumentsList != null) {
					LoaContractDocument loaContractDocument = loaContractDocumentsList.stream()
							.filter(document -> document.getEnvelopeId().equals(reviewers.getEnvelopeId())).findFirst()
							.orElse(null);
					ReviewerWorkflowListResponse response = ReviewerWorkflowListResponse.builder()
							.projectId(reviewers.getProjectId()).contractName(reviewers.getContractName())
							.envelopeId(reviewers.getEnvelopeId()).senderEmail(reviewers.getCreatedBy())
							.creatorName(reviewers.getCreatorName()).createdOn(reviewers.getCreatedOn())
							.status(loaContractDocument != null ? loaContractDocument.getStatus() : null)
							.commencementDate(reviewers.getCommencementDate())
							.completionDate(reviewers.getCompletionDate()).subsidiary(reviewers.getSubsidiary())
							.contractNumber(reviewers.getContractNumber()).vendorName(reviewers.getVendorName())
							.build();
					listResponse.add(response);
				}
				if (cmuDocumentsList != null) {
					CmuContractDocument cmuDocument = cmuDocumentsList.stream()
							.filter(document -> document.getEnvelopeId().equals(reviewers.getEnvelopeId())).findFirst()
							.orElse(null);
					ReviewerWorkflowListResponse response = ReviewerWorkflowListResponse.builder()
							.projectId(reviewers.getProjectId()).contractName(reviewers.getContractName())
							.envelopeId(reviewers.getEnvelopeId()).senderEmail(reviewers.getCreatedBy())
							.creatorName(reviewers.getCreatorName()).createdOn(reviewers.getCreatedOn())
							.status(cmuDocument != null ? cmuDocument.getStatus() : null)
							.commencementDate(reviewers.getCommencementDate()).expiryDate(reviewers.getExpiryDate())
							.completionDate(reviewers.getCompletionDate()).subsidiary(reviewers.getSubsidiary())
							.tenant(reviewers.getTenant()).build();
					listResponse.add(response);
				}
				if (looDocumentsList != null) {
					LooContractDocument looDocument = looDocumentsList.stream()
							.filter(document -> document.getEnvelopeId().equals(reviewers.getEnvelopeId())).findFirst()
							.orElse(null);
					ReviewerWorkflowListResponse response = ReviewerWorkflowListResponse.builder()
							.projectId(reviewers.getProjectId()).contractName(reviewers.getContractName())
							.envelopeId(reviewers.getEnvelopeId()).senderEmail(reviewers.getCreatedBy())
							.creatorName(reviewers.getCreatorName()).createdOn(reviewers.getCreatedOn())
							.status(looDocument != null ? looDocument.getStatus() : null)
							.commencementDate(reviewers.getCommencementDate()).expiryDate(reviewers.getExpiryDate())
							.completionDate(reviewers.getCompletionDate()).subsidiary(reviewers.getSubsidiary())
							.tenant(reviewers.getTenant()).build();
					listResponse.add(response);
				}

				if (taDocumentList != null) {
					TADocument taDocument = taDocumentList.stream()
							.filter(document -> document.getEnvelopeId().equals(reviewers.getEnvelopeId())).findFirst()
							.orElse(null);
					ReviewerWorkflowListResponse response = ReviewerWorkflowListResponse.builder()
							.projectId(reviewers.getProjectId()).contractName(reviewers.getContractName())
							.envelopeId(reviewers.getEnvelopeId()).senderEmail(reviewers.getCreatedBy())
							.creatorName(reviewers.getCreatorName()).createdOn(reviewers.getCreatedOn())
							.status(taDocument != null ? taDocument.getStatus() : null)
							.commencementDate(reviewers.getCommencementDate()).expiryDate(reviewers.getExpiryDate())
							.completionDate(reviewers.getCompletionDate()).subsidiary(reviewers.getSubsidiary())
							.tenant(reviewers.getTenant()).build();
					listResponse.add(response);
				}
			}
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", listResponse);
		return new CommonResponse(HttpStatus.OK, new Response("CLMReviewerWorkflowResponse", commonResponse),
				"Workflow details fetched successfully");
	}

	@Override
	@Transactional
	public CommonResponse postCommentsOnEnvelope(HttpServletRequest request, CommentRequest body)
			throws DataValidationException {
		String token = request.getHeader(Constant.HEADER_STRING);
		String xAuthProvider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		String email = null, name = null;
		if (xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
				name = jwt.getClaim("name").asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
				name = jwt.getClaim("name").asString();
			}
		}
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(userInfo, body.getEnvelopeId()))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		String userType = (cacheValue != null) ? cacheValue.getDisplayname()
				: (userInfo.getCurrentRole() != null ? userInfo.getCurrentRole() : null);
		String flowType = null;
		String moduleName = null;
		CentralRepoDocument centralRepoDocument=centralRepoDocumentRepository.findByEnvelopeId(body.getEnvelopeId());
		moduleName = centralRepoDocument!=null?centralRepoDocument.getRepositoryName():"";
		if(moduleName.equalsIgnoreCase(Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY))
			 moduleName=Constant.CONTRACT_MODULE;
		if (Constant.PCD_ADMIN_ACCESS_LIST.contains(userType)||userType.equalsIgnoreCase("PCD_USER")) {
			flowType = Constant.PCD;
		}
		else if (Constant.COMMERCIAL_ADMIN_ACCESS_LIST.contains(userType)||userType.equalsIgnoreCase("COMMERCIAL_USER")) {
			flowType = Constant.COMMERCIAL;
		}
		else if(userType.equalsIgnoreCase("SUPER_ADMIN")||userType.equalsIgnoreCase("LEGAL_USER")||userType.equalsIgnoreCase("LEGAL_ADMIN")) {
			flowType = "clm";
		}
		
		log.info("Module -",moduleName);
		log.info("flowType -",flowType);
		List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository.findByEnvelopeId(body.getEnvelopeId());
		List<DocumentVersionDocument> documentVersionDocumentList = documentVersionRepository
				.findByEnvelopeId(body.getEnvelopeId());
		DocumentVersionDocument currentDocument = documentVersionDocumentList.stream()
				.max(Comparator.comparingInt(doc -> Integer.parseInt(doc.getVersionOrder().replace(".", ""))))
				.orElseThrow(() -> new DataValidationException("Document Version Document doesn't exists", "400",
						HttpStatus.BAD_REQUEST));
		String version = currentDocument.getVersionOrder();
		String matchingDocumentName = null;
		if (currentDocument.getDocuments() != null) {
			matchingDocumentName = currentDocument.getDocuments().stream()
					.filter(doc -> doc.getDocumentId().trim().equalsIgnoreCase(String.valueOf(body.getDocumentId())))
					.map(Document::getName).findFirst().orElseThrow(() -> new DataValidationException(
							"Version Document doesn't contain the provided documentId", "400", HttpStatus.BAD_REQUEST));
		} else {
			throw new DataValidationException("Version Document doesn't contains documents", "400",
					HttpStatus.BAD_REQUEST);
		}
		List<Comments> commentsList = currentDocument.getComments();
		if (commentsList == null)
			commentsList = new ArrayList<>();
		Comments comment = new Comments();
		comment.setCommentedBy(email);
		comment.setCommentedOn(new Date());
		comment.setMessage(body.getComment());
		comment.setDocumentId(body.getDocumentId() + "");
		comment.setReviewerName(name);
		commentsList.add(comment);
		currentDocument.setComments(commentsList);
		documentVersionRepository.save(currentDocument);
		try {
			PostCommentMailBody mailBody = new PostCommentMailBody(reviewerDocumentList.get(0).getCreatedBy(),
					reviewerDocumentList.get(0).getCreatorName(), matchingDocumentName,
					reviewerDocumentList.get(0).getContractName(), body.getComment(), flowType.toLowerCase(), body.getEnvelopeId(),
					moduleName, version);
			mailSenderService.sendCommentMail(mailBody);
			return new CommonResponse(HttpStatus.OK,
					new Response("Envelope Comment Response", "Successfully added comments"),
					"Document comment successfully");
		} catch (UnsupportedEncodingException | MessagingException e1) {
			e1.printStackTrace();
			return new CommonResponse(HttpStatus.OK, new Response("Envelope Comment Response", null),
					"Document comment Unsuccessfully");
		}
	}

	@Override
	public CommonResponse getCommentsList(String envelopeId,HttpServletRequest request) throws DataValidationException {

		String email = getEmailFromToken(request).getUserEmail();
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		List<DocumentVersionDocument> documentVersionDocumentList = documentVersionRepository
				.findByEnvelopeId(envelopeId);
		if (documentVersionDocumentList.isEmpty())
			throw new DataValidationException("Version Document not Found for specified envelopeId", envelopeId, null);
		CommentsListResponse response = new CommentsListResponse();
		List<CommentsList> responseList = new ArrayList<>();
		for (DocumentVersionDocument versionDoc : documentVersionDocumentList) {
			CommentsList list = new CommentsList();
			List<Comments> commentList = new ArrayList<>();
			list.setVersionOrder("v".concat(versionDoc.getVersionOrder()));
			list.setOrder(versionDoc.getDocVersion());
			if (versionDoc.getComments() != null) {
				for (Comments c : versionDoc.getComments()) {
					Document foundDocument = versionDoc.getDocuments().stream()
							.filter(doc -> c.getDocumentId().equals(doc.getDocumentId())).findFirst().orElse(null);
					Comments comment = new Comments(c.getMessage(), c.getCommentedBy(), c.getCommentedOn(),
							c.getDocumentId(), c.getReviewerName(),
							foundDocument != null ? foundDocument.getName() : null);
					commentList.add(comment);
					list.setComments(commentList);
					for (Document d : versionDoc.getDocuments()) {
						if (d.getDocumentId().equalsIgnoreCase(c.getDocumentId()))
							list.setDocumentName(d.getName());
					}
				}
				responseList.add(list);
			}
		}
		response.setCommentsList(responseList);
		return new CommonResponse(HttpStatus.OK, new Response("Comment List Response", response),
				"Comment List fetched Successfully");
	}

	public CommonResponse getListOfClmContractForCreate(HttpServletRequest request, int page, int limit,
			String searchText, String order, String orderBy) throws DataValidationException {

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
		List<WorkflowCreateResponse> createResponses = new ArrayList<>();
		Map<String, Object> commonResponse = new HashMap<>();
		String email = getEmailFromToken(request).getUserEmail();
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: null : null;
		log.info("Flow type: {}", flowType);
		long totalCount = 0;
		List<WorkFlowCreatorDocument> list;
		if (order == null) {
			totalCount = creatorDocumentRespository.findByEmail(email, flowType);
			list = creatorDocumentRespository.findByEmailAndFlowType(email, flowType, pageable);
		} else {
			if (orderBy.contains("subsidiary")) {
				orderBy = orderBy.replace("subsidiary", "");
			}
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			if (searchText != null && !searchText.isEmpty()) {
				totalCount = creatorDocumentRespository.findDocumentCountByField(orderBy, "^" + searchText, email,
						flowType, pageable, collation);
				list = creatorDocumentRespository.findByField(orderBy, "^" + searchText, email, flowType, pageable,
						collation);
			} else {
				totalCount = creatorDocumentRespository.findDocumentCountByField(orderBy, "^", email, flowType,
						pageable, collation);
				list = creatorDocumentRespository.findByField(orderBy, "^", email, flowType, pageable, collation);
			}
		}
		List<String> listOfEnvelopeIds = list.stream().map(document -> document.getEnvelopeId())
				.collect(Collectors.toList());
		List<LoaDocument> loaDocumentsList = null;
		List<LoaContractDocument> loaContractDocumentsList = null;
		List<CmuContractDocument> cmuDocumentsList = null;
		List<LooContractDocument> looDocumentsList = null;
		if (flowType.equalsIgnoreCase("LOA_create")) {
			loaDocumentsList = loaDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		}
		if (flowType.equalsIgnoreCase("CONTRACT_create")) {
			loaContractDocumentsList = loacontractDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		}
		if (flowType.equalsIgnoreCase("CMU_create")) {
			cmuDocumentsList = cmuDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		}
		if (flowType.equalsIgnoreCase("LOO_create")) {
			looDocumentsList = looDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		}
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeIdIn(listOfEnvelopeIds);
		for (WorkFlowCreatorDocument creator : list) {
			List<String> reviewers = new ArrayList<>();
			List<ReviewerDocument> reviewerslist = reviewerList.stream()
					.filter(document -> document.getEnvelopeId().equals(creator.getEnvelopeId()))
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
			if (loaDocumentsList != null) {
				LoaDocument loaDocument = loaDocumentsList.stream()
						.filter(document -> document.getEnvelopeId().equals(creator.getEnvelopeId())).findFirst()
						.orElse(null);
				WorkflowCreateResponse resp = WorkflowCreateResponse.builder().contractName(creator.getContractName())
						.createdOn(creator.getCreatedOn()).projectId(creator.getProjectId()).ownersEmail(email)
						.ownersName(getEmailFromToken(request).getUserName()).envelopeId(creator.getEnvelopeId())
						.status((loaDocument != null ? loaDocument.getStatus() : null)).pendingWith(reviewers)
						.tenantName(creator.getTenantName()).startDate(creator.getStartDate())
						.expiryDate(creator.getExpiryDate())
						.commencementDate(loaDocument != null ? loaDocument.getCommencementDate() : null)
						.completionDate(loaDocument != null ? loaDocument.getCompletionDate() : null)
						.subsidiary(loaDocument != null ? loaDocument.getSubsidiary() : null)
						.isFilesUploaded(loaDocument != null ? loaDocument.isFilesUploaded() : false).build();
				createResponses.add(resp);
			}
			if (loaContractDocumentsList != null) {
				LoaContractDocument loaContractDocument = loaContractDocumentsList.stream()
						.filter(document -> document.getEnvelopeId().equals(creator.getEnvelopeId())).findFirst()
						.orElse(null);
				WorkflowCreateResponse resp = WorkflowCreateResponse.builder().contractName(creator.getContractName())
						.createdOn(creator.getCreatedOn()).projectId(creator.getProjectId()).ownersEmail(email)
						.ownersName(getEmailFromToken(request).getUserName()).envelopeId(creator.getEnvelopeId())
						.status((loaContractDocument != null ? loaContractDocument.getStatus() : null))
						.pendingWith(reviewers)
						.commencementDate(
								loaContractDocument != null ? loaContractDocument.getCommencementDate() : null)
						.completionDate(loaContractDocument != null ? loaContractDocument.getCompletionDate() : null)
						.subsidiary(loaContractDocument != null ? loaContractDocument.getSubsidiary() : null).build();

				createResponses.add(resp);
			}

			if (cmuDocumentsList != null) {
				CmuContractDocument cmuDocument = cmuDocumentsList.stream()
						.filter(document -> document.getEnvelopeId().equals(creator.getEnvelopeId())).findFirst()
						.orElse(null);
				WorkflowCreateResponse resp = WorkflowCreateResponse.builder().contractName(creator.getContractName())
						.createdOn(creator.getCreatedOn()).projectId(creator.getProjectId()).ownersEmail(email)
						.ownersName(getEmailFromToken(request).getUserName()).envelopeId(creator.getEnvelopeId())
						.status((cmuDocument != null ? cmuDocument.getStatus() : null)).pendingWith(reviewers)
						.tenantName(creator.getTenantName()).startDate(creator.getStartDate())
						.expiryDate(creator.getExpiryDate())
						.commencementDate(cmuDocument != null ? cmuDocument.getCommencementDate() : null)
						.completionDate(cmuDocument != null ? cmuDocument.getCompletionDate() : null)
						.subsidiary(cmuDocument != null ? cmuDocument.getSubsidiary() : null).build();
				createResponses.add(resp);
			}

			if (looDocumentsList != null) {
				LooContractDocument looDocument = looDocumentsList.stream()
						.filter(document -> document.getEnvelopeId().equals(creator.getEnvelopeId())).findFirst()
						.orElse(null);
				WorkflowCreateResponse resp = WorkflowCreateResponse.builder().contractName(creator.getContractName())
						.createdOn(creator.getCreatedOn()).projectId(creator.getProjectId()).ownersEmail(email)
						.ownersName(getEmailFromToken(request).getUserName()).envelopeId(creator.getEnvelopeId())
						.status((looDocument != null ? looDocument.getStatus() : null)).pendingWith(reviewers)
						.tenantName(creator.getTenantName()).startDate(creator.getStartDate())
						.expiryDate(creator.getExpiryDate())
						.commencementDate(looDocument != null ? looDocument.getCommencementDate() : null)
						.completionDate(looDocument != null ? looDocument.getExpiryDate() : null)
						.subsidiary(looDocument != null ? looDocument.getSubsidiary() : null).build();
				createResponses.add(resp);
			}
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", createResponses);
		return new CommonResponse(HttpStatus.OK, new Response("CLMReviewerWorkflowResponse", commonResponse),
				"Workflow details fetched successfully");
	}

	@Override
	@Transactional
	public CommonResponse approveDocument(HttpServletRequest request, String envelopeId)
			throws DataValidationException, MessagingException, IOException, TemplateException {
		String email = getEmailFromToken(request).getUserEmail();
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		List<ReviewerDocument> revDocumentList = reviewerDocumentRepository.findByEnvelopeId(envelopeId);
		ReviewerDocument currDocument = reviewerDocumentRepository.findByEmailAndEnvelopeId(email, envelopeId);
		List<DocumentVersionDocument> documentLatestVersion = documentVersionRepository.findByEnvelopeId(envelopeId);
		DocumentVersionDocument latestDocumentVersion = documentLatestVersion.stream()
				.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion))
				.orElseThrow(() -> new NoSuchElementException("No document version found"));
		WorkFlowCreatorDocument workflowcreator = creatorDocumentRespository.findByEnvelopeId(envelopeId);
		workflowcreator.getPendingWith().removeIf(p -> p.equalsIgnoreCase(email));
		creatorDocumentRespository.save(workflowcreator);
		List<ReviewerDocument> excludeCurrentDocument = new ArrayList<>();
		for (ReviewerDocument e : revDocumentList) {
			if (!e.getEmail().equalsIgnoreCase(currDocument.getEmail()))
				excludeCurrentDocument.add(e);
		}
		boolean isSameOrder = revDocumentList.stream()
				.allMatch(e -> e.getRoutingOrder() == revDocumentList.get(0).getRoutingOrder());
		String documentRepoName=centralRepoDocumentRepository.findByEnvelopeId(envelopeId).getRepositoryName();
		if (isSameOrder) {
			currDocument.setEndDate(new Date());
			currDocument.setCompleted(true);
			reviewerDocumentRepository.save(currDocument);
//			if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//				List<LoaDocument> list = loaDocumentRepository.findByEnvelopeIds(envelopeId);
//				mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//						currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
//						Constant.LOA_MODULE);
//			}
//			if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//				mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//						currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
//						Constant.CONTRACT_MODULE);
//			}
			switch (documentRepoName) {
			case Constant.LOA_DOCUMENT_REPOSITORY:
				if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
				    	mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
							currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
							Constant.LOA_MODULE);
				}
				break;
			case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
				if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
					mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
							currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
							Constant.CONTRACT_MODULE);
				}
				break;
			case Constant.CMU_DOCUMENT_REPOSITORY:
				if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
					mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
							currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "commercial",
							Constant.CMU_MODULE);
				}
				break;
			case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
				if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
					mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
							currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "commercial",
							Constant.LOO_MODULE);
				}
				break;
			case Constant.TA_DOCUMENT_REPOSITORY:
				if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
						||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
					mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
							currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "commercial",
							Constant.TA_MODULE);
				  }
				break;
			}
			boolean allReviewed = excludeCurrentDocument.stream().allMatch(e -> e.getEndDate() != null);
			if (allReviewed) {
				try {
//					if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "LOA", "pcd",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						List<LoaDocument> list = loaDocumentRepository.findByEnvelopeIds(envelopeId);
//						for (LoaDocument loa : list) {
//							loa.setFlowCompleted(true);
//							loa.setStatus(Constant.SEND);
//							loaDocumentRepository.save(loa);
//						}
//					}
//					if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "CONTRACT", "pcd",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						LoaContractDocument contract = loacontractDocumentRepository.findByEnvelopeId(envelopeId);
//						contract.setIsFlowCompleted(true);
//						contract.setStatus(Constant.SEND);
//						loacontractDocumentRepository.save(contract);
//					}
//					if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "CMU", "commercial",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						CmuContractDocument cmuDocument = cmuDocumentRepository.findByEnvelopeId(envelopeId);
//						cmuDocument.setIsFlowCompleted(true);
//						cmuDocument.setStatus(Constant.COMPLETED);
//						cmuDocumentRepository.save(cmuDocument);
//					}
//					if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "LOO", "commercial",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						LooContractDocument looDocument = looDocumentRepository.findByEnvelopeId(envelopeId);
//						looDocument.setIsFlowCompleted(true);
//						looDocument.setStatus(Constant.SEND); //
//						looDocumentRepository.save(looDocument);
//					}
//					if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "TA", "commercial",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						TADocument taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
//						taDocument.setIsSignersCompleted("In-Progress");
//						taDocument.setIsCvefSignersCompleted("sent");
//						taDocument.setFlowCompleted(true);
//						taDocument.setStatus(Constant.SEND); 
//						taDocumentRepository.save(taDocument);
//					}
					switch (documentRepoName) {
					case Constant.LOA_DOCUMENT_REPOSITORY:
						if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.LOA_MODULE, "pcd",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							List<LoaDocument> list = loaDocumentRepository.findByEnvelopeIds(envelopeId);
							for (LoaDocument loa : list) {
								loa.setFlowCompleted(true);
							//	loa.setStatus(Constant.SEND);
								loaDocumentRepository.save(loa);
							}
						}
						break;
					case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
						if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.CONTRACT_MODULE, "pcd",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							LoaContractDocument contract = loacontractDocumentRepository.findByEnvelopeId(envelopeId);
							contract.setIsFlowCompleted(true);
							loacontractDocumentRepository.save(contract);
						}
						break;
					case Constant.CMU_DOCUMENT_REPOSITORY:
						if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.CMU_MODULE, "commercial",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							CmuContractDocument cmuDocument = cmuDocumentRepository.findByEnvelopeId(envelopeId);
							cmuDocument.setIsFlowCompleted(true);
							cmuDocument.setStatus(Constant.COMPLETED);
							cmuDocumentRepository.save(cmuDocument);
						}
						break;
					case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
						if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.LOO_MODULE, "commercial",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							LooContractDocument looDocument = looDocumentRepository.findByEnvelopeId(envelopeId);
							looDocument.setIsFlowCompleted(true);
							//	looDocument.setStatus(Constant.SEND);
							looDocumentRepository.save(looDocument);
						}
						break;
						
					case Constant.TA_DOCUMENT_REPOSITORY:
						if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.TA_MODULE, "commercial",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							TADocument taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
							taDocument.setIsSignersCompleted("In-Progress");
							taDocument.setIsCvefSignersCompleted("sent");
							taDocument.setFlowCompleted(true);
							//taDocument.setStatus(Constant.SEND); 
							taDocumentRepository.save(taDocument);
						}
						break;
					}
				} catch (Exception ex) {
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response("Document Approve Response", ex.getMessage()), "Document Approval Failed");
				}
			}
			return new CommonResponse(HttpStatus.OK, new Response("Document Approve Response", "Document Approved"),
					"Document Approval Successful");
		} else {
			boolean isLast = excludeCurrentDocument.stream()
					.allMatch(e -> (e.getRoutingOrder() < currDocument.getRoutingOrder()));
			if (isLast) {
				currDocument.setEndDate(new Date());
				currDocument.setCompleted(true);
				reviewerDocumentRepository.save(currDocument);
				try {
//					if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "LOA", "pcd",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//								currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
//								"pcd", Constant.LOA_MODULE);
//						List<LoaDocument> list = loaDocumentRepository.findByEnvelopeIds(envelopeId);
//						for (LoaDocument loa : list) {
//							loa.setFlowCompleted(true);
//							loa.setStatus(Constant.SEND);
//							loaDocumentRepository.save(loa);
//						}
//					}
//					if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "CONTRACT", "pcd",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//								currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
//								"pcd", Constant.CONTRACT_MODULE);
//						LoaContractDocument contract = loacontractDocumentRepository.findByEnvelopeId(envelopeId);
//						contract.setIsFlowCompleted(true);
//						contract.setStatus(Constant.SEND);
//						loacontractDocumentRepository.save(contract);
//					}
//					if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "CMU", "commercial",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//								currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
//								"commercial", Constant.CMU_MODULE);
//						CmuContractDocument cmuDocument = cmuDocumentRepository.findByEnvelopeId(envelopeId);
//						cmuDocument.setIsFlowCompleted(true);
//						cmuDocument.setStatus(Constant.COMPLETED);
//						log.info("status changed");
//						cmuDocumentRepository.save(cmuDocument);
//					}
//					if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "LOO", "commercial",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//								currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
//								"commercial", Constant.LOO_MODULE);
//						LooContractDocument looDocument = looDocumentRepository.findByEnvelopeId(envelopeId);
//						looDocument.setIsFlowCompleted(true);
//						looDocument.setStatus(Constant.SEND);
//						looDocumentRepository.save(looDocument);
//					}
//					if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//						mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
//								currDocument.getCreatorName(), currDocument.getContractName(), "TA", "commercial",
//								latestDocumentVersion.getVersionOrder(), envelopeId);
//						mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
//								currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
//								"commercial", Constant.TA_MODULE);
//						TADocument taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
//						taDocument.setFlowCompleted(true);
//						taDocument.setStatus(Constant.SEND); 
//						taDocumentRepository.save(taDocument);
//					}
//					return new CommonResponse(HttpStatus.OK,
//							new Response("Document Approve Response", "Document Approved"),
//							"Document Approval Successful");
					switch (documentRepoName) {
					case Constant.LOA_DOCUMENT_REPOSITORY:
						if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.LOA_MODULE, "pcd",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
									currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
									"pcd", Constant.LOA_MODULE);
							List<LoaDocument> list = loaDocumentRepository.findByEnvelopeIds(envelopeId);
							for (LoaDocument loa : list) {
								loa.setFlowCompleted(true);
								loaDocumentRepository.save(loa);
							}
						}
						break;
					case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
						if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.CONTRACT_MODULE, "pcd",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
									currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
									"pcd", Constant.CONTRACT_MODULE);
							LoaContractDocument contract = loacontractDocumentRepository.findByEnvelopeId(envelopeId);
							contract.setIsFlowCompleted(true);
							loacontractDocumentRepository.save(contract);
						}
						break;
					case Constant.CMU_DOCUMENT_REPOSITORY:
						if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.CMU_MODULE, "commercial",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
									currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
									"commercial", Constant.CMU_MODULE);
							CmuContractDocument cmuDocument = cmuDocumentRepository.findByEnvelopeId(envelopeId);
							cmuDocument.setIsFlowCompleted(true);
							cmuDocument.setStatus(Constant.COMPLETED);
							log.info("status changed");
							cmuDocumentRepository.save(cmuDocument);
						}
						break;
					case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
						if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(),Constant.LOO_MODULE, "commercial",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
									currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
									"commercial", Constant.LOO_MODULE);
							LooContractDocument looDocument = looDocumentRepository.findByEnvelopeId(envelopeId);
							looDocument.setIsFlowCompleted(true);
							looDocumentRepository.save(looDocument);
						}
						break;
						
					case Constant.TA_DOCUMENT_REPOSITORY:
						if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
								||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
							mailSenderService.sendMailToReviewer(email, currDocument.getCreatedBy(),
									currDocument.getCreatorName(), currDocument.getContractName(), Constant.TA_MODULE, "commercial",
									latestDocumentVersion.getVersionOrder(), envelopeId);
							mailSenderService.sendMailToCreator(currDocument.getCreatorName(), currDocument.getCreatedBy(),
									currDocument.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId,
									"commercial", Constant.TA_MODULE);
							TADocument taDocument = taDocumentRepository.findByEnvelopeId(envelopeId);
							taDocument.setFlowCompleted(true);
							taDocumentRepository.save(taDocument);
						}
						break;
					}
					return new CommonResponse(HttpStatus.OK,
							new Response("Document Approve Response", "Reviewer Document Approved"),
							"Document Approval Successful");
				} catch (Exception ex) {
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response("Document Approve Response", ex.getMessage()), "Document Approval Failed");
				}
			} else {
				ReviewerDocument nextReviewer = null;
				for (ReviewerDocument e : excludeCurrentDocument) {
					if (e.getRoutingOrder() == currDocument.getRoutingOrder() + 1)
						nextReviewer = e;
				}
				currDocument.setCompleted(true);
				currDocument.setEndDate(new Date());
				reviewerDocumentRepository.save(currDocument);
				if (nextReviewer == null)
					throw new DataValidationException("Reviewer with nextRoutingOrder not found", "400",
							HttpStatus.BAD_REQUEST);
				nextReviewer.setOrderFlag(true);
				reviewerDocumentRepository.save(nextReviewer);

//				if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
//							nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
//							Constant.LOA_MODULE);
//				}
//				if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
//							nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
//							Constant.CONTRACT_MODULE);
//				}
//				if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
//							nextReviewer.getReviewerName(), nextReviewer.getContractName(),
//							latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "pcd",
//							Constant.LOA_MODULE);
//				}
//				if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
//							nextReviewer.getReviewerName(), nextReviewer.getContractName(),
//							latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "pcd",
//							Constant.CONTRACT_MODULE);
//				}
//				if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
//							nextReviewer.getReviewerName(), nextReviewer.getContractName(),
//							latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "commercial",
//							Constant.CMU_MODULE);
//				}
//				if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
//							nextReviewer.getReviewerName(), nextReviewer.getContractName(),
//							latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "commercial",
//							Constant.LOO_MODULE);
//				}
//				if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())) {
//					mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
//							nextReviewer.getReviewerName(), nextReviewer.getContractName(),
//							latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "commercial",
//							Constant.TA_MODULE);
//				}
				switch (documentRepoName) {
				case Constant.LOA_DOCUMENT_REPOSITORY:
					if (Constant.LOA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
						mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
								nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
								Constant.LOA_MODULE);
						mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
								nextReviewer.getReviewerName(), nextReviewer.getContractName(),
								latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "pcd",
								Constant.LOA_MODULE);
					}
					break;
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					if (Constant.CONTRACT_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.PCD_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
						mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
								nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "pcd",
								Constant.CONTRACT_MODULE);
						mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
								nextReviewer.getReviewerName(), nextReviewer.getContractName(),
								latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "pcd",
								Constant.CONTRACT_MODULE);
					}
					break;
				case Constant.CMU_DOCUMENT_REPOSITORY:
					if (Constant.CMU_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
						
						mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
								nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "commercial",
								Constant.CMU_MODULE);
						mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
								nextReviewer.getReviewerName(), nextReviewer.getContractName(),
								latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "commercial",
								Constant.CMU_MODULE);
					}
					break;
				case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
					if (Constant.LOO_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())) {
						
						mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
								nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "commercial",
								Constant.LOO_MODULE);
						mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
								nextReviewer.getReviewerName(), nextReviewer.getContractName(),
								latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "commercial",
								Constant.LOO_MODULE);

					}
					break;
					
				case Constant.TA_DOCUMENT_REPOSITORY:
					if (Constant.TA_REVIEWER.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.COMMERCIAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.SUPER_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.LEGAL_ADMIN.equalsIgnoreCase(cacheValue.getDisplayname())
							||Constant.LEGAL_USER.equalsIgnoreCase(cacheValue.getDisplayname())) {
						mailSenderService.sendMailToCreator(nextReviewer.getCreatorName(), nextReviewer.getCreatedBy(),
								nextReviewer.getContractName(), latestDocumentVersion.getVersionOrder(), envelopeId, "commercial",
								Constant.TA_MODULE);
						mailSenderService.sendMailToNextReviewer(envelopeId, nextReviewer.getEmail(),
								nextReviewer.getReviewerName(), nextReviewer.getContractName(),
								latestDocumentVersion.getVersionOrder(), nextReviewer.getCreatedBy(), "commercial",
								Constant.TA_MODULE);
					}
					break;
				}
				return new CommonResponse(HttpStatus.OK,
						new Response("Document Approve Response", "Reviewer Document Approved"),
						"Document Approval Successful");
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
	public CommonResponse getListOfReviewedClmContract(HttpServletRequest request, int page, int limit,
			String searchText, String order, String orderBy, String status, String subsidiary, String customFlowType,String category)
			throws DataValidationException {
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
		List<ReviewerWorkflowListResponse> listResponse = new ArrayList<>();
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
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
		String flowType = (cacheValue != null) ? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()) : null;
		log.info("flowType:: {}", flowType);
		List<ReviewerDocument> reviewerList;
		Map<String, Object> commonResponse = new HashMap<>();
		long totalCount = 0;

		if (order == null) {
			totalCount = reviewerDocumentRepository.findReviewedDocumentCount(email, true, flowType);
			reviewerList = reviewerDocumentRepository.findReviewedDocument(email, true, flowType, pageable);
		} else {
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			Query query = new Query();
			query.collation(collation);
			if (cacheValue.getDisplayname().equalsIgnoreCase(Constant.PCD_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.SUPER_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.PCD_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_ADMIN)) {

				if (customFlowType != null) {
					flowType = AllRoleFlowMapper.getFlowType(customFlowType);
					query.addCriteria(Criteria.where("email").is(email));
					query.addCriteria(Criteria.where("flowType").is(flowType));
				} else {
					List<String> flowTypeList = new ArrayList<>();
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER));
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER));
					query.addCriteria(Criteria.where("flowType").in(flowTypeList));
				}
			} else if (cacheValue.getDisplayname().equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.SUPER_ADMIN)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.COMMERCIAL_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_USER)
					|| cacheValue.getDisplayname().equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				if (customFlowType != null) {
					flowType = AllRoleFlowMapper.getFlowType(customFlowType);
					query.addCriteria(Criteria.where("email").is(email));
					query.addCriteria(Criteria.where("flowType").is(flowType));
				} else {
					List<String> flowTypeList = new ArrayList<>();
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER));
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER));
					flowTypeList.add(AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER));
					query.addCriteria(Criteria.where("flowType").in(flowTypeList));
				}
			}
			else {
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			query.addCriteria(Criteria.where("isCompleted").is(true));
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
			if (searchText != null && !searchText.isEmpty()) {
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("contractName").regex(pattern),
						Criteria.where("projectId").regex(pattern), Criteria.where("contractNumber").regex(pattern));
				query.addCriteria(criteria);
			}
			log.info("Query {}", query);
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, ReviewerDocument.class);
			query.with(pageableObject);
			reviewerList = mongoTemplate.find(query, ReviewerDocument.class).stream().collect(Collectors.toList());
		}
		if (!reviewerList.isEmpty()) {
			List<String> listOfEnvelopeIds = reviewerList.stream().map(document -> document.getEnvelopeId())
					.collect(Collectors.toList());
			List<CentralRepoDocument> documentTypeList = centralRepoDocumentRepository
					.findByEnvelopeId(listOfEnvelopeIds);
			String repoName = documentTypeList.get(0).getRepositoryName();
			System.out.println(repoName);
			ReviewerWorkflowListResponse response = null;
			for (ReviewerDocument reviewers : reviewerList) {
				if (repoName.equalsIgnoreCase(Constant.LOA_DOCUMENT_REPOSITORY)
						|| repoName.equalsIgnoreCase(Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY)) {
					response = ReviewerWorkflowListResponse.builder().projectId(reviewers.getProjectId())
							.contractName(reviewers.getContractName()).envelopeId(reviewers.getEnvelopeId())
							.senderEmail(reviewers.getCreatedBy()).creatorName(reviewers.getCreatorName())
							.createdOn(reviewers.getCreatedOn()).status(reviewers.getStatus())
							.commencementDate(reviewers.getCommencementDate())
							.completionDate(reviewers.getCompletionDate()).subsidiary(reviewers.getSubsidiary())
							.contractNumber(reviewers.getContractNumber()).vendorName(reviewers.getVendorName())
							.build();
					listResponse.add(response);
				} else {
					response = ReviewerWorkflowListResponse.builder().projectId(reviewers.getProjectId())
							.contractName(reviewers.getContractName()).envelopeId(reviewers.getEnvelopeId())
							.senderEmail(reviewers.getCreatedBy()).creatorName(reviewers.getCreatorName())
							.createdOn(reviewers.getCreatedOn()).status(reviewers.getStatus())
							.commencementDate(reviewers.getCommencementDate()).expiryDate(reviewers.getExpiryDate())
							.completionDate(reviewers.getCompletionDate()).subsidiary(reviewers.getSubsidiary())
							.tenant(reviewers.getTenant()).build();
					listResponse.add(response);
				}
			}
		}
		commonResponse.put("total", totalCount);
		commonResponse.put("records", listResponse);
		return new CommonResponse(HttpStatus.OK, new Response("CLMReviewerWorkflowResponse", commonResponse),
				"Workflow details fetched successfully");
	}

	@Override
	public CommonResponse getApprovedLoaList(HttpServletRequest request, String subsidiary) {
		Map<String, List<ApprovedLoaListResponse>> responsese = new HashMap<>();
		subsidiary = subsidiary != null ? subsidiary.replaceAll("[()]", "\\\\$0") : "^";
		List<LoaDocument> approvedLoaList = loaDocumentRepository.findApprovedLoaList(true, true, subsidiary);
		List<ApprovedLoaListResponse> list = new ArrayList<>();
		for (LoaDocument loaDocument : approvedLoaList) {
			ApprovedLoaListResponse response = new ApprovedLoaListResponse();
			response.setEnvelopeId(loaDocument.getEnvelopeId());
			response.setProjectId(loaDocument.getProjectId());
			response.setProjectName(loaDocument.getProjectName());
			response.setCreatedOn(loaDocument.getCreatedOn());
			response.setSenderEmail(loaDocument.getSenderEmail());
			response.setSenderName(loaDocument.getSenderName());
			response.setStatus(loaDocument.isFlowCompleted());
			response.setSubsidiary(loaDocument.getSubsidiary());
			response.setContractNumber(loaDocument.getContractNumber());
			list.add(response);
		}
		responsese.put("records", list);
		return new CommonResponse(HttpStatus.OK, new Response("LoaApprovedListResponse", responsese),
				"LoaApproved list fetched successfully");
	}

	@Override
	public CommonResponse getReviewersList(HttpServletRequest request, String envelopeId) throws DataValidationException {
		
		String email=getEmailFromToken(request).getUserEmail();
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		List<ReviewerDocument> reviewerList = reviewerDocumentRepository.findByEnvelopeId(envelopeId);
		List<ReviewerListResponse> reviewersList = new ArrayList<>();
		for (ReviewerDocument reviewers : reviewerList) {
			ReviewerListResponse reviewerlist = new ReviewerListResponse();
			reviewerlist.setName(reviewers.getReviewerName());
			reviewerlist.setEmailAddress(reviewers.getEmail());
			reviewerlist.setStatus(reviewers.isCompleted());
			reviewerlist.setReviewedAt(reviewers.getEndDate());
			reviewerlist.setReviewingOrder(reviewers.getRoutingOrder());
			reviewersList.add(reviewerlist);
		}
		return new CommonResponse(HttpStatus.OK, new Response("ReviewerListResponse", reviewersList),
				"Reviewer list fetched successfully");
	}

	@Override
	public CommonResponse getApprovedLoaAllList(HttpServletRequest request, int page, int size, String searchText,
			String order, String orderBy, String status, String subsidiary) {
		Sort sort = null;
		PageRequest pageable = PageRequest.of(page, size);
		if (order != null && !order.isEmpty()) {
			Sort.Direction sortDirection = Sort.Direction.ASC;
			if (order.equalsIgnoreCase("desc")) {
				sortDirection = Sort.Direction.DESC;
			}
			sort = Sort.by(sortDirection, orderBy);
			pageable = PageRequest.of(page, size, sort);
		}
		long totalCount = 0;
		List<LoaDocument> list = null;
		if (order == null) {
			totalCount = loaDocumentRepository.countAllLoaList(true, true);
			list = loaDocumentRepository.findApprovedAllLoaList(true, true, pageable);
		} else {
			Collation collation = Collation.of("en").strength(Collation.ComparisonLevel.primary());
			Query query = new Query();
			query.collation(collation);
			query.addCriteria(Criteria.where("isFlowCompleted").is(true));
			query.addCriteria(Criteria.where("isCompleted").is(true));
			if (subsidiary != null && !subsidiary.isBlank()) {
				query.addCriteria(Criteria.where("subsidiary").regex(subsidiary.replaceAll("[()]", "\\\\$0"), "i"));
			}
			if (status != null && !status.isEmpty()) {
				query.addCriteria(Criteria.where("status").regex(status, "i"));
			}
			Criteria criteria = new Criteria();
			if (searchText != null && !searchText.isEmpty()) {
				Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
				criteria.orOperator(Criteria.where("projectName").regex(pattern),
						Criteria.where("contractNumber").regex(pattern), Criteria.where("projectId").regex(pattern));
				query.addCriteria(criteria);
			}
			Pageable pageableObject = pageable;
			totalCount = mongoTemplate.count(query, LoaDocument.class);
			query.with(pageableObject);
			list = mongoTemplate.find(query, LoaDocument.class).stream().collect(Collectors.toList());
		}
		List<LoaDocument> approvedLoaList = list;
		List<ApprovedLoaListResponse> approvedLoaListResponses = new ArrayList<>();
		for (LoaDocument loaDocument : approvedLoaList) {
			ApprovedLoaListResponse response = new ApprovedLoaListResponse();
			response.setEnvelopeId(loaDocument.getEnvelopeId());
			response.setProjectId(loaDocument.getProjectId());
			response.setProjectName(loaDocument.getProjectName());
			response.setCreatedOn(loaDocument.getCreatedOn());
			response.setSenderEmail(loaDocument.getSenderEmail());
			response.setSenderName(loaDocument.getSenderName());
			response.setStatus(loaDocument.isFlowCompleted());
			response.setCommencementDate(loaDocument.getCommencementDate());
			response.setCompletionDate(loaDocument.getCompletionDate());
			response.setSubsidiary(loaDocument.getSubsidiary());
			response.setContractNumber(loaDocument.getContractNumber());
			response.setVendorName(loaDocument.getVendorName());
			approvedLoaListResponses.add(response);
		}
		ApprovedLoaPagination data = new ApprovedLoaPagination(totalCount, approvedLoaListResponses);
		return new CommonResponse(HttpStatus.OK, new Response("LoaApprovedAllListResponse", data),
				"LoaApproved Alllist fetched successfully");
	}

	@Override
	public CommonResponse deleteLock(String envelopeId,HttpServletRequest request) throws DataValidationException {
		
		String email=getEmailFromToken(request).getUserEmail();
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
		if (!restrictionApi(usersInfoDocument, envelopeId))
			throw new DataValidationException("User Not Authorized", "403", HttpStatus.FORBIDDEN);
		String url = getDousignUrl().getDeleteEnvelopeLock().replace("{docusignHost}", docusignHost.trim());
		HttpHeaders headers = new HttpHeaders();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam("envelopeId", envelopeId);
		HttpEntity<?> requestEntity = new HttpEntity<>(headers);
		try {
			ResponseEntity<String> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.PUT,
					requestEntity, String.class);
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK, new Response("Delete Lock Response", "Envelope Locked"),
						"Document Lock Successful");
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete Lock Response", null),
					"Document Lock Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			if (ex instanceof ResourceAccessException) {
				return new CommonResponse(HttpStatus.NOT_FOUND, new Response("Delete Lock Response", new ArrayList<>()),
						"Unable to access remote resources.Please check the connectivity");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete Lock Response", new ArrayList<>()),
					ex.getLocalizedMessage());
		} catch (Exception ex) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete Lock Response", ex.getMessage()),
					"Document Lock Failed");
		}
		return null;
	}

	@Override
	public CommonResponse getDocumentVersionDocuments(String envelopeId, int docVersion)
			throws URISyntaxException, StorageException, DataValidationException {
		List<DocumentVersionDocument> documentList = documentVersionRepository.findByEnvelopeId(envelopeId);
		List<DocumentVersionDocumentResponse> response = new ArrayList<>();
		DocumentVersionDocument matchedDocument = documentList.stream().filter(e -> e.getDocVersion() == docVersion)
				.findFirst().orElseThrow(() -> new DataValidationException(
						"Version Document doesn't contain the provided documentId", "400", HttpStatus.BAD_REQUEST));
		if (matchedDocument.getDocuments() != null) {
			for (Document d : matchedDocument.getDocuments()) {
				DocumentVersionDocumentResponse versionResponse = new DocumentVersionDocumentResponse();
				versionResponse.setDocumentName(d.getName());
				CloudBlobContainer container = cloudBlobClient
						.getContainerReference(containerName.substring(0, containerName.lastIndexOf("/")));
				String path = matchedDocument.getPath();
				CloudBlockBlob blob = container.getBlockBlobReference(path + "/" + d.getDocumentId());
				versionResponse.setBlobUrl(blob.getUri().toString());
				versionResponse.setDocumentId(d.getDocumentId());
				response.add(versionResponse);
			}
			return new CommonResponse(HttpStatus.OK, new Response("Document Version Response", response),
					"Document Version details fetched Successfully");
		} else
			throw new DataValidationException("Provided envelop doesn't contain documents", "400",
					HttpStatus.BAD_REQUEST);

	}

	public boolean restrictionApi(UsersInfoDocument usersInfoDocument, String envelopeId) {
		CentralRepoDocument findRepo = centralRepoDocumentRepository.findByEnvelopeId(envelopeId);
		List<String> pcdEnvelope = Arrays.asList("LOA", "LOA_CONTRACT");
		List<String> commercialEnvelope = Arrays.asList("LOO", "CMU", "TA");
		String currentRole = usersInfoDocument!=null?usersInfoDocument.getCurrentRole():null;
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
