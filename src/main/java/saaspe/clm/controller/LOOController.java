package saaspe.clm.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import saaspe.clm.aspect.ControllerLogging;
import saaspe.clm.constant.Constant;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.LOOService;

@RestController
@ControllerLogging
@RequestMapping("/api/v1/loo")
public class LOOController {

	@Autowired
	LOOService looservice;

	private static final Logger log = LoggerFactory.getLogger(LOOController.class);

	@PostMapping(value = "/addLooContractDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> addLoaContractDocument(
			@RequestParam(value = "body", required = true) String json,
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			@RequestParam(value = "create_id", required = false) String[] createId,
			@RequestParam(value = "delete_id", required = false) String[] deleteId,
			@RequestParam(value = "envelopeId", required = true) String envelopeId, Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			HttpServletRequest request) {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse applicationDetailsResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				String name = jwt.getClaim("name").asString();
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				applicationDetailsResponse = looservice.addLooContractDocument(json, createDocumentFiles, createId,
						deleteId, envelopeId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addLooContract method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addLooContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addLooContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addLooContract", new ArrayList<>()), e.getMessage()));
		}
	}
	
	
	
	@PostMapping(value = "/newAddLooContractDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> addLoaContractDocument(
			@RequestParam(value = "body", required = true) String json,
			@RequestParam(value = "templateId", required = true) String templateId, Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			HttpServletRequest request) {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse applicationDetailsResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				String name = jwt.getClaim("name").asString();
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				applicationDetailsResponse = looservice.newAddLooContractDocument(json, templateId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addLooContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addLooContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addLooContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addLooContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping("/cmuDocument-to-envelope/upload")
	public ResponseEntity<CommonResponse> cmuTemplateDocumentUploadToEnvelope(@RequestParam(required = true) String envelopeId,
			@RequestParam(required = true) String templateId, @RequestParam(value = "templateDocument",required = true) String docRequest,
			@RequestParam(required = true) String envelopeDocumentId,
			@RequestParam(value = "flowType", required = true) String flowType,HttpServletRequest request) {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
			}
			CommonResponse commonResponse = looservice.cmutemplateDocumentUploadToEnvelope(envelopeId, templateId,
					docRequest, envelopeDocumentId,flowType,email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending templateDocumentUploadToEnvelope method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponce", new ArrayList<>()), e.getMessage()));
		}

	}
	
	@GetMapping("/get/approved/LOO-list")
	public ResponseEntity<CommonResponse> getApprovedCMUList(HttpServletRequest request) {
		try {
			CommonResponse applicationDetailsResponse = looservice.getApprovedLOOContractList(request);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending approved LOO List method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("approved LOO ListResponse", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@GetMapping("/detail")
	public ResponseEntity<CommonResponse> getLooContractDocumenttDetailsView(Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(name = "envelopeId") String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = looservice.getLooDocumentDetailsView(envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getLooContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getDetailsViewofLooContractDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLooContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofLooContractDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping(value = "/updateLoaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> clmUpdateDocument(
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
				applicationDetailsResponse = looservice.updateLooDocument(createDocumentFiles, updateDocumentFiles,
						update_id, delete_id, envelopeId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending updateLooDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("updateLooContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending updateLooDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("updateLooContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping(value = "/get/document/versions")
	public ResponseEntity<CommonResponse> getDocumentVersions(HttpServletRequest request,
															  @RequestParam(value = "envelopeId", required = true) String envelopeId) {
		try {
			CommonResponse commonResponse = looservice.getDocumentVersions(request,envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getDocumentVersions method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("GetDocumentVersionsResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending createTemplate method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("GetDocumentVersionResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/create/list")
	public ResponseEntity<CommonResponse> getCreateListLoo(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "20") int limit, @RequestParam(required = false) String searchText, @RequestParam(required = false) String order, @RequestParam(required = false, defaultValue = "projectName") String orderBy, @RequestParam(required = false) String subsidiary, @RequestParam(required = false) String status
			,@RequestParam(required = false) String category) {
		try {
			CommonResponse applicationDetailsResponse = looservice.getCreateListForLoo(request, page, limit, searchText, order, orderBy, subsidiary, status,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListLoo method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(e.getStatusCode(), new Response("getCreateListLoo", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListLoo method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("getCreateListLoo", new ArrayList<>()), e.getMessage()));
		}
	}

	@Scheduled(cron = "0 0 0 */1 * *")
	@GetMapping("/get-expiring-documents")
	public CommonResponse updateStatus()
	{
		log.info("Updating expiring LOO Documents");
		try {
			return looservice.setExpiringEnvelope();
		} catch (Exception e) {
			log.error("*** Ending expiringDocument with an error ***", e);
			e.printStackTrace();
			return null;
		}
	}
}
