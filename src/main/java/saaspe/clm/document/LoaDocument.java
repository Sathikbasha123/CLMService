package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NoArgsConstructor;
import saaspe.clm.model.AllSigners;
import saaspe.clm.model.Currency;
import saaspe.clm.model.Stampers;
import saaspe.clm.model.Vendors;

@Data
@NoArgsConstructor
public class LoaDocument {

	@Transient
	public static final String SEQUENCE_NAME = "LoaDocumentsequence";

	private long id;

	private String opID;

	private String buID;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date createdOn;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date updatedOn;

	private String createdBy;

	private String updatedBy;

	private String templateId;

	private String envelopeId;

	private String status;

	private Currency currency;

	private String senderEmail;

	private String senderName;

	private String referenceId;

	private String referenceType;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private String signers;

	private String reviewers;

	private String newEnvelopeId;

	private int order;

	private String version;

	private String uniqueString;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date lastModifiedDateTime;

	private String projectName;

	private String vendorName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date completionDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private int contractTenure;

	private String projectId = "";

	private boolean isFlowCompleted;
	
	private boolean isCompleted;

	private boolean vendorSigningOrder;
	
	private List<Vendors> vendors;
	
	private boolean isFilesUploaded;
	
	private String subsidiary;
	
	
	private int totalFiles;

	private List<AllSigners> allSigners;

	private List<Stampers> lhdnStampers;

	private Boolean lhdnSigningOrder;

	private String contractNumber;

	private String isSignersCompleted;
	
	private String isVendorsCompleted;
	
	private String isLhdnSignersCompleted;

	private String remark;
	
	private String category;
	
	private boolean watcherEmailStatus;
}
