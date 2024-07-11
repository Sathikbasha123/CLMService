package saaspe.clm.document;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
@Document(collection = "UserInfoDocument")
public class UsersInfoDocument {
	@Transient
	public static final String SEQUENCE_NAME = "UsersInfoDocumentsequence";
	private long id;
	private String uniqueId;
	private String name;
	private String email;
	private boolean isActive;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date deactivatedDate;
	private String deactivatedBy;
	private String division;
	private String currentRole;
	private List<String> roles;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;
	private String createdBy;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;
	private String updatedBy;
	private String approvedBy;
	private String createdThrough;
}
