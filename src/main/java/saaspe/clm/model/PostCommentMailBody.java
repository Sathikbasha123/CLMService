package saaspe.clm.model;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostCommentMailBody {

	private String creatorMail;
	private String creatorName;
	private String documentName;
	private String contractName;
	private String comment;
	private String flowType;
	private String envelopeId;
	private String moduleName;
	private String version;
	
}
