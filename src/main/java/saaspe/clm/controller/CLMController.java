package saaspe.clm.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import saaspe.clm.aspect.ControllerLogging;
import saaspe.clm.constant.Constant;
import saaspe.clm.custom.CustomUserDetails;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.model.SendDocumentPdfMail;
import saaspe.clm.service.CLMService;

@RestController
@ControllerLogging
@RequestMapping("/api/v1/clm")
public class CLMController {

	@Value("${app.jwt.expiration.min}")
	public String clientId;

	@Autowired
	private CLMService clmService;

	private static final Logger log = LoggerFactory.getLogger(CLMController.class);

	@PostMapping(value = "/addcontract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
				applicationDetailsResponse = clmService.addClmContract(json, createDocumentFiles, createId, deleteId,
						templateId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending addClmContract method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			log.error("*** Ending addClmContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/list")
	public ResponseEntity<CommonResponse> getListOfClmContract(Authentication authentication,
			HttpServletRequest request,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit, @RequestParam(required = false) String status,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "contractName") String orderBy) {
		try {
			CommonResponse applicationDetailsResponse = clmService.getListOfClmContract(page, limit, request, status,
					searchText, order, orderBy);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending getListOfClmContract method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getListOfClmContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			log.error("*** Ending getListOfClmContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getListOfClmContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/detail")
	public ResponseEntity<CommonResponse> getClmContractDetailsView(Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = clmService.getClmContractDetailsView(envelopeId,authentication);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending getClmContractDetailsView method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getDetailsViewofClmContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			log.error("*** Ending getClmContractDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofClmContract", new ArrayList<>()), e.getMessage()));
		}
	}

	// @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kuala_Lumpur")
	public ResponseEntity<CommonResponse> upCommingContractRenewalReminderEmail() {
		try {
			CommonResponse commonResponse = clmService.upCommingContractRenewalReminderEmail();
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending upCommingContractRenewalReminderEmail method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("contractDetailsOverviewResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			log.error("*** Ending upCommingContractRenewalReminderEmail method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("ContractRenewalResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping(value = "/update/template/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> updateTemplate(@RequestParam(value = "body", required = true) String json,
			@PathVariable(required = true) String templateId,
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			@RequestPart(value = "update-document-file", required = false) MultipartFile[] updateDocumentFiles,
			@RequestParam(value = "update_id", required = false) String[] updateId,
			@RequestParam(value = "delete_id", required = false) String[] deleteId, HttpServletRequest request) {
		try {

			CommonResponse commonResponse = clmService.updateTemplate(json, templateId, createDocumentFiles,
					updateDocumentFiles, updateId, deleteId, request);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending updateTemplate method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("updateTemplateResponce", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending updateTemplate method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("updateTemplateResponce", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping(value = "/create/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> createTemplate(@RequestParam(value = "body", required = true) String json,
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			HttpServletRequest request, @RequestParam String flowType) {
		try {
			CommonResponse commonResponse = clmService.createTemplate(json, createDocumentFiles, request, flowType);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending createTemplate method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("CreateTemplateResponce", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending createTemplate method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("CreateTemplateResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@PostMapping(value = "/createEnvelopeMultiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> createEnvelope(@RequestParam(value = "body", required = true) String json,
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			@RequestParam(value = "create_id", required = false) String[] createId,
			@RequestParam(value = "delete_id", required = false) String[] deleteId,
			@RequestParam(value = "templateId", required = true) String templateId) {
		try {
			CommonResponse commonResponse = clmService.createEnvelope(json, createDocumentFiles, createId, deleteId,
					templateId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending CreateEnvelope method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("CreateEnvelopeResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/dashboard/view")
	public ResponseEntity<CommonResponse> dashboardView(HttpServletRequest request) {
		try {
			CommonResponse commonResponse = clmService.dashboardView(request);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending DashboardView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardViewResponce", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/enevelope/{envelopeid}")
	public ResponseEntity<CommonResponse> envelopeAudit(@PathVariable String envelopeid) {
		try {
			CommonResponse commonResponse = clmService.envelopeAudit(envelopeid);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending envelopeAudit method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/getEnvelopeDocuments/{envelopeId}/{documentId}")
	public ResponseEntity<CommonResponse> getEnvelopeDocument(@PathVariable String envelopeId,
			@PathVariable String documentId) {
		try {
			CommonResponse commonResponse = clmService.getEnvelopeDocument(envelopeId, documentId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending getEnvelopeDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeDocumentResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/listEnvelopeRecipients/{envelopeId}")
	public ResponseEntity<CommonResponse> getlistEnvelopeRecipients(@PathVariable String envelopeId,HttpServletRequest request) {
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
			CommonResponse commonResponse = clmService.getlistEnvelopeRecipients(envelopeId,email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending getlistEnvelopeRecipients method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeRecipientResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/getEnvelopeDocumentDetails/{envelopeId}")
	public ResponseEntity<CommonResponse> getEnvelopeDocumentDetails(@PathVariable String envelopeId,HttpServletRequest request) {
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
			CommonResponse commonResponse = clmService.getEnvelopeDocumentDetails(envelopeId,email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending getEnvelopeDocumentDetails method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeDocumentDetailResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/template/{templateId}")
	public ResponseEntity<CommonResponse> listTemplateById(@PathVariable String templateId,HttpServletRequest request) {
		try {
			CommonResponse commonResponse = clmService.listTemplateById(templateId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending listTemplateById method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Template List Responce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/templates")
	public ResponseEntity<CommonResponse> listTemplates(@RequestParam(defaultValue = "10") String count,
			@RequestParam(defaultValue = "0") String start, @RequestParam(defaultValue = "asc") String order,
			@RequestParam(defaultValue = "name") String orderBy,
			@RequestParam(name = "searchText", required = false) String searchText, @RequestParam String flowType) {
		try {
			CommonResponse commonResponse = clmService.listTemplates(count, start, order, orderBy, searchText,
					flowType);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending listTemplates method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/get/templates")
	public ResponseEntity<CommonResponse> getAllTemplates(@RequestParam String flowType) {
		try {
			CommonResponse commonResponse = clmService.getAllTemplates(flowType);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending getAllTemplates method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping(value = "/getTemplateDocuments/{templateId}/{documentId}")
	public ResponseEntity<CommonResponse> getTemplateDocument(@PathVariable String templateId,
			@PathVariable String documentId) {
		try {
			CommonResponse commonResponse = clmService.getTemplateDocument(templateId, documentId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending TemplateDocumentResponce method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateDocumentResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping(value = "/getEnvelopeComments/{envelopeId}")
	public ResponseEntity<CommonResponse> getEnvelopeComments(@PathVariable String envelopeId,
			HttpServletRequest request) {
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
		try {
			CommonResponse commonResponse = clmService.getEnvelopeComments(envelopeId, email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending getEnvelopeComments method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeCommentResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@PutMapping(value = "/addcontract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//	@PreAuthorize("hasAuthority('ADD_CONTRACT')")
	public ResponseEntity<CommonResponse> clmUpdateDocument(
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			@RequestPart(value = "update-document-file", required = false) MultipartFile[] updateDocumentFiles,
			@RequestParam(value = "update_id", required = false) String[] updateId,
			@RequestParam(value = "delete_id", required = false) String[] deleteId,
			@RequestParam(value = "envelopeId", required = true) String envelopeId, Authentication authentication,
			HttpServletRequest request) {
		try {
			String email = null;
			String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
			String token = request.getHeader(Constant.HEADER_STRING);
			CommonResponse applicationDetailsResponse = null;
			if (provider != null && provider.equalsIgnoreCase(Constant.AZURE)) {
				DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
				if (jwt.getClaim(Constant.EMAIL).asString() != null) {
					email = jwt.getClaim(Constant.EMAIL).asString();
				} else if (jwt.getClaim("upn").asString() != null) {
					email = jwt.getClaim("upn").asString();
				}
				applicationDetailsResponse = clmService.updateContractDocument(createDocumentFiles, updateDocumentFiles,
						updateId, deleteId, envelopeId, email);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			log.error("*** Ending clmUpdateDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("clmUpdateDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			log.error("*** Ending clmUpdateDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("clmUpdateDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping(value = "/get/document/versions")
	public ResponseEntity<CommonResponse> getDocumentVersions(
			@RequestParam(value = "envelopeId", required = true) String envelopeId,HttpServletRequest request) {
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
			CommonResponse commonResponse = clmService.getDocumentVersions(envelopeId,email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getDocumentVersions method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("GetDocumentVersionsResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getDocumentVersions method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("GetDocumentVersionResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@PostMapping("/getEnvelopeDocumentAsPdf")
	public ResponseEntity<CommonResponse> getEnvelopeDocumentAsPdf(@RequestBody SendDocumentPdfMail mailRequest) {
		try {
			CommonResponse commonResponse = clmService.getEnvelopeDocumentAsPdf(mailRequest);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			log.error("*** Ending getEnvelopeDocumentAsPdf method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeDocumentAsPdf", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/update/envelope-status")
	public ResponseEntity<CommonResponse> updateEnvelopeStatus(HttpServletRequest request,
			@RequestParam(value = "envelopeId", required = true) String envelopeId,
			@RequestParam(value = "status", required = true) String status) {
		try {
			CommonResponse commonResponse = clmService.updateEnvelopeStatus(request, envelopeId, status);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		}
		catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending updateEnvelopeStatus method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(e.getStatusCode(),
					new Response("EnvelopeStatusUpdate Response", new ArrayList<>()), e.getMessage()));
		} 
		catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending updateEnvelopeStatus method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeStatusUpdate Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping(value = "/createConsoleView", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> createEnvelope(
			@RequestParam(value = "envelopeId", required = true) String envelopeId,
			@RequestParam(value = "returnUrl", required = true) String returnUrl,
			@RequestParam(value = "action", required = false) String action, HttpServletRequest request) {

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
		try {
			CommonResponse commonResponse = clmService.createConsoleView(email, envelopeId, returnUrl, action);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending CreateEnvelope method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(e.getStatusCode(),
					new Response("CreateEnvelopeResponce", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending CreateEnvelope method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("CreateEnvelopeResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping(value = "/envelope/{envelopeId}")
	public ResponseEntity<CommonResponse> getEnvelopeByEnvelopeId(@PathVariable String envelopeId,
			 org.springframework.security.core.Authentication authentication)
			throws DataValidationException {
		CustomUserDetails profile = (CustomUserDetails) authentication.getPrincipal();
		String email = profile.getEmail();
		try {
			CommonResponse commonResponse = clmService.getEnvelopeByEnvelopeId(envelopeId, email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getEnvelopeDetailResponse method with an error ***", e);
			return ResponseEntity.status(e.getStatusCode()).body(new CommonResponse(e.getStatusCode(),
					new Response("EnvelopeDetailResponse", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getEnvelopeDetailResponse method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeDetailResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping(value = "/envelope/notification/{envelopeId}")
	public ResponseEntity<CommonResponse> getEnvelopeNotificationByEnvelopeId(@PathVariable String envelopeId)
			throws DataValidationException {
		try {
			CommonResponse commonResponse = clmService.getEnvelopeNotificationByEnvelopeId(envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getEnvelopeNotificationDetailResponse method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeNotificationDetailResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping(value = "/usage")
	public ResponseEntity<CommonResponse> getUsage() throws DataValidationException {
		try {
			CommonResponse commonResponse = clmService.getUsage();
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getEnvelopeNotificationDetailResponse method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EnvelopeNotificationDetailResponse", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/template/list")
	public ResponseEntity<CommonResponse> getTemplates(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit, @RequestParam(required = false) String order,
			@RequestParam(defaultValue = "templateName") String orderBy,
			@RequestParam(name = "searchText", required = false) String searchText,
			@RequestParam(value = "flowType") String flowType) {
		try {
			CommonResponse commonResponse = clmService.getListOfTemplate(page, limit, searchText, order, orderBy,
					flowType);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getAllTemplates method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	// One Document Upload
	@PostMapping(value = "/addLoacontract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//	@PreAuthorize("hasAuthority('ADD_CONTRACT')")
	public ResponseEntity<CommonResponse> addClmContractDocument(
			@RequestParam(value = "body", required = true) String json,
			@RequestPart(value = "create-document-file", required = false) MultipartFile[] createDocumentFiles,
			Authentication authentication,
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
				applicationDetailsResponse = clmService.addClmContract(json, createDocumentFiles, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addClmContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping("/upload")
	public ResponseEntity<Object> uploadDocument(@RequestParam(value = "envelopeId", required = true) String envelopeId,
			@RequestParam(value = "documentId", required = true) String documentId,
			@RequestPart(value = "create-document-file", required = true) MultipartFile createDocumentFiles,
			@RequestParam(value = "flowType", required = true, defaultValue = "external") String flowType,HttpServletRequest request) {
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
			Object commonResponse = clmService.uploadDocuments(envelopeId, documentId, createDocumentFiles, flowType,email);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending uploadDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@PostMapping("templatedoc/toenvelope/upload")
	public ResponseEntity<Object> templateDocumentUploadToEnvelope(@RequestParam(required = true) String envelopeId,
			@RequestParam(required = true) String templateId,
			@RequestParam(value = "templateDocument", required = true) String docRequest,
			@RequestParam(required = true) String envelopeDocumentId,
			@RequestParam(value = "documentType", required = true) String documentType,
			@RequestParam(value = "flowType", required = true) String flowType) {
		try {
			Object commonResponse = clmService.templateDocumentUploadToEnvelope(envelopeId, templateId, docRequest,
					envelopeDocumentId, documentType, flowType);
			return ResponseEntity.status(HttpStatus.OK).body(commonResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending templateDocumentUploadToEnvelope method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("TemplateListResponce", new ArrayList<>()), e.getMessage()));
		}

	}

	@GetMapping("/download-document/{envelopeId}")
	public ResponseEntity<byte[]> downloadDocument(@PathVariable(value = "envelopeId") String envelopeId) {
		try {
			return clmService.downloadDocuments(envelopeId);
		} catch (Exception e) {
			log.error("*** Ending downloadDocument method with an error ***", e);
			return null;
		}
	}
	
	
	@PutMapping("/document-version-mail")
	public ResponseEntity<CommonResponse> emailTrigger(@RequestParam(value = "envelopeId",required = true) @Valid String envelopeId
			,@RequestParam(value = "versionOrder", required = true) @Valid String versionOrder) {
		try {
		      clmService.documentVersionEmail(envelopeId,versionOrder);
		      return ResponseEntity.ok().body(new CommonResponse(HttpStatus.OK,
						new Response("EmailTriggerResponse", new ArrayList<>()), "Email sent successfully"));
		} catch (Exception e) {
			log.error("*** Ending emailTriggerResponse method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("EmailTriggerResponse", new ArrayList<>()), e.getMessage()));
		}
	}
	
}
