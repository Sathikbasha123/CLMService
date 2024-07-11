package saaspe.clm.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoaContractDetailsViewResponse {

	private String projectName;
	private String contractName;
	private String envelopeId;
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
	private List<CvefSigner> cvefSigners;
	private String subsidiary;
	private List<Stampers> lhdnStampers;
	private Boolean lhdnSigningOrder;
	private List<AllSigners> allSigners;
	private String contractNumber;
	private String projectId;
	private String remark;
	private String category;
}
