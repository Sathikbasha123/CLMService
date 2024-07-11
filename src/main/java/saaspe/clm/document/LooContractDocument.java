package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import saaspe.clm.model.AllSigners;
import saaspe.clm.model.Tenants;
import saaspe.clm.model.Vendors;

@Data
@Document(collection = "LooContractDocument")
public class LooContractDocument {

	@Transient
	public static final String SEQUENCE_NAME = "looContractDocumentsequence";

	private long id;

	private String opID;

	private String buID;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;

	private String createdBy;

	private String updatedBy;

	private String senderEmail;

	private String senderName;

	private String status;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private String projectId;

	private String envelopeId;

	private String templateId;

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
	private Date completionDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementBusinessDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date expiryDate;

	private String lotNumber;

	private String location;

	private String area;

	private String airport;

	private Boolean isFlowCompleted;

	private boolean isCompleted;

	private Boolean tenantSigningOrder;

	private List<Vendors> vendors;

	private List<Tenants> tenants;

	private String subsidiary;

	private List<AllSigners> allSigners;

	private String isSignersCompleted;
	
	private String isTenantsCompleted;

	private String category;
	
	private boolean watcherEmailStatus;
}
