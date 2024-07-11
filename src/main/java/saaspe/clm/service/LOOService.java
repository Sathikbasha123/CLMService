package saaspe.clm.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import freemarker.template.TemplateException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;

public interface LOOService {

	CommonResponse addLooContractDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String envelopeId, String email, String name) throws Exception;

	CommonResponse updateLooDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles, String[] update_id, String[] delete_id, String envId, String email)
			throws Exception;

	CommonResponse getApprovedLOOContractList(HttpServletRequest request);

	CommonResponse getLooDocumentDetailsView(String envelopeId)throws DataValidationException, JsonMappingException, JsonProcessingException;

	CommonResponse getDocumentVersions(HttpServletRequest request,String envelopeId) throws DataValidationException;

	CommonResponse getCreateListForLoo(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy, String subsidiary, String status,String category) throws DataValidationException, UnsupportedEncodingException;

	CommonResponse newAddLooContractDocument(String json, String templateId, String email, String name) throws JsonProcessingException, DataValidationException, IOException, TemplateException, MessagingException;
	
	CommonResponse cmutemplateDocumentUploadToEnvelope(String envelopeId, String templateId, String docRequest,
			String envelopeDocumentId,String flowType,String email) throws DataValidationException, JsonMappingException, JsonProcessingException, Exception;

	CommonResponse setExpiringEnvelope();

}