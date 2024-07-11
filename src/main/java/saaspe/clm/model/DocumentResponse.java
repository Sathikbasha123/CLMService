package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(content = Include.NON_NULL)
public class DocumentResponse {

	private String documentId;

	private String documentIdGuid;

	private String name;

	private String documentBase64;
}
