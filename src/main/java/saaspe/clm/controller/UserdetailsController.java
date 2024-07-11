package saaspe.clm.controller;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.UserDetailsService;

@RequestMapping("/api")
@RestController
public class UserdetailsController {

	@Autowired
	private UserDetailsService userDetailsService;

	@GetMapping("/v1/user/details/profile")
	// @PreAuthorize("hasAuthority('VIEW_USER')")
	public ResponseEntity<CommonResponse> getProfile(HttpServletRequest request) {
		try {
			CommonResponse userData = userDetailsService.getProfile(request);
			return ResponseEntity.ok(userData);
		} catch (DataValidationException dve) {
			dve.printStackTrace();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("ProfileResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/userprofile/access")
	public ResponseEntity<CommonResponse> userAccessAndRoles(HttpServletRequest request,
			Authentication authentication) {
		try {
			CommonResponse response = userDetailsService.getUserAccessAndRole(request,
					request.getHeader("X-Auth-Provider"));
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("AccessResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/userprofile/consent-email/to-user")
	public ResponseEntity<CommonResponse> sendConsentToUser(HttpServletRequest request) {
		CommonResponse response = new CommonResponse();
		try {
			response = userDetailsService.sendConsentToUser(request, request.getHeader("X-Auth-Provider"));
			return ResponseEntity.ok(response);
		} catch (DataValidationException e) {
			e.printStackTrace();
			return ResponseEntity.ok(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("UserConsentResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("UserConsentResponse", new ArrayList<>()), e.getMessage()));

		}
	}

	//@Scheduled(cron = "0 0 0 * * *")
	public ResponseEntity<CommonResponse> deleteUserfromredis() {
		try {
			CommonResponse commonResponse = userDetailsService.deleteUserfromredis();
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("deleteUserfromredisResponce", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/delete-user")
	public ResponseEntity<CommonResponse> deleteUserfromredisss() {
		try {
			CommonResponse commonResponse = userDetailsService.deleteUserfromredisss();
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("deleteUserfromredisResponce", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/delete/user")
	public String deleteUserfromredis1(@RequestParam String userEmail) {
		try {
			String commonResponse = userDetailsService.deleteUserfromredisss(userEmail);
			return commonResponse;
		} catch (Exception e) {
			e.printStackTrace();
			return "User deletion failed";
		}
	}

	@GetMapping("/azure/user")
	public String getAzureCache(@RequestParam String userEmail) {
		try {
			String commonResponse = userDetailsService.getAzureUserfromredisss(userEmail);
			return commonResponse;
		} catch (Exception e) {
			e.printStackTrace();
			return "Azure user deletion failed";
		}
	}

}
