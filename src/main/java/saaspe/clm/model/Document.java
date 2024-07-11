package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(content = Include.NON_NULL)
public class Document {

	private String documentBase64;
	private String documentId;
	private String fileExtension;
	private String name;
	private String category;
	private boolean isUploaded;
}
