package saaspe.clm.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import saaspe.clm.constant.Constant;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.*;
import saaspe.clm.service.UserInfoService;

@RestController
@RequestMapping("/api/user")
public class UsersInfoController {

	@Autowired
	private UserInfoService userInfoService;

	@PostMapping("/activate")
	public ResponseEntity<CommonResponse> activateUser(@RequestParam String uniqueId, Authentication authentication,
			HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.activateUser(uniqueId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping("/de-activate")
	public ResponseEntity<CommonResponse> deActivateUser(@RequestParam String uniqueId, Authentication authentication,
			HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.deActivateUser(uniqueId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/onboarded/get/user-list")
	public ResponseEntity<CommonResponse> userListView(Authentication authentication, HttpServletRequest request,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "name") String orderBy,
			@RequestParam(required = false) Boolean isActive, @RequestParam(required = false) String createdThrough,
			@RequestParam(required = false) List<String> division) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.userListView(page, limit, searchText, email, order, orderBy, isActive,
						createdThrough, division);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/onboarded/update-roles")
	public ResponseEntity<CommonResponse> editUserRoles(@RequestBody EditUserRolesRequest EditUserRolesRequest,
			HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.editUserRoles(EditUserRolesRequest, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EditUserRoleResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/update/current-role")
	public ResponseEntity<CommonResponse> updateCurrentUserRoles(@RequestBody CurrentUserRoleUpdateRequest userRoleUpdateRequest,
														HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.updateCurrentUserRoles(userRoleUpdateRequest, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Update User Current Role Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/fetch/current-role")
	public ResponseEntity<CommonResponse> getCurrentUserRoles(HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.getCurrentUserRoles(email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Update User Current Role Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/fetch/user-roles")
	public ResponseEntity<CommonResponse> getUserRoles(HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.getUserRoles(email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Update User Current Role Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/onboarded/update-division-roles")
	public ResponseEntity<CommonResponse> editUserDivision(@RequestBody EditUserRolesRequest editUserRolesRequest,
														HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userInfoService.editUserDivisionAndRoles(editUserRolesRequest, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EditUserRoleResponse", new ArrayList<>()), e.getMessage()));
		}
	}
}
