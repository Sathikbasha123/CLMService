package saaspe.clm.model;


import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import saaspe.clm.docusign.model.Recipients;

@Data
public class LoaDocumentRequest {

	private String projectName;

	private String vendorName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;

	private Currency currency;

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

	private boolean vendorSigningOrder;

	private List<Reviewers> reviewers;

	private List<Vendors> vendors;
	
	private String subsidiary;
	
	private String moduleName;
	

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

	private int contractTenure;

	private String userEmail;

	private List<AllSigners> allSigners;

	private List<Stampers> lhdnStampers;

	private Boolean lhdnSigningOrder;

	private String contractNumber;
	
	private String remark;
	
	private String category;
	
	private String contractName;
}
