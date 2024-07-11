package saaspe.clm.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import saaspe.clm.aspect.ControllerLogging;
import saaspe.clm.custom.CustomUserDetails;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.CMUService;

@RestController
@ControllerLogging
@RequestMapping("/api/v1/cmu")
public class CMUController {

	@Autowired
	CMUService cmuService;

	private static final Logger log = LoggerFactory.getLogger(CMUController.class);

	@PostMapping(value = "/newAddCmuDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> addClmContract(@RequestParam(value = "body", required = true) String json,
			@RequestParam(value = "templateId", required = true) String templateId, Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			HttpServletRequest request) {
		try {
			String email = null;
			String provider = request.getHeader("X-auth-provider");
			String token = request.getHeader("Authorization");
			CommonResponse applicationDetailsResponse = null;
			if (provider != null && provider.equalsIgnoreCase("azure")) {
				DecodedJWT jwt = JWT.decode(token.replace("Bearer ", ""));
				String name = jwt.getClaim("name").asString();
				if (jwt.getClaim("email").asString() != null) {
					email = jwt.getClaim("email").asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				applicationDetailsResponse = cmuService.newAddCmuDocument(json,
						templateId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		}
		catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addCmuDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addCmuDocument", new ArrayList<>()), e.getMessage()));
		} 
		catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addCmuDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addCmuDocument", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@PutMapping(value = "/updateCmuDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> cmuUpdateDocument(
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			@RequestPart(value = "update-document-file", required = false) MultipartFile[] updateDocumentFiles,
			@RequestParam(value = "update_id", required = false) String[] update_id,
			@RequestParam(value = "delete_id", required = false) String[] delete_id,
			@RequestParam(value = "envelopeId", required = true) String envelopeId, Authentication authentication,
			HttpServletRequest request) {
		try {
			String email = null;
			String provider = request.getHeader("X-auth-provider");
			String token = request.getHeader("Authorization");
			CommonResponse applicationDetailsResponse = null;
			if (provider != null && provider.equalsIgnoreCase("azure")) {
				DecodedJWT jwt = JWT.decode(token.replace("Bearer ", ""));
				if (jwt.getClaim("email").asString() != null) {
					email = jwt.getClaim("email").asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				applicationDetailsResponse = cmuService.updateCmuDocument(createDocumentFiles, updateDocumentFiles,
						update_id, delete_id, envelopeId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending updateCmuDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("updateCmuDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending updateCmuDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("updateCmuDocument", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@GetMapping("/get/approved/CMUContract-list")
	public ResponseEntity<CommonResponse> getApprovedCMUList(HttpServletRequest request) {
		try {
			CommonResponse applicationDetailsResponse = cmuService.getApprovedCMUContractList(request);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending approvedCMUList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("approvedCMUListResponse", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@GetMapping("/detail")
	public ResponseEntity<CommonResponse> getCMUContractDocumenttDetailsView(Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(name = "envelopeId") String envelopeId) {
		try {
			CustomUserDetails profile = (CustomUserDetails) authentication.getPrincipal();
			String email = profile.getEmail();
			CommonResponse applicationDetailsResponse = cmuService.getCMUContractDocumentDetailsView(envelopeId,email);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCMUContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofCMUContractDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCMUContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofCMUContractDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping(value = "/get/document/versions")
	public ResponseEntity<CommonResponse> getDocumentVersions(HttpServletRequest request, @RequestParam(value = "envelopeId", required = true) String envelopeId) {
		try {
			CommonResponse commonResponse = cmuService.getDocumentVersions(request,envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getDocumentVersions method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("GetDocumentVersionsResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending createTemplate method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("GetDocumentVersionResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/get/approved/cmu-list")
	public ResponseEntity<CommonResponse> getApprovedLoaAllList(HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "projectId") String orderBy,
			@RequestParam(required = false) String subsidiary, @RequestParam(required = false) String status) {
		try {
			CommonResponse applicationDetailsResponse = cmuService.getApprovedCmuAllList(request, page, size,searchText,order,orderBy,subsidiary,status);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending approvedCmuAllListResponse method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("approvedLoaAllListResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/create/list")
	public ResponseEntity<CommonResponse> getCreateListCmu(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "20") int limit, @RequestParam(required = false) String searchText, @RequestParam(required = false) String order, @RequestParam(required = false, defaultValue = "projectName") String orderBy, @RequestParam(required = false) String subsidiary, @RequestParam(required = false) String status
			,@RequestParam(required = false) String category) {
		try {
			CommonResponse applicationDetailsResponse = cmuService.getCreateListForCmu(request, page, limit, searchText, order, orderBy, subsidiary, status,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListCmu method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("getCreateListCmu", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListCmu method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("getCreateListCmu", new ArrayList<>()), e.getMessage()));
		}
	}

}
