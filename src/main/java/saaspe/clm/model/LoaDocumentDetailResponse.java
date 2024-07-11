package saaspe.clm.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class LoaDocumentDetailResponse {

	private String templateName;
	private String envelopeId;
	private String emailSubject;
	private String emailMessage;
	private String projectId;
	private String projectName;
	private String vendorName;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date commencementDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date completionDate;
	private Currency contractValue;
	private int contractTenure;
	private String status;
	private String contractNumber;

}
