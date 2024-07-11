package saaspe.clm.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import com.microsoft.azure.storage.StorageException;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommentRequest;
import saaspe.clm.model.CommonResponse;

public interface WorkflowService {

	CommonResponse getListOfClmContract(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy,String status,String subsidiary, String customFlowType,String category) throws DataValidationException;

	CommonResponse getListOfClmContractForCreate(HttpServletRequest request, int page, int limit,  String searchText, String order, String orderBy)
			throws DataValidationException;

	CommonResponse getListOfReviewedClmContract(HttpServletRequest request, int page, int limit, String searchText, String order, String orderBy,String status,String subsidiary, String customFlowType,String category)
			throws DataValidationException;

	CommonResponse postCommentsOnEnvelope(HttpServletRequest request, CommentRequest body)
			throws DataValidationException;

	CommonResponse approveDocument(HttpServletRequest request, String envelopeId)
			throws DataValidationException, UnsupportedEncodingException, MessagingException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;

	CommonResponse getCommentsList(String envelopeId,HttpServletRequest request) throws DataValidationException;

	CommonResponse getApprovedLoaList(HttpServletRequest request, String subsidiary);

	CommonResponse getReviewersList(HttpServletRequest request, String envelopeId) throws DataValidationException;

	CommonResponse getApprovedLoaAllList(HttpServletRequest request, int page, int size,String searchText, String order, String orderBy,String status, String subsidiary);

	CommonResponse deleteLock(String envelopeId,HttpServletRequest request) throws DataValidationException;

	CommonResponse getDocumentVersionDocuments(String envelopeId, int docVersion) throws URISyntaxException, StorageException,DataValidationException;

}
