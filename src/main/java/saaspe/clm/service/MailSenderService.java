package saaspe.clm.service;

import freemarker.core.ParseException;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StringUtils;

import saaspe.clm.constant.Constant;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.model.PostCommentMailBody;
import saaspe.clm.repository.UserInfoRespository;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MailSenderService {

	@Value("${sendgrid.domain.name}")
	private String mailDomainName;

	@Value("${sendgrid.domain.orgname}")
	private String senderName;

	@Value("${spring.mail.username}")
	private String fromMail;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private Configuration config;

	@Value("${spring.media.host}")
	private String mediaHost;

	@Value("${spring.image.key}")
	private String imageKey;

	@Value("${spring.host}")
	private String host;

	@Value("${sendgrid.domain.support}")
	private String supportMail;

	@Value("${sendgrid.domain.orgname}")
	private String orgName;

	@Value("${sendgrid.domainname}")
	private String domainName;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Async("threadExecutor")
	public void sendRequestForReviewMail(String envelopeId, String reviewerEmail, String reviewerName,
			String contractName, String versionNumber, String creatorEmail,String flowType,String moduleName)
			throws IOException, TemplateException, MessagingException {
		String toAddress = reviewerEmail;
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{envelopId}}", envelopeId);
		url = url.replace("{{moduleName}}", moduleName.toLowerCase().concat("-review"));
		String subject = Constant.REVIEWER_REQUEST_SUBJECT;
		subject = subject.replace("{{moduleName}}", moduleName.toUpperCase());
		subject = subject.replace("{{contractName}}", contractName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-request-for-review.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{contractName}}", contractName);
		content = content.replace("{{reviewerName}}", reviewerName);
		content = content.replace("{{moduleName}}", moduleName.toUpperCase());
		content = content.replace("{{versionNumber}}", versionNumber);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{creatorEmail}}", creatorEmail);
		content = content.replace("{{orgName}}", senderName);

		try {
			helper.setFrom(fromMail, domainName);
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

	@Async("threadExecutor")
	public void sendMailToCreator(String creatorName, String creatorMail, String contractName, String versionNumber,
			String envelopeId, String flowType, String moduleName) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		String toAddress = creatorMail;
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{envelopId}}", envelopeId);
		url = url.replace("{{moduleName}}", moduleName.toLowerCase());
		String subject = Constant.REVIEWER_APPROVED_SUBJECT;
		subject = subject.replace("{{moduleName}}", moduleName.toUpperCase());
		subject = subject.replace("{{contractName}}", contractName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-reviewer-approved.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{contractName}}", contractName);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{moduleName}}", moduleName.toUpperCase());
		content = content.replace("{{versionNumber}}", versionNumber);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		try {
			helper.setFrom(fromMail, domainName);
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
	@Async("threadExecutor")
	public void sendMailToReviewer(String email, String creatormail, String creatorName, String contractName,
			String moduleName, String flowType, String latestversion, String envelopeId) throws MessagingException,
			TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		String toAddress = creatormail;
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{envelopId}}", envelopeId);
		url = url.replace("{{moduleName}}", moduleName.toLowerCase());
		String subject = "All Reviews Complete";
		String mailContent;
		Map<String, Object> model = new HashMap<>();
		Template content = config.getTemplate("clm-sent-for-signing.html");
		mailContent = FreeMarkerTemplateUtils.processTemplateIntoString(content, model);
		mailContent = mailContent.replace("{{redirectUrl}}", url);
		mailContent = mailContent.replace("{{contractName}}", contractName);
		mailContent = mailContent.replace("{{creatorName}}", creatorName);
		mailContent = mailContent.replace("{{moduleName}}", moduleName);
		mailContent = mailContent.replace("{{orgName}}", orgName);
		mailContent = mailContent.replace("{{versionNumber}}", latestversion);
		mailContent = mailContent.replace("{{mediaHost}}", mediaHost);
		mailContent = mailContent.replace("{{imageKey}}", imageKey);
		mailContent = mailContent.replace("{{supportEmail}}", supportMail);

		helper.setFrom(fromMail, domainName);
		helper.setTo(toAddress);
		helper.setSubject(subject);
		helper.setText(mailContent, true);
		mailSender.send(helper.getMimeMessage());

	}

	@Async("threadExecutor")
	public void sendMailToNextReviewer(String envelopeId, String reviewerEmail, String reviewerName,
			String contractName, String versionNumber, String creatorEmail, String flowType, String moduleName)
			throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException,
			TemplateException, MessagingException {
		String toAddress = reviewerEmail;
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{envelopId}}", envelopeId);
		url = url.replace("{{moduleName}}",moduleName.toLowerCase().concat("-review"));
		String subject = Constant.REVIEWER_REQUEST_SUBJECT;
		subject = subject.replace("{{moduleName}}", moduleName.toUpperCase());
		subject = subject.replace("{{contractName}}", contractName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-request-for-review.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{contractName}}", contractName);
		content = content.replace("{{reviewerName}}", reviewerName);
		content = content.replace("{{moduleName}}", moduleName.toUpperCase());
		content = content.replace("{{versionNumber}}", versionNumber);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{creatorEmail}}", creatorEmail);
		content = content.replace("{{orgName}}", senderName);

		try {
			helper.setFrom(fromMail, domainName);
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

	@Async("threadExecutor")
	public void sendCommentMail(PostCommentMailBody emailBody) throws MessagingException, UnsupportedEncodingException {
		MimeMessage message = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			String subject = "{contractName} {moduleName}: Review Feedback on {documentName} and version {versionNumber}";
			subject = subject.replace("{contractName}", emailBody.getContractName());
			subject = subject.replace("{documentName}", emailBody.getDocumentName());
			subject = subject.replace("{moduleName}", emailBody.getModuleName());
			subject = subject.replace("{versionNumber}",emailBody.getVersion());
			Template t = null;
			t = config.getTemplate("clm-reviewer-comment.html");
			String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, null);
			content = content.replace("{{mediaHost}}", mediaHost);
			content = content.replace("{{imageKey}}", imageKey);
			content = content.replace("{{orgName}}", senderName);
			content = content.replace("{{supportEmail}}", supportMail);

			String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
			url = url.replace("{{host}}", host);
			url = url.replace("{{envelopId}}", emailBody.getEnvelopeId());
			url = url.replace("{{moduleName}}", emailBody.getModuleName().toLowerCase());

			content = content.replace("{{creatorName}}", emailBody.getCreatorName());
			content = content.replace("{{contractName}}", emailBody.getContractName());
			content = content.replace("{{moduleName}}", emailBody.getModuleName());
			content = content.replace("{{documentName}}", emailBody.getDocumentName());
			content = content.replace("{{versionNumber}}", emailBody.getVersion());
			content = content.replace("{{comments}}", emailBody.getComment());
			content = content.replace("{{redirectUrl}}", url);
			helper.setTo(emailBody.getCreatorMail());
			helper.setText(content, true);
			helper.setFrom(fromMail, domainName);
			helper.setSubject(subject);
			mailSender.send(message);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException | IOException | TemplateException ex) {
			throw new MessagingException(ex.getMessage());
		}
	}

	
    @Async("threadExecutor")
	public void sendDocumentVersionCommentMail(PostCommentMailBody emailBody) throws MessagingException, UnsupportedEncodingException {
		MimeMessage message = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			String subject = "{contractName} {moduleName}: - Revised Version {versionNumber} for Your Review";
			subject = subject.replace("{contractName}", emailBody.getContractName());
			subject = subject.replace("{moduleName}", emailBody.getModuleName().toUpperCase());
			subject = subject.replace("{versionNumber}", "1."+emailBody.getVersion());
			Template t = null;
			t = config.getTemplate("document-version-mail.html");
			String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, null);
			content = content.replace("{{mediaHost}}", mediaHost);
			content = content.replace("{{imageKey}}", imageKey);
			content = content.replace("{{orgName}}", senderName);
			content = content.replace("{{supportEmail}}", supportMail);

			String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
			url = url.replace("{{host}}", host);
			url = url.replace("{{envelopId}}", emailBody.getEnvelopeId());
			url = url.replace("{{moduleName}}", emailBody.getModuleName().toLowerCase()+"-review");

			content = content.replace("{{reviewerName}}",StringUtils.capitalize(emailBody.getCreatorName()));
			content = content.replace("{{contractName}}", emailBody.getContractName());
			content = content.replace("{{moduleName}}", emailBody.getModuleName());
			content = content.replace("{{versionNumber}}","1."+emailBody.getVersion());
			content = content.replace("{{redirectUrl}}", url);
			helper.setTo(emailBody.getCreatorMail());
			helper.setText(content, true);
			helper.setFrom(fromMail, domainName);
			helper.setSubject(subject);
			mailSender.send(message);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException | IOException | TemplateException ex) {
			throw new MessagingException(ex.getMessage());
		}
	}

	@Async("threadExecutor")
	public void sendMailToWatchers(String creatorName,String contractName, String creatorMail,
			String envelopeId,String flowType,String moduleName) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		String url = Constant.MAIL_DRILLDOWN_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{envelopId}}", envelopeId);
		url = url.replace("{{moduleName}}", moduleName.toLowerCase());
		String subject = "Internal Signing Completed - Document Sent to Vendors for Signing";
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("watchers-mail.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{contractName}}", contractName);
		content = content.replace("{{moduleName}}", moduleName.toUpperCase());
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		try {
			helper.setFrom(fromMail, domainName);
			helper.setTo(creatorMail);
			helper.setSubject(subject);
			helper.setText(content, true);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException e) {
			throw new MessagingException(e.getMessage());
		}
		mailSender.send(message);
	}

	@Async("threadExecutor")
	public void sendMailForNewUserRequest(String creatorName, String flowType, List<String> roles, String currentRole) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		String url = "{{host}}/auth/login?redirect=/{{flowType}}/user-management";
		url = url.replace("{{host}}", host);
		url = url.replace("{{flowType}}", flowType.toLowerCase());
		String subject = Constant.CLM_NEW_USER_REQUESTING_SUBJECT;
		subject = subject.replace("{{requestedUserName}}", creatorName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-new-request.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{roles}}", roles.toString());
	
		String adminRole = null;
		if(currentRole.equalsIgnoreCase(Constant.PCD_ADMIN) || currentRole.equalsIgnoreCase(Constant.COMMERCIAL_ADMIN) || currentRole.equalsIgnoreCase(Constant.LEGAL)){
			adminRole = Constant.SUPER_ADMIN;
			content = content.replace("{{To}}",Constant.SUPER_ADMIN );
		}else {
			if (flowType.equalsIgnoreCase(Constant.PCD)) {
				adminRole = Constant.PCD_ADMIN;
				content = content.replace("{{To}}",Constant.PCD_ADMIN );
			} else {
				adminRole = Constant.COMMERCIAL_ADMIN;
				content = content.replace("{{To}}",Constant.COMMERCIAL_ADMIN );
			}
		}
		List<String> adminEmails = userInfoRespository.findByCurrentRole(adminRole).stream().map(UsersInfoDocument::getEmail).collect(Collectors.toList());
		for (String email : adminEmails) {
			try {
				helper.setFrom(fromMail, domainName);
				helper.setTo(email);
				helper.setSubject(subject);
				helper.setText(content, true);
			} catch (UnsupportedEncodingException e) {
				throw new UnsupportedEncodingException(e.getMessage());
			} catch (MessagingException e) {
				throw new MessagingException(e.getMessage());
			}
			mailSender.send(message);
		}
	}

	@Async("threadExecutor")
	public void sendMailToNewUserAddedByAdmin(String toAddress, String creatorName, String flowType, List<String> roles) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		String url = Constant.USER_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{flowType}}", flowType.toLowerCase());
		String subject = Constant.CLM_NEW_USER_ADDED_SUBJECT;
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-user-creation-admin.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{moduleName}}", roles.toString());
		try {
			helper.setFrom(fromMail, domainName);
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

	@Async("threadExecutor")
	public void sendMailToRequestedUserForStatus(String toAddress, String subject, String creatorName, String flowType, List<String> roles, String status, String message) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-status-request.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{status}}", status);
		content = content.replace("{{message}}", message);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", host);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{moduleName}}", roles.toString());
		content = content.replace("{{flowType}}", flowType);
		try {
			helper.setFrom(fromMail, domainName);
			helper.setTo(toAddress);
			helper.setSubject(subject);
			helper.setText(content, true);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException e) {
			throw new MessagingException(e.getMessage());
		}
		mailSender.send(mimeMessage);
	}

	@Async("threadExecutor")
	public void sendMailToUserRoleUpdated(String toAddress, String creatorName, String flowType, List<String> roles) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		String url = Constant.USER_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{flowType}}", flowType.toLowerCase());
		String subject = Constant.CLM_USER_ROLES_UPDATED_SUBJECT;
		subject = subject.replace("{{nameOfUser}}", creatorName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-update-role.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{moduleName}}", roles.toString());
		try {
			helper.setFrom(fromMail, domainName);
			helper.setTo(toAddress);
			helper.setSubject(subject);
			helper.setText(content, true);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException e) {
			throw new MessagingException(e.getMessage());
		}
		mailSender.send(mimeMessage);
	}

	@Async("threadExecutor")
	public void sendMailToUserIsActivated(String toAddress, String creatorName, String flowType, List<String> roles) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		String url = Constant.USER_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{flowType}}", flowType.toLowerCase());
		String subject = Constant.CLM_USER_ACTIVATED_SUBJECT;
		subject = subject.replace("{{nameOfUser}}", creatorName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-reactivate-account.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{moduleName}}", roles.toString());
		try {
			helper.setFrom(fromMail, domainName);
			helper.setTo(toAddress);
			helper.setSubject(subject);
			helper.setText(content, true);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException e) {
			throw new MessagingException(e.getMessage());
		}
		mailSender.send(mimeMessage);
	}

	@Async("threadExecutor")
	public void sendMailToUserIsDeactivated(String toAddress, String creatorName, String flowType, List<String> roles,String admin) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		String url = Constant.USER_REDIRECT_URL;
		url = url.replace("{{host}}", host);
		url = url.replace("{{flowType}}", flowType.toLowerCase());
		String subject = Constant.CLM_USER_ACTIVATED_SUBJECT;
		subject = subject.replace("{{nameOfUser}}", creatorName);
		Map<String, Object> model = new HashMap<>();
		Template t = config.getTemplate("clm-deactivate-account.html");
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
		content = content.replace("{{creatorName}}", creatorName);
		content = content.replace("{{mediaHost}}", mediaHost);
		content = content.replace("{{imageKey}}", imageKey);
		content = content.replace("{{redirectUrl}}", url);
		content = content.replace("{{supportEmail}}", supportMail);
		content = content.replace("{{orgName}}", senderName);
		content = content.replace("{{moduleName}}", roles.toString());
		content = content.replace("{{admin}}", admin);
		try {
			helper.setFrom(fromMail, domainName);
			helper.setTo(toAddress);
			helper.setSubject(subject);
			helper.setText(content, true);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException(e.getMessage());
		} catch (MessagingException e) {
			throw new MessagingException(e.getMessage());
		}
		mailSender.send(mimeMessage);
	}

}
