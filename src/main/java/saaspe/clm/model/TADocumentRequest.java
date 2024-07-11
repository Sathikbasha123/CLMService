package saaspe.clm.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import saaspe.clm.docusign.model.Recipients;

@Data
@JsonInclude(value = Include.NON_NULL)
public class TADocumentRequest {

	private String emailSubject;

	private String emailMessage;

	private String contractTitle;

	private String referenceNo;
	
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

	private String lotNumber;

	private String location;

	private String area;

	private String airport;

	private List<Document> documents;

	private Boolean signingOrder;

	private Recipients recipients;

	private String status;

	private String allowComments;

	private Boolean enforceSignerVisibility;

	private String recipientLock;

	private String messageLock;

	private Reminders reminders;

	private Expiration expirations;

	private Boolean signerCanSignOnMobile;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private Boolean tenantSigningOrder;

	private Boolean cvefSigningOrder;

	private List<Reviewers> reviewers;

	@Data
	public class Reminders {
		private String reminderDelay;
		private String reminderFrequency;
		private String reminderEnabled;
	}

	@Data
	public class Expiration {
		private String expireAfter;
		private String expireEnabled;
		private String expireWarn;
	}

	private String userEmail;

	private List<Tenants> tenants;

	private List<Stampers> stampers;

	private List<CvefSigner> cvefSigners;

	private String subsidiary;

	private List<Stampers> lhdnStampers;

	private boolean lhdnSigningOrder;

	private String templateName;

	private List<AllSigners> allSigners;

	private String category;
	
	private String moduleName;
	
	private String contractNumber;
	
	private String contractName;
}
