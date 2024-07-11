package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import saaspe.clm.configuration.mongo.SequenceGeneratorService;
import saaspe.clm.constant.Constant;
import saaspe.clm.document.UserOnboardingDocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.*;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.UserOnboardingRepository;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.service.UserOnboardingService;

@Service
public class UserOnboardingServiceImpl implements UserOnboardingService {

	private static final Logger log = LoggerFactory.getLogger(UserOnboardingServiceImpl.class);

	@Autowired
	private UserOnboardingRepository userOnboardingRepository;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Value("${spring.mail.username}")
	private String fromMail;

	@Value("${sendgrid.domainname}")
	private String domainName;

	@Autowired
	private MailSenderService mailSenderService;

	@Value("${docusign.roles}")
	private String docusignRoles;

	private final MongoTemplate mongoTemplate;

    public UserOnboardingServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
	public CommonResponse userOnboarding(@Valid UserOnboardingRequest userOnboardingRequest, String email,String name)
			throws DataValidationException, TemplateNotFoundException, MalformedTemplateNameException, ParseException,
			IOException, TemplateException, MessagingException {

		if (!userOnboardingRequest.getDivision().equalsIgnoreCase("PCD")
				&& !userOnboardingRequest.getDivision().equalsIgnoreCase("COMMERCIAL") && !userOnboardingRequest.getDivision().equalsIgnoreCase(Constant.LEGAL)) {
			throw new DataValidationException("Division should be PCD/COMMERCIAL/LEGAL", null, null);
		}
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmail(email);
		if(usersInfoDocument!=null){
			throw new DataValidationException("User already exists in Clm Application", null, null);
		}
		List<UserOnboardingDocument> userOnboardingDocumentList = userOnboardingRepository.findByEmail(email);
		if(userOnboardingDocumentList!=null && !userOnboardingDocumentList.isEmpty() && userOnboardingDocumentList.get(0).getStatus().equalsIgnoreCase(Constant.PENDING)){
			throw new DataValidationException("Your Have already requested", null, null);
		}
		if(userOnboardingRequest.getRequestRoles().size()>1 && userOnboardingRequest.getRequestRoles().stream().anyMatch(e -> e.contains("ADMIN")))
		{
		    throw new DataValidationException("Admin role has access to all the modules. Please request again!", "400", HttpStatus.BAD_REQUEST);
		}
		UserOnboardingDocument userOnboardingDocument = new UserOnboardingDocument();
		userOnboardingDocument.setId(sequenceGeneratorService.generateSequence(UserOnboardingDocument.SEQUENCE_NAME));
		userOnboardingDocument.setName(name);
		userOnboardingDocument.setEmail(email);
		userOnboardingDocument.setDivision(userOnboardingRequest.getDivision());
		List<String> roles = new ArrayList<>();
		for (String role : userOnboardingRequest.getRequestRoles()) {
			roles.add(role);
		}
		userOnboardingDocument.setUniqueId(UUID.randomUUID().toString());
		userOnboardingDocument.setRequestRoles(roles);
		userOnboardingDocument.setRoles(roles);
		userOnboardingDocument.setCreatedOn(new Date());
		userOnboardingDocument.setCreatedBy(email);
		userOnboardingDocument.setCreatedThrough("Request");
		userOnboardingDocument.setStatus(Constant.PENDING);
		userOnboardingRepository.save(userOnboardingDocument);
		try {
			mailSenderService.sendMailForNewUserRequest(name, userOnboardingRequest.getDivision(), roles, userOnboardingDocument.getRoles().get(0));
		} catch (IOException | TemplateException | MessagingException e1) {
			e1.printStackTrace();
		}
		return new CommonResponse(HttpStatus.CREATED, new Response("", new ArrayList<>()),
				"Onboarding request submitted!");
	}

	@Override
	public CommonResponse addUserDetailsByAdmin(@Valid UserOnboardingRequest userOnboardingRequest, String email)
			throws DataValidationException {
		if (!userOnboardingRequest.getDivision().equalsIgnoreCase("PCD")
				&& !userOnboardingRequest.getDivision().equalsIgnoreCase("COMMERCIAL") && !userOnboardingRequest.getDivision().equalsIgnoreCase(Constant.LEGAL)) {
			throw new DataValidationException("Division should be PCD/COMMERCIAL", null, null);
		}
		if(userInfoRespository.findByEmail(userOnboardingRequest.getEmail())!=null){
			throw new DataValidationException("User Already Exists", "404", HttpStatus.BAD_REQUEST);
		}
		UsersInfoDocument usersInfoDocument = new UsersInfoDocument();
		usersInfoDocument.setId(sequenceGeneratorService.generateSequence(UsersInfoDocument.SEQUENCE_NAME));
		usersInfoDocument.setName(userOnboardingRequest.getName());
		usersInfoDocument.setEmail(userOnboardingRequest.getEmail());
		usersInfoDocument.setCreatedOn(new Date());
		usersInfoDocument.setUpdatedOn(new Date());
		usersInfoDocument.setUpdatedBy(email);
		usersInfoDocument.setCreatedBy(email);
		List<String> role = new ArrayList<>();
        role.addAll(userOnboardingRequest.getRequestRoles());
		usersInfoDocument.setCurrentRole(role.get(0));
		usersInfoDocument.setUniqueId(UUID.randomUUID().toString());
		usersInfoDocument.setRoles(role);
		usersInfoDocument.setActive(true);
		usersInfoDocument.setDivision(userOnboardingRequest.getDivision());
		usersInfoDocument.setCreatedThrough("Portal");
		userInfoRespository.save(usersInfoDocument);
		try {
			mailSenderService.sendMailToNewUserAddedByAdmin(userOnboardingRequest.getEmail(), userOnboardingRequest.getName(), userOnboardingRequest.getDivision(), userOnboardingRequest.getRequestRoles());
		} catch (IOException | TemplateException | MessagingException e1) {
			e1.printStackTrace();
		}
		return new CommonResponse(HttpStatus.CREATED, new Response("", new ArrayList<>()),
				"Onboarding request submitted!");
	}

	@Override
	public CommonResponse approveUser(@Valid UserApprovalRequest userApprovalRequest, String email, String name) throws DataValidationException{
		UserOnboardingDocument onboardeduser = userOnboardingRepository
				.findByUniqueId(userApprovalRequest.getUniqueId());
		if(userInfoRespository.findByEmail(onboardeduser.getEmail())!=null){
			throw new DataValidationException("User Already Added", "404", HttpStatus.BAD_REQUEST);
		}
		if(userApprovalRequest.getApproved_roles().isEmpty()){
			throw new DataValidationException("Must Assign Atleast One Role to user", "404", HttpStatus.BAD_REQUEST);
		}
		if(onboardeduser.getStatus().equalsIgnoreCase(Constant.REJECTED)){
			throw new DataValidationException("User Have been reject already", "404", HttpStatus.BAD_REQUEST);
		}
		onboardeduser.setUpdatedOn(new Date());
		onboardeduser.setUpdatedBy(email);
		onboardeduser.setStatus(Constant.COMPLETED);
		onboardeduser.setApprovedBy(email);
		userOnboardingRepository.save(onboardeduser);
		UsersInfoDocument usersInfoDocument = new UsersInfoDocument();
		usersInfoDocument.setId(sequenceGeneratorService.generateSequence(UsersInfoDocument.SEQUENCE_NAME));
		usersInfoDocument.setName(onboardeduser.getName());
		usersInfoDocument.setActive(true);
		usersInfoDocument.setEmail(onboardeduser.getEmail());
		usersInfoDocument.setDivision(onboardeduser.getDivision());
		usersInfoDocument.setUniqueId(UUID.randomUUID().toString());
		List<String> roles = new ArrayList<>();
        roles.addAll(userApprovalRequest.getApproved_roles());
		usersInfoDocument.setCurrentRole(roles.isEmpty()?null:roles.get(0));
		usersInfoDocument.setApprovedBy(email);
		usersInfoDocument.setRoles(roles);
		usersInfoDocument.setCreatedOn(new Date());
		usersInfoDocument.setCreatedBy(email);
		usersInfoDocument.setUpdatedOn(new Date());
		usersInfoDocument.setCreatedThrough("Request");
		List<String> docusingRolesList = Arrays.asList(docusignRoles.split(", "));
		String message = Constant.CLM_APPROVED_USER_COMMON_MESSAGE;
		if(docusingRolesList.stream().anyMatch(usersInfoDocument.getCurrentRole()::equalsIgnoreCase)) {
			message += Constant.CLM_APPROVED_USER_DOCUSIGN_REQUIRED_MESSAGE;
		}
		String subject = Constant.CLM_APPROVED_USER_SUBJECT;
		subject = subject.replace("{{nameOfRequester}}", usersInfoDocument.getName());
		try {
			mailSenderService.sendMailToRequestedUserForStatus(usersInfoDocument.getEmail(), subject, usersInfoDocument.getName()
					,usersInfoDocument.getCurrentRole().equalsIgnoreCase(Constant.SUPER_ADMIN)?"CLM":usersInfoDocument.getDivision()
							, usersInfoDocument.getRoles(), Constant.APPROVED, message);
		} catch (IOException | TemplateException | MessagingException e1) {
			e1.printStackTrace();
		}
		userInfoRespository.save(usersInfoDocument);
		return new CommonResponse(HttpStatus.CREATED, new Response("ApprovedUserResponse", new ArrayList<>()),
				"User Approved Sucessfully!");
	}

	@Override
	public CommonResponse getOnboardedUsersListView(int page, int limit, String searchText, String email, String order,
			String orderBy, String createdThrough, String division, String status) {
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
		List<UserOnboardingDocument> list = null;
		long totalCount = 0;
		Query query = new Query();
		Criteria criteria = new Criteria();
		if(searchText!=null&&!searchText.isEmpty()) {
			criteria.orOperator(
					Criteria.where("name").regex(searchText, "i"),
					Criteria.where("email").regex(searchText, "i")
			);
			query.addCriteria(criteria);
		}
		if(status != null && !status.isBlank()){
			query.addCriteria(Criteria.where("status").is(status));
		}

		if (division != null && !division.isBlank()) {
			query.addCriteria(Criteria.where("division").regex(division, "i"));
		}
		log.info("query : {}",query);
		Pageable pageableObject = pageable;
		totalCount = mongoTemplate.count(query,UserOnboardingDocument.class);
		if(order!=null){
			query.with(pageableObject);
		}
		list = mongoTemplate.find(query,UserOnboardingDocument.class).stream().collect(Collectors.toList());
		List<UserOnboardingListResponse> userListResponses=new ArrayList<>();
		if(!list.isEmpty()) {
		    userListResponses = list.stream().map(userOnboardingDocument -> {
			UserOnboardingListResponse userListResponse = new UserOnboardingListResponse();
			userListResponse.setName(userOnboardingDocument.getName());
			userListResponse.setUniqueId(userOnboardingDocument.getUniqueId());
			userListResponse.setEmail(userOnboardingDocument.getEmail());
			userListResponse.setDivision(userOnboardingDocument.getDivision());
			userListResponse.setRequestedRoles(userOnboardingDocument.getRequestRoles());
			userListResponse.setCreatedThrough(userOnboardingDocument.getCreatedThrough());
			userListResponse.setCreatedOn(userOnboardingDocument.getCreatedOn());
			userListResponse.setCreatedBy(userOnboardingDocument.getCreatedBy());
			userListResponse.setApprovedBy(userOnboardingDocument.getApprovedBy());
			userListResponse.setRejectedAt(userOnboardingDocument.getRejectedAt());
			userListResponse.setRejectedBy(userOnboardingDocument.getRejectedBy());
			userListResponse.setRejectionReason(userOnboardingDocument.getRejectionReason());
			userListResponse.setRoles(userOnboardingDocument.getRoles());
			if (userOnboardingDocument.getUpdatedOn() != null) {
				userListResponse.setUpdatedOn(userOnboardingDocument.getUpdatedOn());
			}
			if (userOnboardingDocument.getUpdatedBy() != null) {
				userListResponse.setUpdatedBy(userOnboardingDocument.getUpdatedBy());
			}
			return userListResponse;
		}).collect(Collectors.toList());
		}
		UserOnboardListPagination userswithpagination = new UserOnboardListPagination();
		userswithpagination.setTotal(totalCount);
		userswithpagination.setRecords(userListResponses);
		return new CommonResponse(HttpStatus.OK, new Response("UserOnboardListViewResponse", userswithpagination),
				"List of onboarded users retrieved successfully!");
	}

	@Override
	public CommonResponse editUserRoles(EditUserRolesRequest editUserRolesRequest, String email) {
		UserOnboardingDocument userDocument = userOnboardingRepository.findByUniqueId(editUserRolesRequest.getUniqueId());
		if (userDocument != null) {
			List<String> roles = new ArrayList<>();
			for (String role : editUserRolesRequest.getNewRoles()) {
				roles.add(role);
			}
			userDocument.setUpdatedBy(email);
			userDocument.setUpdatedOn(new Date());
			userDocument.setRoles(roles);
			userOnboardingRepository.save(userDocument);
			return new CommonResponse(HttpStatus.OK, new Response("EditUserRoleResponse", new ArrayList<>()), "User roles updated successfully!");
		} else {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("EditUserRoleResponse", new ArrayList<>()), "User not found with the provided unique ID!");
		}
	}

	@Override
	public CommonResponse rejectionUser(@Valid UserRejectRequest userRejectRequest, String email, String name) throws DataValidationException{
		UserOnboardingDocument onboardeduser = userOnboardingRepository
				.findByUniqueId(userRejectRequest.getUniqueId());
		UsersInfoDocument usersInfoDocument=userInfoRespository.findByEmailAndActive(email, true);
		
		if(userInfoRespository.findByEmail(onboardeduser.getEmail())!=null){
			throw new DataValidationException("User Already Added", "404", HttpStatus.BAD_REQUEST);
		}
		onboardeduser.setUpdatedOn(new Date());
		onboardeduser.setUpdatedBy(email);
		onboardeduser.setStatus(Constant.REJECTED);
		onboardeduser.setRejectionReason(userRejectRequest.getRejectionReason());
		onboardeduser.setRejectedBy(email);
		onboardeduser.setRejectedAt(new Date());
		userOnboardingRepository.save(onboardeduser);
		String message = "";
		String subject = Constant.CLM_REJECTED_USER_SUBJECT;
		subject = subject.replace("{{nameOfRequester}}", onboardeduser.getName());
		try {
			mailSenderService.sendMailToRequestedUserForStatus(onboardeduser.getEmail(), subject,onboardeduser.getName()
					, usersInfoDocument!=null? usersInfoDocument.getDivision().equalsIgnoreCase(Constant.SUPER_ADMIN)?"CLM":usersInfoDocument.getDivision():"CLM"
					, onboardeduser.getRoles()
					, Constant.REJECTED, message);
		} catch (IOException | TemplateException | MessagingException e1) {
			e1.printStackTrace();
		}
		return new CommonResponse(HttpStatus.CREATED, new Response("RejectUserResponse", new ArrayList<>()),
				"User Rejected Successfully!");
	}
}
