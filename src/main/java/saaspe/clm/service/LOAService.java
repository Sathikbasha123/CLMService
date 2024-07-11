package saaspe.clm.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.microsoft.azure.storage.StorageException;

import freemarker.template.TemplateException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;

@Service
public interface LOAService {

	CommonResponse addLoaDocument(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId, String profile, String name)
			throws DataValidationException, JsonProcessingException, IOException, URISyntaxException, StorageException,
			TemplateException, MessagingException;

	CommonResponse getListOfLoaDocument(int page, int limit, HttpServletRequest request, String status,
			String searchText, String order, String orderBy) throws DataValidationException, JsonProcessingException;

	CommonResponse updateLoaDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles,
			String[] update_id, String[] delete_id, String envelopeId, String email) throws Exception;

	CommonResponse getDocumentVersions(HttpServletRequest request, String envelopeId) throws DataValidationException;

	CommonResponse getLoaDocumentDetailsView(String envelopeId) throws DataValidationException, JsonProcessingException;

	CommonResponse getLoaDocumentDetail(String envelopeId) throws DataValidationException, JsonProcessingException;

	CommonResponse getCreateListForLoa(HttpServletRequest request, int page, int limit, String searchText, String order,
			String orderBy, String subsidiary, String status,String category) throws DataValidationException, UnsupportedEncodingException;

	CommonResponse newaddLoaDocument(String json, String templateId, String email, String name, String fileCount)
			throws IOException, JsonMappingException, JsonProcessingException, TemplateException, MessagingException,
			DataValidationException;

	CommonResponse updateLoaDocument(MultipartFile createDocumentFiles, String create_id, String envelopeId,int versionOrder,
			String email) throws DataValidationException;

	CommonResponse deleteandUploadOldDocToBlob(String[] delete_id, String[] existing_id,List<String> existingNames, String envelopeId,int versionOrder,
			String email) throws DataValidationException, JsonProcessingException, URISyntaxException, StorageException;

	CommonResponse setExpiringEnvelope() throws ParseException;;

}
