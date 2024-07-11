package saaspe.clm.model;

import com.mongodb.lang.NonNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {

	@NonNull
	private String envelopeId;
	@NonNull
	private int documentId;
	@NonNull
	private String comment;
}
