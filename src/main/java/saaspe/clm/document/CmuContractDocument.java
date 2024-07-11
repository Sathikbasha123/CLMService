package saaspe.clm.document;

import java.util.Date;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import saaspe.clm.model.Currency;

@Data
@Document(collection = "CmuContractDocument")
public class CmuContractDocument {
	@Transient
	public static final String SEQUENCE_NAME = "cmuContractDocumentsequence";

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

	private String referenceId;

	private String referenceType;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private String signers;

	private String reviewers;

	private String templateId;

	private String envelopeId;

	private int order;

	private String version;

	private String projectId;

	private Boolean isFlowCompleted;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date lastModifiedDateTime;

	private String contractTitle;

	private String tenant;

	private String tenderNo;

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

	private Currency rent;

	private String lotNumber;

	private String location;

	private String area;

	private String airport;

	private String emailMessage;

	private String emailSubject;

	private String subsidiary;

	private String category;
}
