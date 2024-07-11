package saaspe.clm.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
import saaspe.clm.service.UserOnboardingService;

@RestController
@RequestMapping("/api/user/onboarding")
public class UserOnboardingController {

	@Autowired
	private UserOnboardingService userOnboardingService;

	@PostMapping("/request")
	public ResponseEntity<CommonResponse> addUserOnboardingDetails(
			@Valid @RequestBody UserOnboardingRequest userOnboardingRequest, Authentication authentication,
			HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				String name = jwt.getClaim("name").asString();
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userOnboardingService.userOnboarding(userOnboardingRequest, email,name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (DataValidationException e) {
			e.printStackTrace();
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping("/portal")
	public ResponseEntity<CommonResponse> addUserDetailsByAdmin(
			@Valid @RequestBody UserOnboardingRequest userOnboardingRequest, Authentication authentication,
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
				commonResponse = userOnboardingService.addUserDetailsByAdmin(userOnboardingRequest, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping("/approve")
	public ResponseEntity<CommonResponse> approveUser(@Valid @RequestBody UserApprovalRequest userApprovalRequest,
			Authentication authentication, HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				String name = jwt.getClaim("name").asString();
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userOnboardingService.approveUser(userApprovalRequest, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/get/user-list")
	public ResponseEntity<CommonResponse> getOnboardedUsersListView(Authentication authentication,
			HttpServletRequest request,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "name") String orderBy,
			@RequestParam(required = false) String createdThrough, @RequestParam(required = false) String division,@RequestParam(required = false) String status)
			throws DataValidationException {
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
				commonResponse = userOnboardingService.getOnboardedUsersListView(page, limit, searchText, email, order,
						orderBy, createdThrough, division, status);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/update-roles")
	public ResponseEntity<CommonResponse> updateUserRoles(
			@Valid @RequestBody EditUserRolesRequest editUserRolesRequest, Authentication authentication,
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
				commonResponse = userOnboardingService.editUserRoles(editUserRolesRequest, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@PostMapping("/reject")
	public ResponseEntity<CommonResponse> rejectUser(@Valid @RequestBody UserRejectRequest userRejectRequest,
													  Authentication authentication, HttpServletRequest request) throws DataValidationException {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse commonResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				String name = jwt.getClaim("name").asString();
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				commonResponse = userOnboardingService.rejectionUser(userRejectRequest, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("useronboardRequestResponse", new ArrayList<>()), e.getMessage()));
		}
	}

}
