package saaspe.clm.document;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import saaspe.clm.docusign.model.Comments;

@Data
@Document(collection = "DocumentVersionDocument")
public class DocumentVersionDocument {

	@Transient
	public static final String SEQUENCE_NAME = "DocumentVersionDocumentsequence";
	private long id;
	private String versionOrder;
	private int docVersion;
	private String envelopeId;
	private String path;
    private List<Comments> comments;
    private List<saaspe.clm.model.Document> documents;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;

   
}
