package saaspe.clm.service;

import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.CurrentUserRoleUpdateRequest;
import saaspe.clm.model.EditUserRolesRequest;

import java.util.List;

public interface UserInfoService {

	CommonResponse activateUser(String uniqueId, String email);

	CommonResponse deActivateUser(String uniqueId, String email);

	CommonResponse userListView(int page, int limit, String searchText, String email, String order, String orderBy,
			Boolean isActive, String createdThrough, List<String> division);

	CommonResponse editUserRoles(EditUserRolesRequest editUserRolesRequest, String email);

	CommonResponse updateCurrentUserRoles(CurrentUserRoleUpdateRequest userRoleUpdateRequest, String email);

	CommonResponse getCurrentUserRoles(String email);

	CommonResponse getUserRoles(String email);

	CommonResponse editUserDivisionAndRoles(EditUserRolesRequest editUserRolesRequest, String email);
}
