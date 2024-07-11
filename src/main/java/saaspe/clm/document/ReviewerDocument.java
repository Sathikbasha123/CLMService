package saaspe.clm.document;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@Document(collection = "ReviewerDocument")
public class ReviewerDocument {

	@Transient
	public static final String SEQUENCE_NAME = "ReviewerVersionDocumentsequence";
	@Id
	private long id;
	private String email;
	private boolean isCompleted;
	private String orderOfReviewer;
	private String envelopeId;
	private String reviewerName;
	private String senderName;
	private String flowType;
	private int docVersion;
	private boolean orderFlag;
	private int routingOrder;
	private String contractName;
	private String createdBy;
	private String creatorName;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date endDate;
	private String status;
	private String subsidiary;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date expiryDate;

	private String projectId;

	private String vendorName;

	private String contractNumber;

	private String tenant;
}
