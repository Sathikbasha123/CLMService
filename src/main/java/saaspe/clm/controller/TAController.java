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
import saaspe.clm.service.LOAService;
import saaspe.clm.service.TAService;

@RestController
@ControllerLogging
@RequestMapping("/api/v1/ta")
public class TAController {

	@Autowired
	private TAService taService;

	@Autowired
	private LOAService loaService;

	private static final Logger log = LoggerFactory.getLogger(TAController.class);

	@PostMapping(value = "/addTaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> addTaDocument(
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
				applicationDetailsResponse = taService.addTaDocument(json, createDocumentFiles, createId,
						deleteId, envelopeId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addTADocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addTADocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addTADocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addTADocument", new ArrayList<>()), e.getMessage()));
		}
	}
	
	
	@PostMapping(value = "/newAddTaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> newAddTaDocument(
			@RequestParam(value = "body", required = true) String json,
			@RequestParam(value = "templateId", required = true) String envelopeId, Authentication authentication,
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
				applicationDetailsResponse = taService.newAddTaDocument(json,envelopeId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addTADocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addTADocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addTADocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addTADocument", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@GetMapping("/detail")
	public ResponseEntity<CommonResponse> getTaContractDocumenttDetailsView(HttpServletRequest request,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(name = "envelopeId") String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = taService.getTaDocumentDetailsView(envelopeId,request);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getTAContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getDetailsViewofTaContractDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getTaContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofTaContractDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping(value = "/get/document/versions")
	public ResponseEntity<CommonResponse> getDocumentVersions(HttpServletRequest request,
			@RequestParam(value = "envelopeId", required = true) String envelopeId) {
		try {
			CommonResponse commonResponse = taService.getDocumentVersions(request,envelopeId);
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

	@PutMapping(value = "/updateTaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
				applicationDetailsResponse = loaService.updateLoaDocument(createDocumentFiles, updateDocumentFiles,
						update_id, delete_id, envelopeId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addTaDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("updateClmContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addTaDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("updateClmContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/create/list")
	public ResponseEntity<CommonResponse> getCreateListTa(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "20") int limit, @RequestParam(required = false) String searchText, @RequestParam(required = false) String order, @RequestParam(required = false, defaultValue = "projectName") String orderBy, @RequestParam(required = false) String subsidiary, @RequestParam(required = false) String status
			, @RequestParam(required = false) String signingStatus,@RequestParam(required = false) String category) {
		try {
			CommonResponse applicationDetailsResponse = taService.getCreateListForTa(request, page, limit, searchText, order, orderBy, subsidiary, status,signingStatus,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListTa method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND, new Response("getCreateListTa", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListTa method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("getCreateListTa", new ArrayList<>()), e.getMessage()));
		}
	}

	@Scheduled(cron = "0 0 0 */1 * *")
	@GetMapping("/get-expiring-documents")
	public CommonResponse updateStatus()
	{
		log.info("Updating expiring TA Documents");
		try {
			return taService.setExpiringEnvelope();
		} catch (Exception e) {
			log.error("*** Ending expiringDocument with an error ***", e);
			e.printStackTrace();
			return null;
		}
	}
}
