package saaspe.clm.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class LoaContractCreatedResponse {
	private String envelopeId;
	private String projectName;
	private String vendorName;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;
	private Currency contractValue;
	private int contractTenure;
	private String status;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;
	private String senderEmail;
	private List<Vendors> vendors;
	private List<Stampers> stampers;
	private List<Stampers> lhdnStampers;
	private Boolean lhdnSigningOrder;
	private String contractNumber;
}
