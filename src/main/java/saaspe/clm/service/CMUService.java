package saaspe.clm.service;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import com.microsoft.azure.storage.StorageException;

import freemarker.template.TemplateException;

import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

public interface CMUService {

	CommonResponse updateCmuDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles,
			String[] update_id, String[] delete_id, String envelopeId, String email)
            throws Exception;

	CommonResponse getApprovedCMUContractList(HttpServletRequest request);

	CommonResponse getCMUContractDocumentDetailsView(String envelopeId,String email) throws DataValidationException, JsonProcessingException, URISyntaxException, StorageException;

	CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId)throws DataValidationException ;

	CommonResponse getApprovedCmuAllList(HttpServletRequest request, int page, int size,String searchText, String order, String orderBy,String subsidiary, String status);

	CommonResponse getCreateListForCmu(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy, String subsidiary, String status,String category) throws DataValidationException, UnsupportedEncodingException;

	CommonResponse newAddCmuDocument(String json, String templateId, String email, String name) throws JsonMappingException, JsonProcessingException, IOException, TemplateException, MessagingException, DataValidationException;

}