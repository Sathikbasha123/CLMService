package saaspe.clm.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.util.Date;

@Data
@Document(collection = "ClmTemplateDocument")
public class CreateTemplate {

	@Transient
	public static final String SEQUENCE_NAME = "clmTemplateDocumentsequecnce";

	@Id
	@JsonIgnore
	private long id;

	private String templateId;

	private String templateName;

	private String flowType;

	private String ownerName;

	private String ownerEmail;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date createdOn;

	private String updateByName;

	private String updateByEmail;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date updatedOn;
}
