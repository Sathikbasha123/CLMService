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

public interface LoaContractService {

	CommonResponse addLoaContractDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId, String profile, String name)
			throws DataValidationException, JsonProcessingException, IOException, TemplateException, MessagingException;

	CommonResponse getLoaContractDocumentDetailsView(String envelopeId)  throws DataValidationException;

	CommonResponse getListOfLoaContractCreated(int page, int limit);

	CommonResponse getCreateListForContract(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy, String subsidiary, String status,String signingStatus,String category) throws DataValidationException, UnsupportedEncodingException, JsonMappingException, JsonProcessingException;

	CommonResponse newAddLoaContractDocument(String json, String envelopeId, String email, String name) throws IOException, TemplateException, MessagingException, DataValidationException;

	CommonResponse setExpiringEnvelope();

}
