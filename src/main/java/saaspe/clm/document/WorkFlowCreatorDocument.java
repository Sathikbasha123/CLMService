package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@Document(collection = "WorkFlowCreatorDocument")
public class WorkFlowCreatorDocument {

	@Transient
	public static final String SEQUENCE_NAME = "WorkFlowCreatorDocumentsequence";
	private long id;
	private String email;
	private List<String> pendingWith;
	private String envelopeId;
	private String contractName;
	private String flowType;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;
	private String projectId;
	private String projectName;
	private String tenantName;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date startDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date expiryDate;
	private String status;
	private String subsidiary;
}
