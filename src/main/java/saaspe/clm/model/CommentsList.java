package saaspe.clm.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saaspe.clm.docusign.model.Comments;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(content = Include.NON_NULL)
public class CommentsList {

	private String versionOrder;
	private int order;
	private List<Comments> comments;
	private String documentName;

}
