package saaspe.clm.serviceImpl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import saaspe.clm.constant.Constant;
import saaspe.clm.document.*;
import saaspe.clm.model.*;
import saaspe.clm.repository.LoaDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.DashboardService;
import saaspe.clm.utills.AllRoleFlowMapper;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DashboardServiceImpl implements DashboardService {

	private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

	@Autowired
	ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	LoaDocumentRepository documentRepository;

	@Autowired
	WorkFlowCreatorDocumentRespository creatorDocumentRespository;

	@Autowired
	UserInfoRespository userInfoRespository;

	@Autowired
	RedisUtility redisUtility;

	private final MongoTemplate mongoTemplate;

	public DashboardServiceImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public CommonResponse getDashboardView(HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		System.out.println(cacheValue);
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: (userInfo.getDivision() != null ? userInfo.getDivision() : null) : null;
		List<ReviewerWorkflowListResponse> listResponse = new ArrayList<>();
		if (flowType.equalsIgnoreCase("LOA_Review")) {
			List<ReviewerDocument> pendingReviewCount = reviewerDocumentRepository
					.findLatestDocumentsByEnvelopeAndEmail(email, true, flowType);
			long reviewedCount = reviewerDocumentRepository.findReviewedDocumentCount(email, true);
			long total = reviewerDocumentRepository.getTotalCount(email, flowType);
			Collections.sort(pendingReviewCount, Comparator.comparing(ReviewerDocument::getCreatedOn).reversed());
			List<ReviewerDocument> reviewerList = pendingReviewCount.subList(0,
					Math.min(pendingReviewCount.size(), 10));
			for (ReviewerDocument review : reviewerList) {
				ReviewerWorkflowListResponse reviewerLists = ReviewerWorkflowListResponse.builder()
						.contractName(review.getContractName()).envelopeId(review.getEnvelopeId())
						.senderEmail(review.getCreatedBy()).creatorName(review.getCreatorName())
						.createdOn(review.getCreatedOn()).build();
				listResponse.add(reviewerLists);
			}
			response.put("totalLoa", total);
			response.put("pendingCount", pendingReviewCount.size());
			response.put("reviewedCount", reviewedCount);
			response.put("reviewList", listResponse);
		}
		if (flowType.equalsIgnoreCase("LOA_Create")) {
			long totalLoa = creatorDocumentRespository.findByEmail(email, flowType);
			List<LoaDocument> allDocument = documentRepository.findBySenderEmail(email);
			response.put("totalLoa", totalLoa);
			response.put("signatureCompleted",
					allDocument.stream().filter(doc -> "completed".equals(doc.getStatus())).count());
			response.put("signaturePending", allDocument.stream()
					.filter(doc -> !"completed".equals(doc.getStatus()) && !"created".equals(doc.getStatus())).count());
			response.put("latestOnboarded", allDocument.subList(0, Math.min(allDocument.size(), 10)));
			response.put("expiringLoa", "");

		}
		return new CommonResponse(HttpStatus.OK, new Response("CLMReviewerWorkflowResponse", response),
				"Workflow details fetched successfully");
	}

	public CommonResponse getPcdLoaDashboardCount(HttpServletRequest request) {
		DashboardCountResponse dashboardCountResponse = new DashboardCountResponse();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		System.out.println(cacheValue);
		System.out.println(userInfo);
		String userType = (cacheValue != null) ? cacheValue.getDisplayname()
				: (userInfo.getCurrentRole() != null ? userInfo.getCurrentRole() : null);
		System.out.println("Usertype : " + userType);
		String flowType = null;
		Query query = new Query();
		long totalDocument = 0;
		long inProgressDocument = 0;
		long completedDocument = 0;
		long declinedDocument = 0;
		long voidedDocument = 0;
		long expiredDocument = 0;
		long createdDocument = 0;
		if (userType.equalsIgnoreCase(Constant.LOA_CREATOR) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_USER)) {
			totalDocument = mongoTemplate.count(query, LoaDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(4)));
			inProgressDocument = mongoTemplate.count(query, LoaDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(1)));
			createdDocument = mongoTemplate.count(query, LoaDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(0)));
			completedDocument = mongoTemplate.count(query, LoaDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
			declinedDocument = mongoTemplate.count(query, LoaDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(6)));
			voidedDocument = mongoTemplate.count(query, LoaDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(7)));
			expiredDocument = mongoTemplate.count(query, LoaDocument.class);
			dashboardCountResponse.setTotalDocument(totalDocument);
			dashboardCountResponse.setInProgressDocument(inProgressDocument);
			dashboardCountResponse.setCompletedDocument(completedDocument);
		}
		if (userType.equalsIgnoreCase(Constant.LOA_REVIEWER) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_USER)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			query = new Query();
			query.addCriteria(Criteria.where("isCompleted").is(false));
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
					|| userType.equalsIgnoreCase(Constant.PCD_USER)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER)));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			long pendingForReviewDocument = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setPendingForReviewDocument(pendingForReviewDocument);
			query = new Query();
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
					|| userType.equalsIgnoreCase(Constant.PCD_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER)));
				query.addCriteria(Criteria.where("isCompleted").is(true));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
				query.addCriteria(Criteria.where("isCompleted").is(true));
			}
			log.info("Query reviewer criteria: {}", query);
			long reviewedDocument = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setReviewed(reviewedDocument);
			query = new Query();
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
					|| userType.equalsIgnoreCase(Constant.PCD_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER)));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.LOA_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			long totalReview = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setTotalReview(totalReview);

		}
		ChartDataResponse chartDataResponse = new ChartDataResponse(completedDocument, declinedDocument,
				createdDocument, voidedDocument, inProgressDocument, expiredDocument);
		dashboardCountResponse.setChartData(chartDataResponse);
		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", dashboardCountResponse),
				"PCD Dashboard details fetched successfully");
	}

	public CommonResponse getPcdContractDashboardCount(HttpServletRequest request) {
		DashboardCountResponse dashboardCountResponse = new DashboardCountResponse();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		System.out.println(cacheValue);
		String userType = (cacheValue != null) ? cacheValue.getDisplayname()
				: (userInfo.getCurrentRole() != null ? userInfo.getCurrentRole() : null);
		System.out.println("Usertype : " + userType);
		String flowType = null;
		Query query = new Query();
		long totalDocument = 0;
		long inProgressDocument = 0;
		long completedDocument = 0;
		long declinedDocument = 0;
		long voidedDocument = 0;
		long expiredDocument = 0;
		long createdDocument = 0;
		if (userType.equalsIgnoreCase(Constant.CONTRACT_CREATOR) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_USER)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			totalDocument = mongoTemplate.count(query, LoaContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(4)));
			inProgressDocument = mongoTemplate.count(query, LoaContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(1)));
			createdDocument = mongoTemplate.count(query, LoaContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(0)));
			completedDocument = mongoTemplate.count(query, LoaContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
			declinedDocument = mongoTemplate.count(query, LoaContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(6)));
			voidedDocument = mongoTemplate.count(query, LoaContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(7)));
			expiredDocument = mongoTemplate.count(query, LoaContractDocument.class);
			dashboardCountResponse.setTotalDocument(totalDocument);
			dashboardCountResponse.setInProgressDocument(inProgressDocument);
			dashboardCountResponse.setCompletedDocument(completedDocument);
		}

		if (userType.equalsIgnoreCase(Constant.CONTRACT_REVIEWER) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			query = new Query();
			query.addCriteria(Criteria.where("isCompleted").is(false));
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER)));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			long pendingForReviewDocument = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setPendingForReviewDocument(pendingForReviewDocument);
			query = new Query();
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
					|| userType.equalsIgnoreCase(Constant.PCD_USER)
					|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER)));
				query.addCriteria(Criteria.where("isCompleted").is(true));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
				query.addCriteria(Criteria.where("isCompleted").is(true));
			}
			log.info("Query reviewer criteria: {}", query);
			long reviewedDocument = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setReviewed(reviewedDocument);
			query = new Query();
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.PCD_ADMIN)
					|| userType.equalsIgnoreCase(Constant.PCD_USER)
					|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER)));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.CONTRACT_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			long totalReview = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setTotalReview(totalReview);

		}
		ChartDataResponse chartDataResponse = new ChartDataResponse(completedDocument, declinedDocument,
				createdDocument, voidedDocument, inProgressDocument, expiredDocument);
		dashboardCountResponse.setChartData(chartDataResponse);
		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", dashboardCountResponse),
				"PCD Dashboard details fetched successfully");
	}

	public CommonResponse getLoaLatestOnboardedDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").nin("declined", "expired", "voided"));
		query.fields().include("projectName", "commencementDate", "status", "envelopeId", "completionDate");
		List<LoaDocument> list = new ArrayList<>(mongoTemplate.find(query, LoaDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (LoaDocument document : list) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setContractName(document.getProjectName());
			dashboardPcdResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(document.getCommencementDate());
			dashboardPcdResponse.setStatus(document.getStatus());
			dashboardPcdResponse.setCompletionDate(document.getCompletionDate());
			dashboardPcdResponseList.add(dashboardPcdResponse);

		}
		response.setResponse(new Response("Latest Onboarded Loa list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Onboarded Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getLoaExpiringDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "completionDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.addCriteria(Criteria.where("status").is("completed"));
		query.with(pageRequest);
		query.fields().include("projectName", "commencementDate", "status", "envelopeId", "completionDate");
		log.info("query {}: ", query);
		List<LoaDocument> list = new ArrayList<>(mongoTemplate.find(query, LoaDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (LoaDocument document : list) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setContractName(document.getProjectName());
			dashboardPcdResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(document.getCommencementDate());
			dashboardPcdResponse.setStatus(document.getStatus());
			dashboardPcdResponse.setCompletionDate(document.getCompletionDate());
			dashboardPcdResponseList.add(dashboardPcdResponse);
		}
		response.setResponse(new Response("Expiring Loa list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Expiring Document has been retrieved successfully");
		return response;
	}

	@Override
	public CommonResponse getPcdPendingForReviewDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
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
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: null : null;
		List<ReviewerDocument> reviewerList;
		Query query = new Query();
		query.addCriteria(Criteria.where("email").is(email));
		query.addCriteria(Criteria.where("flowType").is(flowType));
		query.addCriteria(Criteria.where("isCompleted").is(false));
		Sort sort = Sort.by(Sort.Direction.DESC, "completionDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		reviewerList = new ArrayList<>(mongoTemplate.find(query, ReviewerDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (ReviewerDocument reviewerDocument : reviewerList) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setProjectId(reviewerDocument.getProjectId());
			dashboardPcdResponse.setContractName(reviewerDocument.getContractName());
			dashboardPcdResponse.setEnvelopeId(reviewerDocument.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(reviewerDocument.getCommencementDate());
			dashboardPcdResponse.setStatus(reviewerDocument.getStatus());
			dashboardPcdResponse.setOwnerName(reviewerDocument.getCreatorName());
			dashboardPcdResponse.setCommencementDate(reviewerDocument.getCommencementDate());
			dashboardPcdResponse.setCompletionDate(reviewerDocument.getCompletionDate());
			dashboardPcdResponse.setSubsidiary(reviewerDocument.getSubsidiary());
			dashboardPcdResponse.setOwnerEmail(reviewerDocument.getEmail());
			dashboardPcdResponseList.add(dashboardPcdResponse);
		}
		response.setResponse(new Response("Pending Loa list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Pending Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getContractLatestOnboardedDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").nin("declined", "expired", "voided"));
		query.fields().include("projectName", "commencementDate", "status", "envelopeId", "completionDate");
		List<LoaContractDocument> list = new ArrayList<>(mongoTemplate.find(query, LoaContractDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (LoaContractDocument document : list) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setContractName(document.getProjectName());
			dashboardPcdResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(document.getCommencementDate());
			dashboardPcdResponse.setStatus(document.getStatus());
			dashboardPcdResponse.setCompletionDate(document.getCompletionDate());
			dashboardPcdResponseList.add(dashboardPcdResponse);
		}
		response.setResponse(new Response("Latest Onboarded Contract list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Onboarded Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getContractExpiringDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "completionDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.addCriteria(Criteria.where("status").is("completed"));
		query.with(pageRequest);
		query.fields().include("projectName", "commencementDate", "status", "envelopeId", "completionDate");
		List<LoaContractDocument> list = new ArrayList<>(mongoTemplate.find(query, LoaContractDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (LoaContractDocument document : list) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setContractName(document.getProjectName());
			dashboardPcdResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(document.getCommencementDate());
			dashboardPcdResponse.setStatus(document.getStatus());
			dashboardPcdResponse.setCompletionDate(document.getCompletionDate());
			dashboardPcdResponseList.add(dashboardPcdResponse);

		}
		response.setResponse(new Response("Expiring Contract list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Expiring Contract Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getCmuLatestOnboardedDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").nin("declined", "expired", "voided"));
		query.fields().include("contractTitle", "commencementDate", "status", "envelopeId");
		List<CmuContractDocument> list = new ArrayList<>(mongoTemplate.find(query, CmuContractDocument.class));
		List<DashboardCommercialResponse> dashboardCommercialResponsesList = new ArrayList<>();
		for (CmuContractDocument document : list) {
			DashboardCommercialResponse dashboardCommercialResponse = new DashboardCommercialResponse();
			dashboardCommercialResponse.setContractTitle(document.getContractTitle());
			dashboardCommercialResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardCommercialResponse.setCommencementDate(document.getCommencementDate());
			dashboardCommercialResponse.setStatus(document.getStatus());
			dashboardCommercialResponse.setExpiryDate(document.getExpiryDate());
			dashboardCommercialResponsesList.add(dashboardCommercialResponse);
		}
		response.setResponse(new Response("Latest Onboarded Contract list", dashboardCommercialResponsesList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Onboarded Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getCmuExpiringDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "expiryDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.addCriteria(Criteria.where("status").is("completed"));
		query.with(pageRequest);
		query.fields().include("contractTitle", "commencementDate", "status", "envelopeId", "expiryDate");
		List<CmuContractDocument> list = new ArrayList<>(mongoTemplate.find(query, CmuContractDocument.class));
		List<DashboardCommercialResponse> dashboardCommercialResponsesList = new ArrayList<>();
		for (CmuContractDocument document : list) {
			DashboardCommercialResponse dashboardResponse = new DashboardCommercialResponse();
			dashboardResponse.setContractTitle(document.getContractTitle());
			dashboardResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardResponse.setCommencementDate(document.getCommencementDate());
			dashboardResponse.setStatus(document.getStatus());
			dashboardResponse.setExpiryDate(document.getExpiryDate());
			dashboardCommercialResponsesList.add(dashboardResponse);
		}
		response.setResponse(new Response("Expiring Cmu list", dashboardCommercialResponsesList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Expiring Cmu Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getLooLatestOnboardedDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").nin("declined", "expired", "voided"));
		query.fields().include("contractTitle", "commencementDate", "status", "envelopeId");
		List<LooContractDocument> list = new ArrayList<>(mongoTemplate.find(query, LooContractDocument.class));
		List<DashboardCommercialResponse> dashboardPcdResponseList = new ArrayList<>();
		for (LooContractDocument document : list) {
			DashboardCommercialResponse dashboardCommercialResponse = new DashboardCommercialResponse();
			dashboardCommercialResponse.setContractTitle(document.getContractTitle());
			dashboardCommercialResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardCommercialResponse.setCommencementDate(document.getCommencementDate());
			dashboardCommercialResponse.setStatus(document.getStatus());
			dashboardCommercialResponse.setExpiryDate(document.getExpiryDate());
			dashboardPcdResponseList.add(dashboardCommercialResponse);
		}
		response.setResponse(new Response("Latest Onboarded Contract list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Onboarded Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getLooExpiringDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "expiryDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.addCriteria(Criteria.where("status").is("completed"));
		query.with(pageRequest);
		query.fields().include("contractTitle", "commencementDate", "status", "envelopeId", "expiryDate");
		List<LooContractDocument> list = new ArrayList<>(mongoTemplate.find(query, LooContractDocument.class));
		List<DashboardCommercialResponse> dashboardCommercialResponsesList = new ArrayList<>();
		for (LooContractDocument document : list) {
			DashboardCommercialResponse dashboardResponse = new DashboardCommercialResponse();
			dashboardResponse.setContractTitle(document.getContractTitle());
			dashboardResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardResponse.setCommencementDate(document.getCommencementDate());
			dashboardResponse.setStatus(document.getStatus());
			dashboardResponse.setExpiryDate(document.getExpiryDate());
			dashboardCommercialResponsesList.add(dashboardResponse);
		}
		response.setResponse(new Response("Expiring Loo list", dashboardCommercialResponsesList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("LExpiring Loo Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getTaLatestOnboardedDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").nin("declined", "expired", "voided"));
		query.fields().include("contractTitle", "commencementDate", "status", "envelopeId");
		List<TADocument> list = new ArrayList<>(mongoTemplate.find(query, TADocument.class));
		List<DashboardCommercialResponse> dashboardPcdResponseList = new ArrayList<>();
		for (TADocument document : list) {
			DashboardCommercialResponse dashboardCommercialResponse = new DashboardCommercialResponse();
			dashboardCommercialResponse.setContractTitle(document.getContractTitle());
			dashboardCommercialResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardCommercialResponse.setCommencementDate(document.getCommencementDate());
			dashboardCommercialResponse.setStatus(document.getStatus());
			dashboardCommercialResponse.setExpiryDate(document.getExpiryDate());
			dashboardPcdResponseList.add(dashboardCommercialResponse);
		}
		response.setResponse(new Response("Latest Onboarded Contract list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Onboarded Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getTaExpiringDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "expiryDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.addCriteria(Criteria.where("status").is("completed"));
		query.with(pageRequest);
		query.fields().include("contractTitle", "commencementDate", "status", "envelopeId", "expiryDate");
		List<TADocument> list = new ArrayList<>(mongoTemplate.find(query, TADocument.class));
		List<DashboardCommercialResponse> dashboardCommercialResponsesList = new ArrayList<>();
		for (TADocument document : list) {
			DashboardCommercialResponse dashboardResponse = new DashboardCommercialResponse();
			dashboardResponse.setContractTitle(document.getContractTitle());
			dashboardResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardResponse.setCommencementDate(document.getCommencementDate());
			dashboardResponse.setStatus(document.getStatus());
			dashboardResponse.setExpiryDate(document.getExpiryDate());
			dashboardCommercialResponsesList.add(dashboardResponse);
		}
		response.setResponse(new Response("Expiring Ta list", dashboardCommercialResponsesList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Expiring Ta Document has been retrieved successfully");
		return response;
	}

	@Override
	public CommonResponse getCommercialPendingForReviewDashboardList(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: (userInfo.getDivision() != null ? userInfo.getDivision() : null) : null;
		List<ReviewerDocument> reviewerList;
		Query query = new Query();
		// if admin need access the condition need to be changed
		query.addCriteria(Criteria.where("email").is(email));
		query.addCriteria(Criteria.where("flowType").is(flowType));
		query.addCriteria(Criteria.where("isCompleted").is(false));
		Sort sort = Sort.by(Sort.Direction.DESC, "completionDate");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		reviewerList = new ArrayList<>(mongoTemplate.find(query, ReviewerDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (ReviewerDocument reviewerDocument : reviewerList) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setProjectId(reviewerDocument.getProjectId());
			dashboardPcdResponse.setContractName(reviewerDocument.getContractName());
			dashboardPcdResponse.setEnvelopeId(reviewerDocument.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(reviewerDocument.getCommencementDate());
			dashboardPcdResponse.setStatus(reviewerDocument.getStatus());
			dashboardPcdResponse.setOwnerName(reviewerDocument.getCreatorName());
			dashboardPcdResponse.setCommencementDate(reviewerDocument.getCommencementDate());
			dashboardPcdResponse.setCompletionDate(reviewerDocument.getCompletionDate());
			dashboardPcdResponse.setSubsidiary(reviewerDocument.getSubsidiary());
			dashboardPcdResponse.setOwnerEmail(reviewerDocument.getEmail());
			dashboardPcdResponseList.add(dashboardPcdResponse);
		}
		response.setResponse(new Response("Pending Loa list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Pending Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getCommercialCmuDashboardCount(HttpServletRequest request) {
		DashboardCountResponse dashboardCountResponse = new DashboardCountResponse();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		System.out.println(cacheValue);
		String userType = (cacheValue != null) ? cacheValue.getDisplayname()
				: (userInfo.getCurrentRole() != null ? userInfo.getCurrentRole() : null);
		System.out.println("Usertype : " + userType);
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: (userInfo.getDivision() != null ? userInfo.getDivision() : null) : null;
		Query query = new Query();
		long totalDocument = 0;
		long inProgressDocument = 0;
		long completedDocument = 0;
		long declinedDocument = 0;
		long voidedDocument = 0;
		long expiredDocument = 0;
		long createdDocument = 0;
		if (userType.equalsIgnoreCase(Constant.CMU_CREATOR) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			totalDocument = mongoTemplate.count(query, CmuContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(4)));
			inProgressDocument = mongoTemplate.count(query, CmuContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(1)));
			createdDocument = mongoTemplate.count(query, CmuContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(0)));
			completedDocument = mongoTemplate.count(query, CmuContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
			declinedDocument = mongoTemplate.count(query, CmuContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(6)));
			voidedDocument = mongoTemplate.count(query, CmuContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(7)));
			expiredDocument = mongoTemplate.count(query, CmuContractDocument.class);
			dashboardCountResponse.setTotalDocument(totalDocument);
			dashboardCountResponse.setInProgressDocument(inProgressDocument);
			dashboardCountResponse.setCompletedDocument(completedDocument);
		}

		if (userType.equalsIgnoreCase(Constant.CMU_REVIEWER) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			query = new Query();
			query.addCriteria(Criteria.where("isCompleted").is(false));
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
					|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
					|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER)));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			long pendingForReviewDocument = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setPendingForReviewDocument(pendingForReviewDocument);
			query = new Query();
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
					|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
					|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER)));
				query.addCriteria(Criteria.where("isCompleted").is(true));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
				query.addCriteria(Criteria.where("isCompleted").is(true));
			}
			log.info("Query reviewer criteria: {}", query);
			long reviewedDocument = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setReviewed(reviewedDocument);
			query = new Query();
			if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
					|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
					|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
				query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER)));
			} else {
				flowType = AllRoleFlowMapper.getFlowType(Constant.CMU_REVIEWER);
				query.addCriteria(Criteria.where("email").is(email));
				query.addCriteria(Criteria.where("flowType").is(flowType));
			}
			long totalReview = mongoTemplate.count(query, ReviewerDocument.class);
			dashboardCountResponse.setTotalReview(totalReview);
		}
		ChartDataResponse chartDataResponse = new ChartDataResponse(completedDocument, declinedDocument,
				createdDocument, voidedDocument, inProgressDocument, expiredDocument);
		dashboardCountResponse.setChartData(chartDataResponse);
		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", dashboardCountResponse),
				"PCD Dashboard details fetched successfully");

	}

	public CommonResponse getCommercialLooDashboardCount(HttpServletRequest request) {
		DashboardCountResponse dashboardCountResponse = new DashboardCountResponse();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		System.out.println(cacheValue);
		String userType = (cacheValue != null) ? cacheValue.getDisplayname()
				: (userInfo.getCurrentRole() != null ? userInfo.getCurrentRole() : null);
		System.out.println("Usertype : " + userType);
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: (userInfo.getDivision() != null ? userInfo.getDivision() : null) : null;
		Query query = new Query();
		long totalDocument = 0;
		long inProgressDocument = 0;
		long completedDocument = 0;
		long declinedDocument = 0;
		long voidedDocument = 0;
		long expiredDocument = 0;
		long createdDocument = 0;
		if (userType.equalsIgnoreCase(Constant.LOO_CREATOR) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			totalDocument = mongoTemplate.count(query, LooContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(4)));
			inProgressDocument = mongoTemplate.count(query, LooContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(1)));
			createdDocument = mongoTemplate.count(query, LooContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(0)));
			completedDocument = mongoTemplate.count(query, LooContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
			declinedDocument = mongoTemplate.count(query, LooContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(6)));
			voidedDocument = mongoTemplate.count(query, LooContractDocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(7)));
			expiredDocument = mongoTemplate.count(query, LooContractDocument.class);
			dashboardCountResponse.setTotalDocument(totalDocument);
			dashboardCountResponse.setInProgressDocument(inProgressDocument);
			dashboardCountResponse.setCompletedDocument(completedDocument);
		}

		if (userType.equalsIgnoreCase(Constant.LOO_REVIEWER) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			   query = new Query();
               query.addCriteria(Criteria.where("isCompleted").is(false));
               if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)) {
            		query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER)));
               }
               else {
                   flowType = AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER);
                   query.addCriteria(Criteria.where("email").is(email));
                   query.addCriteria(Criteria.where("flowType").is(flowType));
               }
               long pendingForReviewDocument = mongoTemplate.count(query, ReviewerDocument.class);
               dashboardCountResponse.setPendingForReviewDocument(pendingForReviewDocument);
               query = new Query();
               if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
            		   || userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
           		query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER)));
                   query.addCriteria(Criteria.where("isCompleted").is(true));
               }
               else {
                   flowType = AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER);
                   query.addCriteria(Criteria.where("email").is(email));
                   query.addCriteria(Criteria.where("flowType").is(flowType));
                   query.addCriteria(Criteria.where("isCompleted").is(true));
               }
               log.info("Query reviewer criteria: {}", query);
               long reviewedDocument = mongoTemplate.count(query, ReviewerDocument.class);
               dashboardCountResponse.setReviewed(reviewedDocument);
               query = new Query();
               if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
            		   || userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
           		query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER)));
               }
               else {
                   flowType = AllRoleFlowMapper.getFlowType(Constant.LOO_REVIEWER);
                   query.addCriteria(Criteria.where("email").is(email));
                   query.addCriteria(Criteria.where("flowType").is(flowType));
               }
               long totalReview = mongoTemplate.count(query, ReviewerDocument.class);
               dashboardCountResponse.setTotalReview(totalReview);
	
			
			
		}
		ChartDataResponse chartDataResponse = new ChartDataResponse(completedDocument, declinedDocument,
				createdDocument, voidedDocument, inProgressDocument, expiredDocument);
		dashboardCountResponse.setChartData(chartDataResponse);
		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", dashboardCountResponse),
				"PCD Dashboard details fetched successfully");
	}

	public CommonResponse getCommercialTaDashboardCount(HttpServletRequest request) {
		DashboardCountResponse dashboardCountResponse = new DashboardCountResponse();
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
		UsersInfoDocument userInfo = userInfoRespository.findByEmailAndActive(email, true);
		System.out.println(cacheValue);
		String userType = (cacheValue != null) ? cacheValue.getDisplayname()
				: (userInfo.getCurrentRole() != null ? userInfo.getCurrentRole() : null);
		System.out.println("Usertype : " + userType);
		String flowType = (cacheValue != null) ? !AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname()).isBlank()
				? AllRoleFlowMapper.getFlowType(cacheValue.getDisplayname())
				: (userInfo.getDivision() != null ? userInfo.getDivision() : null) : null;
		Query query = new Query();
		long totalDocument = 0;
		long inProgressDocument = 0;
		long completedDocument = 0;
		long declinedDocument = 0;
		long voidedDocument = 0;
		long expiredDocument = 0;
		long createdDocument = 0;
		if (userType.equalsIgnoreCase(Constant.LOO_CREATOR) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			totalDocument = mongoTemplate.count(query, TADocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(4)));
			inProgressDocument = mongoTemplate.count(query, TADocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(1)));
			createdDocument = mongoTemplate.count(query, TADocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(0)));
			completedDocument = mongoTemplate.count(query, TADocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
			declinedDocument = mongoTemplate.count(query, TADocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(6)));
			voidedDocument = mongoTemplate.count(query, TADocument.class);
			query = new Query();
			query.addCriteria(Criteria.where("status").is(Constant.CLM_ENVELOPE_STATUS.get(7)));
			expiredDocument = mongoTemplate.count(query, TADocument.class);
			dashboardCountResponse.setTotalDocument(totalDocument);
			dashboardCountResponse.setInProgressDocument(inProgressDocument);
			dashboardCountResponse.setCompletedDocument(completedDocument);
		}
		if (userType.equalsIgnoreCase(Constant.TA_REVIEWER) || userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)
				|| userType.equalsIgnoreCase(Constant.SUPER_ADMIN)
				|| userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
			   query = new Query();
               query.addCriteria(Criteria.where("isCompleted").is(false));
               if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
            		   || userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
           		query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER)));
               }
               else {
                   flowType = AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER);
                   query.addCriteria(Criteria.where("email").is(email));
                   query.addCriteria(Criteria.where("flowType").is(flowType));
               }
               long pendingForReviewDocument = mongoTemplate.count(query, ReviewerDocument.class);
               dashboardCountResponse.setPendingForReviewDocument(pendingForReviewDocument);
               query = new Query();
               if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
            		   || userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
           		query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER)));
                   query.addCriteria(Criteria.where("isCompleted").is(true));
               }
               else {
                   flowType = AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER);
                   query.addCriteria(Criteria.where("email").is(email));
                   query.addCriteria(Criteria.where("flowType").is(flowType));
                   query.addCriteria(Criteria.where("isCompleted").is(true));
               }
               log.info("Query reviewer criteria: {}", query);
               long reviewedDocument = mongoTemplate.count(query, ReviewerDocument.class);
               dashboardCountResponse.setReviewed(reviewedDocument);
               query = new Query();
               if (userType.equalsIgnoreCase(Constant.SUPER_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN)||userType.equalsIgnoreCase(Constant.COMMERCIAL_USER)
            		   || userType.equalsIgnoreCase(Constant.LEGAL_USER)|| userType.equalsIgnoreCase(Constant.LEGAL_ADMIN)) {
           		query.addCriteria(Criteria.where("flowType").is(AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER)));
               }
               else {
                   flowType = AllRoleFlowMapper.getFlowType(Constant.TA_REVIEWER);
                   query.addCriteria(Criteria.where("email").is(email));
                   query.addCriteria(Criteria.where("flowType").is(flowType));
               }
               long totalReview = mongoTemplate.count(query, ReviewerDocument.class);
               dashboardCountResponse.setTotalReview(totalReview);
		}
		ChartDataResponse chartDataResponse = new ChartDataResponse(completedDocument, declinedDocument,
				createdDocument, voidedDocument, inProgressDocument, expiredDocument);
		dashboardCountResponse.setChartData(chartDataResponse);
		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", dashboardCountResponse),
				"PCD Dashboard details fetched successfully");
	}

	public CommonResponse getLegalContractDashboardCount(HttpServletRequest request,String category,String subsidiary) {
		LegalDashboardCountResponse legalDashboardCountResponse = new LegalDashboardCountResponse();
		Query query = new Query();
		long totalDocument = 0;
		long completedCvef = 0;
		long rejectedCvef = 0;
		long signaturePendingCvef = 0;
		long stampedContract = 0;
		long stamperPendingContract = 0;
		long lhdnCompletedContract = 0;
		long lhdnPendingContract = 0;
		totalDocument = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
			query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isCvefSignersCompleted").is(Constant.COMPLETED));
		completedCvef = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
			query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isCvefSignersCompleted").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
		rejectedCvef = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
			query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isCvefSignersCompleted").is(Constant.IN_PROGRESS));
		signaturePendingCvef = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isStampersCompleted").is(Constant.COMPLETED));
		stampedContract = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isStampersCompleted").is(Constant.IN_PROGRESS));
		stamperPendingContract = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isLhdnSignersCompleted").is(Constant.COMPLETED));
		lhdnCompletedContract = mongoTemplate.count(query, LoaContractDocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isLhdnSignersCompleted").is(Constant.IN_PROGRESS));
		lhdnPendingContract = mongoTemplate.count(query, LoaContractDocument.class);

		legalDashboardCountResponse.setTotalDocument(totalDocument);
		legalDashboardCountResponse.setCompletedCvef(completedCvef);
		legalDashboardCountResponse.setRejectedCvef(rejectedCvef);
		legalDashboardCountResponse.setSignaturePendingCvef(signaturePendingCvef);
		legalDashboardCountResponse.setStampedContract(stampedContract);
		legalDashboardCountResponse.setStamperPendingContract(stamperPendingContract);
		legalDashboardCountResponse.setLhdnCompletedContract(lhdnCompletedContract);
		legalDashboardCountResponse.setLhdnPendingContract(lhdnPendingContract);

		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", legalDashboardCountResponse),
				"Legal Dashboard details fetched successfully");
	}

	public CommonResponse getLegalTaDashboardCount(HttpServletRequest request,String category,String subsidiary) {
		LegalDashboardCountResponse legalDashboardCountResponse = new LegalDashboardCountResponse();
		Query query = new Query();
		long totalDocument = 0;
		long completedCvef = 0;
		long rejectedCvef = 0;
		long signaturePendingCvef = 0;
		long stampedContract = 0;
		long stamperPendingContract = 0;
		long lhdnCompletedContract = 0;
		long lhdnPendingContract = 0;
		totalDocument = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
			query.addCriteria(Criteria.where("category").is(category));
			}
		query.addCriteria(Criteria.where("isCvefSignersCompleted").is(Constant.COMPLETED));
		completedCvef = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isCvefSignersCompleted").is(Constant.CLM_ENVELOPE_STATUS.get(2)));
		rejectedCvef = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isCvefSignersCompleted").is(Constant.IN_PROGRESS));
		signaturePendingCvef = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isStampersCompleted").is(Constant.COMPLETED));
		stampedContract = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isStampersCompleted").is(Constant.IN_PROGRESS));
		stamperPendingContract = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isLhdnSignersCompleted").is(Constant.COMPLETED));
		lhdnCompletedContract = mongoTemplate.count(query, TADocument.class);
		query = new Query();
		if (subsidiary != null && !subsidiary.isBlank()) {
			Pattern pattern = Pattern.compile(Pattern.quote(subsidiary));
			query.addCriteria(Criteria.where("subsidiary").regex(pattern));
		}
		if (category != null && !category.isEmpty()) {
				query.addCriteria(Criteria.where("category").is(category));
		}
		query.addCriteria(Criteria.where("isLhdnSignersCompleted").is(Constant.IN_PROGRESS));
		lhdnPendingContract = mongoTemplate.count(query, TADocument.class);

		legalDashboardCountResponse.setTotalDocument(totalDocument);
		legalDashboardCountResponse.setCompletedCvef(completedCvef);
		legalDashboardCountResponse.setRejectedCvef(rejectedCvef);
		legalDashboardCountResponse.setSignaturePendingCvef(signaturePendingCvef);
		legalDashboardCountResponse.setStampedContract(stampedContract);
		legalDashboardCountResponse.setStamperPendingContract(stamperPendingContract);
		legalDashboardCountResponse.setLhdnCompletedContract(lhdnCompletedContract);
		legalDashboardCountResponse.setLhdnPendingContract(lhdnPendingContract);
		return new CommonResponse(HttpStatus.OK, new Response("DashboardCountResponse", legalDashboardCountResponse),
				"Legal Dashboard details fetched successfully");
	}

	public CommonResponse getContractCompletedCvefAndStamperDashboardList(HttpServletRequest request, String category, String subsidiary) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn"); 
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").is(Constant.COMPLETED));
		if (category != null && !category.isEmpty()) {
			query.addCriteria(Criteria.where("category").is(category));
		}
		if (subsidiary != null && !subsidiary.isBlank()) {
			query.addCriteria(Criteria.where("subsidiary").regex(subsidiary.replaceAll("[()]", "\\\\$0"), "i"));
		}
		List<LoaContractDocument> list = new ArrayList<>(mongoTemplate.find(query, LoaContractDocument.class));
		List<DashboardPcdResponse> dashboardPcdResponseList = new ArrayList<>();
		for (LoaContractDocument document : list) {
			DashboardPcdResponse dashboardPcdResponse = new DashboardPcdResponse();
			dashboardPcdResponse.setContractName(document.getProjectName());
			dashboardPcdResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardPcdResponse.setCommencementDate(document.getCommencementDate());
			dashboardPcdResponse.setStatus(document.getStatus());
			dashboardPcdResponse.setCompletionDate(document.getCompletionDate());
			dashboardPcdResponse.setVendorName(document.getVendorName());
			dashboardPcdResponse.setSubsidiary(document.getSubsidiary());
			dashboardPcdResponse.setCreatedOn(document.getCreatedOn());
			dashboardPcdResponse.setContractNumber(document.getContractNumber());
			dashboardPcdResponse.setProjectId(document.getProjectId());
			dashboardPcdResponseList.add(dashboardPcdResponse);
		}
		response.setResponse(new Response("Dashboard Legal Contract List", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Completed Legal Document has been retrieved successfully");
		return response;
	}

	public CommonResponse getTaCompletedCvefAndStamperDashboardList(HttpServletRequest request, String category, String subsidiary) {
		CommonResponse response = new CommonResponse();
		Query query = new Query();
		Sort sort = Sort.by(Sort.Direction.DESC, "createdOn");
		PageRequest pageRequest = PageRequest.of(0, 10, sort);
		query.with(pageRequest);
		query.addCriteria(Criteria.where("status").is(Constant.COMPLETED));
		if (category != null && !category.isEmpty()) {
			query.addCriteria(Criteria.where("category").is(category));
		}
		if (subsidiary != null && !subsidiary.isBlank()) {
			query.addCriteria(Criteria.where("subsidiary").regex(subsidiary.replaceAll("[()]", "\\\\$0"), "i"));
		}
		List<TADocument> list = new ArrayList<>(mongoTemplate.find(query, TADocument.class));
		List<DashboardCommercialResponse> dashboardPcdResponseList = new ArrayList<>();
		for (TADocument document : list) {
			DashboardCommercialResponse dashboardCommercialResponse = new DashboardCommercialResponse();
			dashboardCommercialResponse.setContractTitle(document.getContractTitle());
			dashboardCommercialResponse.setProjectId(document.getProjectId());
			dashboardCommercialResponse.setEnvelopeId(document.getEnvelopeId());
			dashboardCommercialResponse.setCommencementDate(document.getCommencementDate());
			dashboardCommercialResponse.setStatus(document.getStatus());
			dashboardCommercialResponse.setExpiryDate(document.getExpiryDate());
			dashboardCommercialResponse.setSubsidiary(document.getSubsidiary());
			dashboardCommercialResponse.setTenant(document.getTenant());
			dashboardCommercialResponse.setCreatedOn(document.getCreatedOn());
			dashboardPcdResponseList.add(dashboardCommercialResponse);
		}
		response.setResponse(new Response("Latest Completed Legal Ta list", dashboardPcdResponseList));
		response.setStatus(HttpStatus.OK);
		response.setMessage("Latest Completed Legal Document has been retrieved successfully");
		return response;
	}
}
