package saaspe.clm.service;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import freemarker.template.TemplateException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;

public interface UserDetailsService {

	CommonResponse getProfile(HttpServletRequest request) throws DataValidationException;

	CommonResponse getUserAccessAndRole(HttpServletRequest request, String xAuthProvider);

	CommonResponse sendConsentToUser(HttpServletRequest request, String header)
			throws DataValidationException, IOException, TemplateException, MessagingException;

	CommonResponse deleteUserfromredis();

	CommonResponse deleteUserfromredisss();

	String deleteUserfromredisss(String userEmail);

	String getAzureUserfromredisss(String userEmail);

}
