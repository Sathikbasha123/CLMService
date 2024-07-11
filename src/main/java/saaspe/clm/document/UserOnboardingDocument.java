package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@Document(collection = "UserOnboardingDocument")
public class UserOnboardingDocument {

	@Transient
	public static final String SEQUENCE_NAME = "userOnboardingDocumentsequence";
	private long id;
	private String uniqueId;
	private String name;
	private String email;
	private String division;
	private List<String> requestRoles;
	private List<String> roles;
	private String status;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date createdOn;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date updatedOn;
	private String updatedBy;
	private String createdBy;
	private String createdThrough;
	private String rejectionReason;
	private String rejectedBy;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date rejectedAt;
	private String approvedBy;

}
