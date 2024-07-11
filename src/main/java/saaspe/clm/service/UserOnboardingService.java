package saaspe.clm.service;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.validation.Valid;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.*;

public interface UserOnboardingService {

	CommonResponse userOnboarding(@Valid UserOnboardingRequest userOnboardingRequest, String email,String name) throws DataValidationException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException;

	CommonResponse addUserDetailsByAdmin(@Valid UserOnboardingRequest userOnboardingRequest, String email) throws DataValidationException;

	CommonResponse approveUser(@Valid UserApprovalRequest userApprovalRequest, String email,String name) throws DataValidationException;

	CommonResponse getOnboardedUsersListView(int page, int limit, String searchText, String email, String order,
			String orderBy, String createdThrough, String division, String status);

	CommonResponse editUserRoles(EditUserRolesRequest editUserRolesRequest, String email) ;

	CommonResponse rejectionUser(@Valid UserRejectRequest userRejectRequest, String email, String name) throws DataValidationException;

}
