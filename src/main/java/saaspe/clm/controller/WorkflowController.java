package saaspe.clm.controller;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommentRequest;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.WorkflowService;

@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

	private static final Logger log = LoggerFactory.getLogger(CLMController.class);

	@Autowired
	private WorkflowService workflowService;

	@GetMapping("/review/list")
	public ResponseEntity<CommonResponse> getReviewerWorkflow(HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "contractName") String orderBy,
  			@RequestParam(required = false) String subsidiary,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String customFlowType) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getListOfClmContract(request, page, limit,
					searchText, order, orderBy,status,subsidiary,customFlowType,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getReviewerWorkflow method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getReviewerWorkflow", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getListOfClmContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getReviewerWorkflow", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/reviewed/list")
	public ResponseEntity<CommonResponse> getReviewerReviewedWorkflow(HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "contractName") String orderBy,
			@RequestParam(required = false) String subsidiary,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String customFlowType){
		try {
			CommonResponse applicationDetailsResponse = workflowService.getListOfReviewedClmContract(request, page,
					limit,searchText,order,orderBy,status,subsidiary,customFlowType,category);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getReviewerWorkflow method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getReviewerReviewedWorkflow", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getListOfClmContract method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getReviewerReviewedWorkflow", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/create/list")
	public ResponseEntity<CommonResponse> getReviewerCreateWorkflow(HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int limit,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "contractName") String orderBy) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getListOfClmContractForCreate(request, page,
					limit, searchText, order, orderBy);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getReviewerCreateWorkflow method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("getReviewerCreateWorkflow", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getReviewerCreateWorkflow method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("getReviewerCreateWorkflow", new ArrayList<>()), e.getMessage()));
		}
	}

	@PostMapping("/comments")
	public ResponseEntity<CommonResponse> postCommentsOnEnvelope(HttpServletRequest request,
			@RequestBody CommentRequest body) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.postCommentsOnEnvelope(request, body);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending postCommentsOnEnvelope method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("Envelope Comments Response", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending postCommentsOnEnvelope method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Envelope Comments Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/getCommentsList")
	public ResponseEntity<CommonResponse> getCommentsList(HttpServletRequest request, @RequestParam String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getCommentsList(envelopeId,request);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending getCommentsList method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(e.getStatusCode(),
					new Response("Envelope Comments Response", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCommentsList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Envelope Comments Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/approve")
	public ResponseEntity<CommonResponse> approveDocument(HttpServletRequest request, @RequestParam String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.approveDocument(request, envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending approveDocument method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("Document Approve Response", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending approveDocument method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Approve Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@PutMapping("/deleteLock")
	public ResponseEntity<CommonResponse> deleteLock(@RequestParam("envelopeId") String envelopeId,HttpServletRequest request) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.deleteLock(envelopeId,request);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		}catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending deleteLock method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(e.getStatusCode(),
					new Response("Document Approve Response", new ArrayList<>()), e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending deleteLock method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Lock Delete Response", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/get/approved/loa-list")
	public ResponseEntity<CommonResponse> getApprovedLoaList(HttpServletRequest request,@RequestParam(required = false) String subsidiary) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getApprovedLoaList(request,subsidiary);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		}  catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending approvedLoaList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("approvedLoaListResponse", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/get/reviewers/list")
	public ResponseEntity<CommonResponse> getReviewersList(HttpServletRequest request,
			@RequestParam String envelopeId) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getReviewersList(request, envelopeId);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (DataValidationException e) {
			e.printStackTrace();
			log.error("*** Ending ReviewersList method with an error ***", e);
			return ResponseEntity.ok(new CommonResponse(e.getStatusCode(),
					new Response("ReviewersList", new ArrayList<>()), e.getMessage()));
		}catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending ReviewersList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("ReviewersList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("/get/approved/loa-alllist")
	public ResponseEntity<CommonResponse> getApprovedLoaAllList(HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size,
			@RequestParam(required = false) String searchText, @RequestParam(required = false) String order,
			@RequestParam(required = false, defaultValue = "projectId") String orderBy,
			@RequestParam(required = false) String subsidiary,
			@RequestParam(required = false) String status) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getApprovedLoaAllList(request, page, size,searchText,order,orderBy,status,subsidiary);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending approvedLoaAllListResponse method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("approvedLoaAllListResponse", new ArrayList<>()), e.getMessage()));
		}
	}
	
	@GetMapping("/get/document/version")
	public ResponseEntity<CommonResponse> getDocumentVersionDocuments(HttpServletRequest request,@RequestParam String envelopeId,
			@RequestParam int docVersion) {
		try {
			CommonResponse applicationDetailsResponse = workflowService.getDocumentVersionDocuments(envelopeId,docVersion);
			return ResponseEntity.status(HttpStatus.OK).body(applicationDetailsResponse);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getDocumentVersionDocuments method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Document Version Response", new ArrayList<>()), e.getMessage()));
		}
	}

}
