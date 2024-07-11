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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.LoaContractService;

@RestController
@ControllerLogging
@RequestMapping("/api/v1/loaContract")
public class LoaContractController {
	
	@Autowired LoaContractService loaContractService;
	
	private static final Logger log = LoggerFactory.getLogger(LoaContractController.class);
	
	@PostMapping(value = "/addLoaContractDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> addLoaContractDocument(@RequestParam(value = "body", required = true) String json,
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
				applicationDetailsResponse = loaContractService.addLoaContractDocument(json, createDocumentFiles, createId, deleteId,
						envelopeId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending addClmContract method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending addClmContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("addClmContract", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@PostMapping(value = "/newAddLoaContractDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse> newAddLoaContractDocument(@RequestParam(value = "body", required = true) String json,
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
				applicationDetailsResponse = loaContractService.newAddLoaContractDocument(json,
						envelopeId, email, name);
			}
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending newAddLoaContractDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("New AddLoaContractDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending newAddLoaContractDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("New AddLoaContractDocument", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/detail")
	public ResponseEntity<CommonResponse> getLoaContractDocumenttDetailsView(Authentication authentication,
			@RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID,
			@RequestHeader(name = "buID", required = false, defaultValue = "") String buID,
			@RequestParam(name = "envelopeId") String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = loaContractService.getLoaContractDocumentDetailsView(envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getLoaContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getDetailsViewofLoaContractDocument", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLoaContractDocumentDetailsView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getDetailsViewofLoaContractDocument", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@GetMapping("/getCreatedContractList")
	public ResponseEntity<CommonResponse> getCreatedContract(Authentication authentication, @RequestHeader(name = "opID", required = false, defaultValue = "SAASPE") String opID, @RequestHeader(name = "buID", required = false, defaultValue = "") String buID, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "20") int limit) {
		try {
			CommonResponse applicationDetailsResponse = loaContractService.getListOfLoaContractCreated(page, limit);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getListOfLoaContractCreated method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("getListOfLoaContractCreated", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/create/list")
	public ResponseEntity<CommonResponse> getCreateListLoaContract(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, 
			@RequestParam(required = false) String order, 
			@RequestParam(required = false, defaultValue = "projectName") String orderBy, 
			@RequestParam(required = false) String subsidiary, 
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String signingStatus,
			@RequestParam(required = false) String category) {
		try {
			CommonResponse applicationDetailsResponse = loaContractService.getCreateListForContract(request, page, limit, searchText, order, orderBy, subsidiary, status,signingStatus,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListLoaContract method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND, new Response("getCreateListLoaContract", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCreateListLoaContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("getCreateListLoaContract", new ArrayList<>()), e.getMessage()));
		}
	}

	@Scheduled(cron = "0 0 0 */1 * *")
	@GetMapping("/get-expiring-documents")
	public CommonResponse updateStatus()
	{
		log.info("Updating expiring Contract Documents");
		try {
			return loaContractService.setExpiringEnvelope();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending expiringDocument with an error ***", e);
			e.printStackTrace();
			return null;
		}
	}

}
