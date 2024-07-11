package saaspe.clm.model;

import lombok.Data;

@Data
public class DocumentVersionDocumentResponse {

	private String documentName;
	private String blobUrl;
	private String documentId;
}
