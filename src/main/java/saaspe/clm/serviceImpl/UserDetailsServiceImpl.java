package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import saaspe.clm.constant.Constant;
import saaspe.clm.document.UserOnboardingDocument;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.Clm;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.DocusignCreateUserResponse;
import saaspe.clm.model.DocusignUserCache;
import saaspe.clm.model.ProfileData;
import saaspe.clm.model.Response;
import saaspe.clm.model.UserAccessRoleResponse;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.repository.UserOnboardingRepository;
import saaspe.clm.service.UserDetailsService;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	@Autowired
	RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RedisUtility redisUtility;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Autowired
	private UserOnboardingRepository userOnboardingRepository;

	@Value("${spring.media.host}")
	private String mediaHost;

	@Value("${spring.image.key}")
	private String imageKey;

	@Value("${sendgrid.domain.support}")
	private String supportEmail;

	@Value("${sendgrid.domain.orgname}")
	private String senderName;

	@Value("${sendgrid.domain.name}")
	private String mailDomainName;

	@Value("${docusign.roles}")
	private String docsignRoles;
	
	@Autowired
	private Configuration config;

	@Autowired
	private JavaMailSender mailSender;

	@Value("${docusign.host}")
	private String docusignHost;

	@Override
	public CommonResponse getProfile(HttpServletRequest request) throws DataValidationException {
		ProfileData userData = new ProfileData();
		String token = request.getHeader(Constant.HEADER_STRING);
		String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		if (provider.equalsIgnoreCase(Constant.AZURE)) {
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			String name = jwt.getClaim("name").asString();
			userData.setUserName(name);
			userData.setUserLogo("https://saaspemedia.blob.core.windows.net/images/avatar/svg/avatar-13.svg");
		}
		return new CommonResponse(HttpStatus.OK, new Response("userProfileResponse", userData),
				"Details Retrieved Successfully");
	}

	@Override
	public CommonResponse getUserAccessAndRole(HttpServletRequest request, String xAuthProvider) {
		CommonResponse commonResponse = new CommonResponse();
		UserAccessRoleResponse accessRoleResponse = new UserAccessRoleResponse();
		Response response = new Response();
		String token = request.getHeader(Constant.HEADER_STRING);
		DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
		String name = jwt.getClaim("name").asString();
		String email = null;
		if (jwt.getClaim(Constant.EMAIL).asString() != null) {
			email = jwt.getClaim(Constant.EMAIL).asString();
		} else if (jwt.getClaim("upn").asString() != null) {
			email = jwt.getClaim("upn").asString();
		}
		accessRoleResponse.setEmailAddress(email);
		accessRoleResponse.setName(name);
		UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmail(email);
		if(usersInfoDocument==null){
			List<UserOnboardingDocument> userOnboardingDocumentList = userOnboardingRepository.findByEmail(email);
			if(userOnboardingDocumentList!=null && !userOnboardingDocumentList.isEmpty()) {
				UserOnboardingDocument userOnboardingDocument = userOnboardingDocumentList.get(0);
				accessRoleResponse.setStatus(null);
				accessRoleResponse.setIsOnboardedUser(false);
				accessRoleResponse.setIsUserOnboardApproved(false);
				if (userOnboardingDocument != null && userOnboardingDocument.getStatus().equalsIgnoreCase(Constant.COMPLETED)) {
					accessRoleResponse.setIsOnboardedUser(true);
					accessRoleResponse.setIsUserOnboardApproved(true);
					accessRoleResponse.setStatus(userOnboardingDocument.getStatus());
				} else if (userOnboardingDocument!=null && userOnboardingDocument.getStatus().equalsIgnoreCase(Constant.REJECTED)) {
					accessRoleResponse.setIsOnboardedUser(true);
					accessRoleResponse.setIsUserOnboardApproved(false);
					accessRoleResponse.setRejectionReason(userOnboardingDocument.getRejectionReason());
				} else if (userOnboardingDocument != null && userOnboardingDocument.getStatus().equalsIgnoreCase(Constant.PENDING)) {
					accessRoleResponse.setIsOnboardedUser(true);
					accessRoleResponse.setIsUserOnboardApproved(false);
					accessRoleResponse.setStatus(userOnboardingDocument.getStatus());
				}
			} else{
				accessRoleResponse.setIsOnboardedUser(false);
				accessRoleResponse.setIsUserOnboardApproved(false);
			}
			return new CommonResponse(HttpStatus.OK,new Response(Constant.USER_ACCESS_ROLE_RESPONSE, accessRoleResponse) , "Please Request Admin To Access");
		}
		accessRoleResponse.setIsOnboardedUser(true);
		accessRoleResponse.setIsUserOnboardApproved(true);
		accessRoleResponse.setIsClmUser(true);
		if(!usersInfoDocument.isActive()) {
			return new CommonResponse(HttpStatus.OK,
					new Response(Constant.USER_ACCESS_ROLE_RESPONSE, accessRoleResponse), "User is Not Active");
		}
		accessRoleResponse.setIsActiveClmUser(true);
		Clm clm = new Clm();
		clm.setActive(true);
		List<String> docusingRolesList = Arrays.asList(docsignRoles.split(", "));
		if(usersInfoDocument.getCurrentRole()!=null && docusingRolesList.stream().anyMatch(usersInfoDocument.getCurrentRole()::equalsIgnoreCase)) {
			accessRoleResponse.setIsDocusignRequired(true);
			DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
			System.out.println("DocuSign userId - "+userId);
			ResponseEntity<Clm> responseEntity = null;
			URI uri = UriComponentsBuilder.fromUriString(docusignHost + "/getConsent").queryParam(Constant.EMAIL, email)
					.queryParam("userId", userId.getUserId()).build().toUri();
			try {
				responseEntity = restTemplate.exchange(uri.toString(), HttpMethod.GET, null, Clm.class);
			} 
			catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("UserAccessAndRolesResponse", new ArrayList<>()), "UserAccessroles failed");
			} catch (RestClientException ex) {
				ex.printStackTrace();
				if (ex instanceof ResourceAccessException) {
					return new CommonResponse(HttpStatus.NOT_FOUND,
							new Response("Document Update Response", new ArrayList<>()),
							"Unable to access remote resources.Please check the connectivity");
				}
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("UserAccessAndRolesResponse", new ArrayList<>()), "UserAccessroles failed");
			}
			if(responseEntity.getBody()!=null)
			{
			if (responseEntity.getBody().getError() == null) {
				clm.setEnabled(true);
				clm.setConsentGiven(true);
				accessRoleResponse.setIsDocusignAccountActivated(true);
				accessRoleResponse.setIsConsentSubmitted(true);
			} else if (responseEntity.getBody().getError() != null
					&& responseEntity.getBody().getError().equalsIgnoreCase("consent_required")) {
				clm.setEnabled(true);
				clm.setConsentGiven(false);
				clm.setConsentUrl(responseEntity.getBody().getConsentUrl());
				accessRoleResponse.setConsentUrl(responseEntity.getBody().getConsentUrl());
				accessRoleResponse.setIsConsentSubmitted(false);
				accessRoleResponse.setIsDocusignAccountActivated(true);
			} else if (responseEntity.getBody().getActivated().equalsIgnoreCase("false")) {
				clm.setActive(false);
				accessRoleResponse.setIsDocusignAccountActivated(false);
			} else if (responseEntity.getBody().getError() != null
					&& responseEntity.getBody().getError().equalsIgnoreCase("invalid_grant")) {
				clm.setEnabled(true);
				clm.setConsentGiven(false);
				clm.setActive(false);
				accessRoleResponse.setIsConsentSubmitted(false);
				accessRoleResponse.setIsDocusignAccountActivated(false);
			}
			}
		}
		
		clm.setEnabled(true);
		accessRoleResponse.setRole(usersInfoDocument.getCurrentRole());
		accessRoleResponse.setClm(clm);
		String[] list = null;
		accessRoleResponse.setAccess(list);
		response.setData(accessRoleResponse);
		response.setAction("UserAccessAndRolesResponse");
		commonResponse.setResponse(response);
		commonResponse.setMessage("Data retrieved successfully");
		commonResponse.setStatus(HttpStatus.OK);

		return commonResponse;
	}

	@Override
	public CommonResponse deleteUserfromredis() {
		URI uri = UriComponentsBuilder.fromUriString(docusignHost + "/get/user").build().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		try {
			JsonNode rootNode = mapper.readTree(response.getBody());
			JsonNode usersNode = rootNode.get("users");
			if (usersNode.isArray() && usersNode.size() > 0) {
				for (JsonNode userNode : usersNode) {
					TokenCache redisEmail = redisUtility.getValue("DO" + userNode.get(Constant.EMAIL).asText());
					if (redisEmail != null && ("Closed".equalsIgnoreCase(userNode.get("membership_status").asText())
							&& userNode.get(Constant.EMAIL).asText().equals(redisEmail.getEmailAddress()))) {
						redisUtility.deleteKeyformredis("DO" + userNode.get(Constant.EMAIL).asText());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public CommonResponse sendConsentToUser(HttpServletRequest request, String xAuthProvider)
			throws DataValidationException, IOException, TemplateException, MessagingException {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		if (xAuthProvider == null || xAuthProvider.equalsIgnoreCase(Constant.AZURE)) {
			String token = request.getHeader(Constant.HEADER_STRING);
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			String email = null;
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
			String name = jwt.getClaim("name").asString();
			String toAddress = email;
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message);
			String subject = "View Consent";
			String consentMailContent;
			Map<String, Object> model = new HashMap<>();
			DocusignUserCache userId = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
			URI uri = UriComponentsBuilder.fromUriString(docusignHost + "/getConsent").queryParam(Constant.EMAIL, email)
					.queryParam("userId", userId.getUserId()).build().toUri();
			ResponseEntity<Clm> responseEntity;
			try {
				responseEntity = restTemplate.exchange(uri.toString(), HttpMethod.GET, null, Clm.class);
			} catch (HttpServerErrorException ex) {
				ex.printStackTrace();
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("UserConsent", new ArrayList<>()),
						"UserConsent failed");
			}
			Clm responseBody = responseEntity.getBody();
			if (responseBody != null && "consent_required".equalsIgnoreCase(responseBody.getError())) {
				model.put("name", name);
				model.put("consentUrl", responseBody.getConsentUrl());
				Template consentMailTemplate = config.getTemplate("user-consent 2.html");
				consentMailContent = FreeMarkerTemplateUtils.processTemplateIntoString(consentMailTemplate, model);
				consentMailContent = consentMailContent.replace("{{name}}", name);
				consentMailContent = consentMailContent.replace("{{consentUrl}}", responseBody.getConsentUrl());
				consentMailContent = consentMailContent.replace("{{supportEmail}}", supportEmail);
				consentMailContent = consentMailContent.replace("{{orgName}}", senderName);
				consentMailContent = consentMailContent.replace("{{mediaHost}}", mediaHost);
				consentMailContent = consentMailContent.replace("{{imageKey}}", imageKey);
			} else {
				response.setData("User with email " + email + " has already given consent");
				response.setAction("UserConsentResponse");
				commonResponse.setResponse(response);
				commonResponse.setMessage("User has already given consent");
				commonResponse.setStatus(HttpStatus.BAD_REQUEST);
				return commonResponse;
			}
			helper.setFrom(mailDomainName, senderName);
			helper.setTo(toAddress!=null?toAddress:"");
			helper.setSubject(subject);
			helper.setText(consentMailContent, true);
			mailSender.send(helper.getMimeMessage());
			response.setData("Consent URL Sent to " + toAddress);
			response.setAction("UserConsentResponse");
			commonResponse.setResponse(response);
			commonResponse.setMessage("Email sent successfully");
			commonResponse.setStatus(HttpStatus.OK);
			return commonResponse;
		} else
			throw new DataValidationException("Headers should be azure", "400", HttpStatus.BAD_REQUEST);
	}

	@Override
	public CommonResponse deleteUserfromredisss() {

		URI uri = UriComponentsBuilder.fromUriString(docusignHost + "/get/user").build().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		try {
			JsonNode rootNode = mapper.readTree(response.getBody());
			JsonNode usersNode = rootNode.get("users");
			if (usersNode.isArray() && usersNode.size() > 0) {
				for (JsonNode userNode : usersNode) {
					DocusignUserCache redisEmail = redisUtility
							.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + userNode.get(Constant.EMAIL).asText());
					if (redisEmail != null) {
						if ("Closed".equalsIgnoreCase(userNode.get("membership_status").asText())
								&& userNode.get(Constant.EMAIL).asText().equals(redisEmail.toString())) {
							redisUtility.deleteKeyformredis(
									Constant.DOCUSIGN_REDIS_PREFIX + userNode.get(Constant.EMAIL).asText());
						}
						redisUtility.deleteKeyformredis(
								Constant.DOCUSIGN_REDIS_PREFIX + userNode.get(Constant.EMAIL).asText());
						DocusignCreateUserResponse docusingCreateUserResponse = mapper.readValue(response.getBody(),
								DocusignCreateUserResponse.class);
						DocusignUserCache doCache = new DocusignUserCache();
						doCache.setUserName(docusingCreateUserResponse.getUserName());
						doCache.setUserId(docusingCreateUserResponse.getId());
						doCache.setUserEmail(docusingCreateUserResponse.getEmail());
						redisUtility.setDocusingValue(
								Constant.DOCUSIGN_REDIS_PREFIX + docusingCreateUserResponse.getEmail(), doCache);
					} else{
						DocusignCreateUserResponse docusingCreateUserResponse = mapper.readValue(response.getBody(),
								DocusignCreateUserResponse.class);
						DocusignUserCache doCache = new DocusignUserCache();
						doCache.setUserName(docusingCreateUserResponse.getUserName());
						doCache.setUserId(docusingCreateUserResponse.getId());
						doCache.setUserEmail(docusingCreateUserResponse.getEmail());
						redisUtility.setDocusingValue(
								Constant.DOCUSIGN_REDIS_PREFIX + docusingCreateUserResponse.getEmail(), doCache);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String deleteUserfromredisss(String email) {
		DocusignUserCache cache = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
		if (cache != null) {
			return cache.getUserId();
		} else {
			return "No values " + email;
		}
	}

	@Override
	public String getAzureUserfromredisss(String userEmail) {
		TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + userEmail);
		DocusignUserCache cache = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + userEmail);
		if (cacheValue != null) {
			return cacheValue.toString() + "Docusign : " + cache;
		} else {
			return "No values " + userEmail;
		}
	}

}
