package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import saaspe.clm.model.*;

@Data
@Document(collection = "LoaContractDocument")
public class LoaContractDocument {

	@Transient
	public static final String SEQUENCE_NAME = "loaContractDocumentsequence";

	private long id;

	private String projectId;

	private String projectName;

	private String envelopeId;

	private String templateId;

	private String vendorName;

	private String contractName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;

	private Currency currency;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private int contractTenure;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;

	private String createdBy;

	private String updatedBy;

	private String senderEmail;
	
	private String senderName;

	private String version;

	private String opID;

	private String buID;

	private String status;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private Boolean isFlowCompleted;

	private Boolean cvefSigningOrder;

	private Boolean vendorSigningOrder;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date lastModifiedDateTime;

	private List<Vendors> vendors;

	private List<Stampers> stampers;

	private List<CvefSigner> cvefSigners;

	private String subsidiary;

	private List<Stampers> lhdnStampers;

	private Boolean lhdnSigningOrder;

	private List<AllSigners> allSigners;

	private String contractNumber;

	private String isSignersCompleted;

	private String isCvefSignersCompleted;

	private String isStampersCompleted;

	private String isVendorsCompleted;

	private String isLhdnSignersCompleted;

	private String remark;
	
	private String category;
	
	private boolean watcherEmailStatus;
}
