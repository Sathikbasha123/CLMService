package saaspe.clm.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import saaspe.clm.docusign.model.Recipients;

@Data
public class LoaContractDocumentRequest {

	
	private String projectName;

	private String contractName;

	private String vendorName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;

	private Currency currency;

	private int contractTenure;

	private List<Document> documents;

	private String emailSubject;

	private String emailMessage;

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

	private List<Reviewers> reviewers;

	private List<Vendors> vendors;

	private List<Stampers> stampers;

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

	private Boolean cvefSigningOrder;

	private Boolean vendorSigningOrder;

	private List<CvefSigner> cvefSigners;
	
	private String subsidiary;

	private List<Stampers> lhdnStampers;

	private Boolean lhdnSigningOrder;

	private List<AllSigners> allSigners;

	private String contractNumber;
	
	private String moduleName;

	private String remark;
	private String category;
}
