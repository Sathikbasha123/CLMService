package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import saaspe.clm.model.*;

@Data
@Document(collection = "TADocument")
public class TADocument {
	
	@Transient
	public static final String SEQUENCE_NAME = "TaDocumentsequence";

	private long id;

	private String projectId;

	private String opID;

	private String buID;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;

	private String createdBy;

	private String updatedBy;

	private String templateId;

	private String envelopeId;

	private String status;

	private String senderEmail;

	private String referenceId;

	private String referenceType;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private String signers;

	private String reviewers;

	private String newEnvelopeId;

	private int order;

	private String version;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date lastModifiedDateTime;
	
	private String contractTitle;

	private String tenant;

	private String tenderNo;

	private String referenceNo;

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

	private boolean isFlowCompleted;

	private Boolean tenantSigningOrder;

	private Boolean cvefSigningOrder;

	private List<Tenants> tenants;

	private List<Stampers> stampers;

	private List<CvefSigner> cvefSigners;

	private String subsidiary;

	private List<Stampers> lhdnStampers;

	private boolean lhdnSigningOrder;

	private List<AllSigners> allSigners;
	
	private String isSignersCompleted;
	
	private String isCvefSignersCompleted;

	private String isStampersCompleted;
	
	private String isTenantsCompleted;

	private String isLhdnSignersCompleted;

	private String category;
	
	private boolean watcherEmailStatus;
}
