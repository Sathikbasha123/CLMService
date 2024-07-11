package saaspe.clm.constant;

import java.util.Arrays;
import java.util.List;

public class Constant {

	private Constant() {
		super();
	}

	public static final String HEADER_PROVIDER_NAME = "Internal";
	public static final String HEADER_PROVIDER_STRING = "X-Auth-Provider";
	public static final String GRAPH_GROUP_URL_ME = "https://graph.microsoft.com/v1.0/me";
	public static final String GRAPH_GROUP_URL = "https://graph.microsoft.com/v1.0/me/transitiveMemberOf/microsoft.graph.group";

	public static final String ROLE_SUPER_ADMIN = "VIEW_USER, VIEW_APPLICATION, VIEW_DEPARTMENT, VIEW_ONBOARDINGMGMT, REVIEW_ONBOARDINGMGMT, APPROVE_ONBOARDINGMGMT, ADD_ADMINUSER, VIEW_ADMINUSER, EDIT_ADMINUSER, DELETE_ADMINUSER, ADD_MULTICLOUD, EDIT_MULTICLOUD, DELETE_MULTICLOUD, VIEW_MULTICLOUD, ADD_INVOICE, DELETE_INVOICE, VIEW_PROJECT, VIEW_INTEGRATION, VIEW_DASHBOARD, VIEW_INVOICE, VIEW_SUBSCRIPTION, VIEW_MARKETPLACE, VIEW_CONTRACT, EDIT_CURRENCY, CREATE_BUDGET";
	public static final String ROLE_REVIEWER = "VIEW_USER, VIEW_APPLICATION, VIEW_DEPARTMENT, VIEW_ONBOARDINGMGMT, REVIEW_ONBOARDINGMGMT, APPROVE_ONBOARDINGMGMT, VIEW_MULTICLOUD, VIEW_PROJECT, VIEW_INTEGRATION, VIEW_DASHBOARD, VIEW_INVOICE, VIEW_SUBSCRIPTION, VIEW_MARKETPLACE, VIEW_CONTRACT";
	public static final String ROLE_APPROVER = "VIEW_USER, VIEW_APPLICATION, VIEW_DEPARTMENT, VIEW_ONBOARDINGMGMT, REVIEW_ONBOARDINGMGMT, APPROVE_ONBOARDINGMGMT, VIEW_MULTICLOUD, VIEW_PROJECT, VIEW_INTEGRATION, VIEW_DASHBOARD, VIEW_INVOICE, VIEW_SUBSCRIPTION, VIEW_MARKETPLACE, VIEW_CONTRACT";
	public static final String ROLE_CONTRIBUTOR = "VIEW_USER, VIEW_APPLICATION, VIEW_DEPARTMENT, ADD_USER, ADD_APPLICATION, ADD_DEPARTMENT, EDIT_USER, EDIT_APPLICATION, EDIT_DEPARTMENT, DELETE_USER, DELETE_APPLICATION, DELETE_DEPARTMENT, VIEW_REQUESTMGMT, ENABLE_INTEGRATION, REMOVE_INTEGRATION, MAP_INTEGRATION, VIEW_MULTICLOUD, VIEW_INTEGRATION, VIEW_DASHBOARD, VIEW_INVOICE, VIEW_SUBSCRIPTION, VIEW_PROJECT, EDIT_PROJECT, VIEW_MARKETPLACE, VIEW_CONTRACT, ADD_CONTRACT, EDIT_CONTRACT, ADD_PROJECT";
	public static final String ROLE_SUPPORT = "ADD_WORKFLOW, EDIT_WORKFLOW, VIEW_WORKFLOW, VIEW_USER, VIEW_APPLICATION, VIEW_DEPARTMENT, VIEW_MULTICLOUD, VIEW_PROJECT, VIEW_INTEGRATION, VIEW_DASHBOARD, VIEW_INVOICE, VIEW_SUBSCRIPTION, VIEW_MARKETPLACE, VIEW_CONTRACT, ADD_INVOICE, DELETE_INVOICE";
	public static final String ROLE_CUSTOM = "VIEW_USER, VIEW_APPLICATION, VIEW_DEPARTMENT, VIEW_REQUESTMGMT, VIEW_MULTICLOUD, VIEW_INTEGRATION, VIEW_DASHBOARD, VIEW_INVOICE, VIEW_SUBSCRIPTION, VIEW_PROJECT, VIEW_MARKETPLACE, VIEW_CONTRACT";
	public static final String ROLE_CLM = "VIEW_CONTRACT, ADD_CONTRACT, EDIT_CONTRACT";

	public static final String ROLE_CLM_CREATE_LOA = "VIEW_DASHBOARD, VIEW_TEMPLATE, ADD_TEMPLATE, EDIT_TEMPLATE, VIEW_TEMPLATE, VIEW_LOA, ADD_LOA, VIEW_LOA, EDIT_LOA";
	public static final String ROLE_CLM_REVIEW_LOA = "VIEW_DASHBOARD, VIEW_LOA, APPROVE_LOA";
	public static final String ROLE_CLM_CREATE_CONTRACT = "VIEW_DASHBOARD, VIEW_LOA, VIEW_CONTRACT, ADD_CONTRACT, VIEW_CONTRACT, EDIT_CONTRACT";
	public static final String ROLE_CLM_REVIEW_CONTRACT = "VIEW_DASHBOARD, VIEW_CONTRACT, VIEW_CONTRACT, APPROVE_CONTRACT";

	public static final int EMAIL_VERIFICATION_CODE_EXPIRE_DATE = 2880;

	public static final String USER_ID_ERROR_MESSAGE = "UserId or EmailAddress must be valid";
	public static final String USER_ID_ERROR_KEY = "userId";
	public static final String VERIFY_INITIATE_URL = "/api/userprofile/verify-initiate";
	public static final String RESET_INITIATE_URL = "/api/userprofile/reset-initiate";
	public static final String VERIFY_EMAIL_ERROR_KEY = "emailAddress";
	public static final String VERIFY_EMAIL_ERROR_MESSAGE = "Email is already verified";

	public static final String TOKEN_PREFIX = "Bearer ";
	public static final String HEADER_STRING = "Authorization";
	public static final String SIGN_UP_URL = "/api/userprofile/signup";
	public static final String LOGIN_URL = "/api/userprofile/login";
	public static final String RESET_PASSWORD_URL = "/api/userprofile/reset-password";
	public static final String VERIFY_EMAIL_URL = "/api/userprofile/verify-email";
	public static final String VERIFY_OTP = "/api/userprofile/verify-otp";
	public static final String CREATE_PASSWORD = "/api/auth/create-password";
	public static final String REFRESH_TOKEN = "/api/userprofile/refresh/token";
	public static final String DOCUSIGN_EVENTS = "/docusign/events";
	public static final String ENQUIRY = "/api/enquiry";

	public static final String CONFIRM_PASSWORD_ERROR_MESSAGE = "Password and Confirm Password don't match";
	public static final String NEW_PASSWORD_EQUALS_OLD_PASSWORD_ERROR_MESSAGE = "New Password cannot be the same as Old Password";
	public static final String CONTRACT_EMAIL_SUBJECT = "Contract Renewal Reminder";

	public static final String BUID = "BUID_01";

	public static final String URL_ERROR = "URL Error";

	public static final String UNABLE_TO_CONNECT_TO_AZURE = "Unable to Connect to Azure, Please Check URL in properties";

	public static final String INSUFFICIENT_STORAGE = "INSUFFICIENT_STORAGE";

	public static final String CLIENT_ERROR = "A Client side exception occurred, please get back after sometime!!";

	public static final String START_TIME = "startTime";

	public static final String END_TIME = "endTime";

	public static final String TIME_TAKEN = "timeTaken";

	public static final String AUTH_CODE = "auth_code";
	public static final String DEV = "dev";

	public static final String DOCUSIGN_REDIS_PREFIX = "CLM-DS";

	public static final List<String> CLM_ENVELOPE_STATUS = Arrays.asList("completed", "created", "declined",
			"delivered", "sent", "signed", "voided", "expired");

	public static final String CLM_CONTRACT_RESPONSE = "CLM Contract Response";
	public static final String CONTRACT_CREATION_FAILED = "Contract Creation Failed";
	public static final String UPDATE_TEMPLATE_RESPONSE = "UpdateTemplateResponse";
	public static final String CREATE_TEMPLATE_RESPONSE = "CreateTemplateResponse";
	public static final String ENVELOPE_DOCUMENT_RESPONSE = "Envelope document Response";
	public static final String HTTP_STATUS_CODE = "HTTP status code: ";
	public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final String LIST_ENVELOPE_RESPONSE = "ListEnvelope Recipients Response";
	public static final String ENVELOPE_DOCUMENT_DETAILS_RESPONSE = "EnvelopeDocumentdetails Response";
	public static final String TEMPLATE_LIST_RESPONSE = "Template List Response";
	public static final String TEMPLATE_DOCUMENT_RESPONSE = "Template document Response";
	public static final String ENVELOPE_COMMENTS_RESPONSE = "EnvelopeCommentsResponse";
	public static final String LOA_DOCUMENT_RESPONSE = "Document Response";
	public static final String NO_TEMPLATE_LIST_RESPONSE = "No Template Found, Please create New Template";

	public static final String LOA_CONTRACT_DOCUMENT_RESPONSE = "LOA Contract Document Response";

	public static final String CMU_DOCUMENT_RESPONSE = "CMU Document Response";
	public static final String LOO_DOCUMENT_RESPONSE = "LOO Document Response";
	public static final String CVEF_DOCUMENT_RESPONSE = "CVEF Document Response";
	public static final String TA_DOCUMENT_RESPONSE = "TA Document Response";
	public static final String USER_ACCESS_ROLE_RESPONSE = "UserAccessAndRolesResponse";
	public static final String DOCUSIGN_HOST = "{docusignHost}";
	public static final String CREATE_FILE = "createFile";
	public static final String DELETE = "delete";
	public static final String TEMPLATE_ID = "templateId";
	public static final String ENVELOPE_ID = "envelopeId";
	public static final String SENDER = "sender";
	public static final String USERNAME = "userName";
	public static final String EMAIL = "email";
	public static final String AZURE = "azure";
	public static final String USER_NAME = "userName";
	public static final String STATUS = "status";
	public static final String CREATED_DATE_TIME = "createdDateTime";
	public static final String COMPLETED_DATE_TIME = "completedDateTime";
	public static final String CREATE_ID = "create_id";
	public static final String TEMPLATEID = "{templateId}";
	public static final String TEMPLATE_NAME = "template_name";
	public static final String ENVELOPEID = "{envelopeId}";
	public static final String AZURE_TOKEN = "azuretoken";
	public static final String FIRST_NAME = "firstName";
	public static final String DISPLAY_NAME = "displayName";
	public static final String LAST_NAME = "lastName";
	public static final String SEND = "sent";
	public static final String LOA_MODULE = "loa";
	public static final String CONTRACT_MODULE = "contracts";
	public static final String CMU_MODULE = "cmu";
	public static final String LOO_MODULE = "loo";
	public static final String TA_MODULE = "ta";
	public static final String REVIEWER_REQUEST_SUBJECT = "Review and Approval Requested: {{contractName}} {{moduleName}}";
	public static final String REVIEWER_APPROVED_SUBJECT = "{{contractName}} {{moduleName}} - Reviewed and Approved!";
	public static final String USER_REDIRECT_URL = "{{host}}/auth/login?redirect=/{{flowType}}";
	public static final String MAIL_DRILLDOWN_REDIRECT_URL = "{{host}}/auth/login?redirect=/{{moduleName}}/view/{{envelopId}}";
	public static final String CMU_REVIEWER = "CMU_REVIEWER";
	public static final String LOO_REVIEWER = "LOO_REVIEWER";
	public static final String TA_REVIEWER = "TA_REVIEWER";

	public static final String LOA_DOCUMENT_REPOSITORY = "LOA";
	public static final String LOA_CONTRACT_DOCUMENT_REPOSITORY = "LOA_CONTRACT";
	public static final String LOO_CONTRACT_DOCUMENT_REPOSITORY = "LOO";
	public static final String CMU_DOCUMENT_REPOSITORY = "CMU";
	public static final String CVEF_DOCUMENT_REPOSITORY = "CVEF";
	public static final String TA_DOCUMENT_REPOSITORY = "TA";
	public static final String DOCUMENTID = "{documentId}";
	public static final String COMPLETED = "completed";
	public static final String PENDING = "pending";
	public static final String IN_PROGRESS = "In-Progress";
	public static final String REJECTED = "rejected";
	public static final String PCD_ADMIN = "PCD_ADMIN";
	public static final String COMMERCIAL_ADMIN = "COMMERCIAL_ADMIN";
	public static final String LOA_REVIEWER = "LOA_REVIEWER";
	public static final String SENT = "sent";

	public static final String LOA_CREATOR = "LOA_CREATOR";
	public static final String CONTRACT_CREATOR = "CONTRACT_CREATOR";
	public static final String CONTRACT_REVIEWER = "CONTRACT_REVIEWER";
	public static final String CMU_CREATOR = "CMU_CREATOR";
	public static final String LOO_CREATOR = "LOO_CREATOR";
	public static final String SUPER_ADMIN = "SUPER_ADMIN";
	public static final String LEGAL_ADMIN = "LEGAL_ADMIN";
	public static final String LEGAL_USER = "LEGAL_USER";
	public static final String TA_CREATOR = "TA_CREATOR";
	public static final String PCD_USER = "PCD_USER";
	public static final String COMMERCIAL_USER = "COMMERCIAL_USER";

	public static final String PCD = "PCD";
	public static final String COMMERCIAL = "COMMERCIAL";
	public static final String LEGAL = "LEGAL";

	public static final String CLM_NEW_USER_REQUESTING_SUBJECT = "{{requestedUserName}} - Requesting for access to CLM";
	public static final String CLM_NEW_USER_ADDED_SUBJECT = "Welcome to CLM!";
	public static final String CLM_APPROVED_USER_SUBJECT = "Access Granted for {{nameOfRequester}} to CLM";
	public static final String CLM_REJECTED_USER_SUBJECT = "Access Rejected for {{nameOfRequester}} to CLM";
	public static final String APPROVED = "APPROVED";
	public static final String CLM_APPROVED_USER_COMMON_MESSAGE = "These modules have been selected based on your assigned role and responsibilities. You should now be able to log into the CLM system and access these modules.<br>";
	public static final String CLM_APPROVED_USER_DOCUSIGN_REQUIRED_MESSAGE = "<br>Please make sure to activate your DocuSign account, and to grant Consent to the CLM portal ";
	public static final String CLM_USER_ROLES_UPDATED_SUBJECT = "Role Change for {{nameOfUser}} in CLM";
	public static final String CLM_USER_ACTIVATED_SUBJECT = "Account Reactivated for {{nameOfUser}}";

	public static final List<String> PCD_ADMIN_ACCESS_LIST = Arrays.asList("LOA_CREATOR","CONTRACT_CREATOR","LOA_REVIEWER", "CONTRACT_REVIEWER",
			"SUPER_ADMIN", "PCD_ADMIN","LEGAL_USER","LEGAL_ADMIN");
	public static final List<String> COMMERCIAL_ADMIN_ACCESS_LIST = Arrays.asList("CMU_CREATOR","LOO_CREATOR","TA_CREATOR","LOO_REVIEWER",
			"CMU_REVIEWER", "TA_REVIEWER", "SUPER_ADMIN", "COMMERCIAL_ADMIN","LEGAL_USER","LEGAL_ADMIN");
}
