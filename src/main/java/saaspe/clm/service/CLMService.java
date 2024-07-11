package saaspe.clm.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.microsoft.azure.storage.StorageException;
import freemarker.template.TemplateException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.SendDocumentPdfMail;

public interface CLMService {

	CommonResponse addClmContract(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId, String profile, String name)
			throws DataValidationException, JsonProcessingException, IOException;

	CommonResponse getListOfClmContract(int page, int limit, HttpServletRequest request, String status,
			String searchText, String order, String orderBy) throws DataValidationException, JsonProcessingException;

	CommonResponse getClmContractDetailsView(String envelopeId,Authentication authentication)
			throws DataValidationException, JsonProcessingException, IllegalArgumentException, IllegalAccessException;

	CommonResponse upCommingContractRenewalReminderEmail()
			throws IOException, TemplateException, DataValidationException, MessagingException;

	CommonResponse updateTemplate(String json, String templateId, MultipartFile[] createDocumentFiles,
			MultipartFile[] updateDocumentFiles, String[] createId, String[] updateId, HttpServletRequest request)
			throws JsonProcessingException, DataValidationException;

	CommonResponse createTemplate(String json, MultipartFile[] createDocumentFiles, HttpServletRequest request,
			String flowType) throws JsonProcessingException, DataValidationException;

	CommonResponse createEnvelope(String json, MultipartFile[] createDocumentFiles, String[] createId,
			String[] deleteId, String templateId) throws JsonProcessingException;

	CommonResponse dashboardView(HttpServletRequest request) throws JsonProcessingException;

	CommonResponse envelopeAudit(String envelopeid);

	CommonResponse getEnvelopeDocument(String envelopeId, String documentId) throws DataValidationException;

	CommonResponse getlistEnvelopeRecipients(String envelopeId,String email) throws DataValidationException;

	CommonResponse getEnvelopeDocumentDetails(String envelopeId,String email) throws DataValidationException;

	CommonResponse listTemplateById(String templateId);

	CommonResponse listTemplates(String count, String start, String order, String orderBy, String searchText,
			String flowType) throws IOException;

	CommonResponse getAllTemplates(String flowType);

	CommonResponse getTemplateDocument(String templateId, String documentId);

	CommonResponse updateContractDocument(MultipartFile[] createDocumentFiles, MultipartFile[] updateDocumentFiles,
			String[] updateId, String[] deleteId, String envelopeId, String email)
			throws DataValidationException, JsonProcessingException;

	CommonResponse getEnvelopeComments(String envelopeId, String email) throws DataValidationException;

	CommonResponse getDocumentVersions(String envelopeId,String email) throws DataValidationException;

	CommonResponse getEnvelopeDocumentAsPdf(SendDocumentPdfMail mailRequest)
			throws DataValidationException, MessagingException, UnsupportedEncodingException;

	CommonResponse updateEnvelopeStatus(HttpServletRequest request, String envelopeId, String status)
			throws IllegalStateException, JsonMappingException, JsonProcessingException,DataValidationException;

	CommonResponse createConsoleView(String email, String envelopeId, String returnUrl, String action)
			throws DataValidationException;

	CommonResponse getEnvelopeByEnvelopeId(String envelopeId, String email) throws DataValidationException;

	CommonResponse getEnvelopeNotificationByEnvelopeId(String envelopeId);

	CommonResponse getUsage();

	CommonResponse getListOfTemplate(int page, int limit, String searchText, String order, String orderBy,
			String flowType) throws UnsupportedEncodingException;

	CommonResponse addClmContract(String json, MultipartFile[] createDocumentFiles, String email, String name)
			throws JsonMappingException, JsonProcessingException;

	Object uploadDocuments(String envelopeId, String documentId, MultipartFile createDocumentFiles, String flowType,String email)
			throws IOException, URISyntaxException, StorageException, DataValidationException;

	CommonResponse templateDocumentUploadToEnvelope(String envelopeId, String templateId, String docRequest,
			String envelopeDocumentId, String documentType, String flowType)
			throws DataValidationException, JsonMappingException, JsonProcessingException;

	ResponseEntity<byte[]> downloadDocuments(String envelopeId);

	void documentVersionEmail(String envelopeId, String versionOrder) throws UnsupportedEncodingException, MessagingException, Exception;

}
