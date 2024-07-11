package saaspe.clm.controller;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
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
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.LOAService;

@RestController
@ControllerLogging
@RequestMapping("/api/v1/loa")
public class LOAController {

	@Autowired
	private LOAService loaService;

	private static final Logger log = LoggerFactory.getLogger(LOAController.class);

	@PostMapping(value = "/addLoaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> addClmContract(@RequestParam(value = "body", required = true) String json,
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			@RequestParam(value = "create_id", required = false) String[] createId,
			@RequestParam(value = "delete_id", required = false) String[] deleteId,
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
				applicationDetailsResponse = loaService.addLoaDocument(json, createDocumentFiles, createId, deleteId,
						templateId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addLoaDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addLoaDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addLoaDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addLoaDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping(value = "/newAddLoaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> newAddLoaDocument(@RequestParam(value = "body", required = true) String json,
			Authentication authentication, @RequestParam(value = "templateId", required = true) String templateId,
			@RequestParam(value = "fileCount", required = true) String fileCount,
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
				applicationDetailsResponse = loaService.newaddLoaDocument(json, templateId, email, name, fileCount);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addLoaDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addLoaDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addLoaDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addLoaDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping(value = "/update/LoaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> clmUpdateDocument(
			@RequestPart(value = "create-document-file", required = true) MultipartFile createDocumentFiles,
			@RequestParam(value = "create_id", required = false) String create_id,
			@RequestParam(value = "envelopeId", required = true) String envelopeId, 
			@RequestParam(value = "versionOrder", required=true) int versionOrder,Authentication authentication,
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
				applicationDetailsResponse = loaService.updateLoaDocument(createDocumentFiles, create_id, envelopeId,versionOrder,
						email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		}  catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending clmUpdateDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("clmUpdateDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping(value = "/delete/loaDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> deleteandUploadOldDocToBlob(
			@RequestParam(value = "envelopeId", required = true) String envelopeId, Authentication authentication,
			@RequestParam(value = "delete_id", required = false) String[]  delete_id,
			@RequestParam(value = "existing_id", required = false) String[]  existing_id, 
			@RequestParam(value = "versionOrder", required=true) int versionOrder,
			@RequestParam(value = "existing_file_names", required=false) List<String> existing_names,
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
				applicationDetailsResponse = loaService.deleteandUploadOldDocToBlob(delete_id, existing_id,existing_names, envelopeId,versionOrder,email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending deleteandUploadOldDocToBlob method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("deleteandUploadOldDocToBlob", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending deleteandUploadOldDocToBlob method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("deleteandUploadOldDocToBlob", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/list")
	public ResponseEntity<CommonResponse> getListOfLoaContract(Authentication authentication,
			HttpServletRequest request,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit, @RequestParam(required = false) String status,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "contractName") String orderBy) {
		try {
			CommonResponse applicationDetailsResponse = loaService.getListOfLoaDocument(page, limit, request, status,
					searchText, order, orderBy);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getListOfLoaDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getListOfLoaDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getListOfLoaDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getListOfLoaDocument", new ArrayList<>()), e.getMessage()));
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
				applicationDetailsResponse = loaService.updateLoaDocument(createDocumentFiles, updateDocumentFiles,
						update_id, delete_id, envelopeId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addLoaDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addLoaDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping(value = "/get/document/versions")
	public ResponseEntity<CommonResponse> getDocumentVersions(HttpServletRequest request,
			@RequestParam(value = "envelopeId", required = true) String envelopeId) {
		try {
			CommonResponse commonResponse = loaService.getDocumentVersions(request, envelopeId);
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

	@GetMapping("/detail")
	public ResponseEntity<CommonResponse> getLoaDocumenttDetailsView(Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = loaService.getLoaDocumentDetailsView(envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getLoaDocumentDetailsView method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getDetailsViewofLoaDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLoaDocumentDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofLoaDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/getLoa")
	public ResponseEntity<CommonResponse> getLoaDetail(Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = loaService.getLoaDocumentDetail(envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getLoaDocumentDetail method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getDetailOfLoaDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLoaDocumentDetail method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailOfLoaDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/create/list")
	public ResponseEntity<CommonResponse> getCreateListLoa(HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "projectName") String orderBy,
			@RequestParam(required = false) String subsidiary, @RequestParam(required = false) String status,
			@RequestParam(required = false) String category) {
		try {
			
			var check = SecurityContextHolder.getContext().getAuthentication();
			log.info("check {}",check);
			CommonResponse applicationDetailsResponse = loaService.getCreateListForLoa(request, page, limit, searchText,
					order, orderBy, subsidiary, status,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListLoa method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getCreateListLoa", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListLoa method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getCreateListLoa", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@Scheduled(cron = "0 0 0 */1 * *")
	@GetMapping("/get-expiring-documents")
	public CommonResponse updateStatus()
	{
		log.info("Updating expiring status");
		try {
			return loaService.setExpiringEnvelope();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending expiringDocument with an error ***", e);
			e.printStackTrace();
			return null;
		}
	}
}
