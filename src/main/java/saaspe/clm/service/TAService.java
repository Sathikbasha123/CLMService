package saaspe.clm.service;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import freemarker.template.TemplateException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface TAService {

	CommonResponse addTaDocument(String json, MultipartFile[] createDocumentFiles, String[] createId, String[] deleteId,
			String envelopeId, String email, String name)
			throws DataValidationException, IOException;

	CommonResponse getTaDocumentDetailsView(String envelopeId,HttpServletRequest request) throws DataValidationException;

	CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId) throws DataValidationException;

	CommonResponse getCreateListForTa(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy, String subsidiary, String status, String signingStatus,String category) throws DataValidationException, UnsupportedEncodingException, JsonMappingException, JsonProcessingException;

	CommonResponse newAddTaDocument(String json, String envelopeId, String email, String name) throws JsonMappingException, JsonProcessingException, DataValidationException, IOException, TemplateException, MessagingException;

	CommonResponse setExpiringEnvelope();
}
