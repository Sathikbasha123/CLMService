package saaspe.clm.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class CmuDocumentDetailResponse {

	private String contractTitle;

	private String tenant;

	private String tenderNo;

	private String tradingName;

	private String tenancyTerm;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementBusinessDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date expiryDate;

	private Currency rent;

	private String lotNumber;

	private String location;

	private String area;

	private String airport;

	private String emailSubject;

	private String emailMessage;

	private String templateName;

	private String currentVersion;

	private String pendingWith;

	private String status;

	private String referenceNo;

	private String envelopeId;
	private String senderName;
	private String senderEmail;
	private String subsidiary;

	private String templateId;

	private Boolean tenantSigningOrder;

	private List<Vendors> vendors;

	List<DocumentVersionDocumentResponse> documentVersionDocumentResponses;

	private List<Tenants> tenants;

	private String projectId;

	private List<AllSigners> allSigners;
	
	private String category;

}
