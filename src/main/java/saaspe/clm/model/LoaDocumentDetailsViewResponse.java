package saaspe.clm.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoaDocumentDetailsViewResponse {

	private String templateName;
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
	private List<Vendors> vendors;
	private String subsidiary;
	
	private boolean isFilesUploaded;
	private int filesUploadCount;
	private int totalFiles;
	private List<AllSigners> allSigners;
	private boolean vendorSigningOrder;
	private Boolean signerSigningOrder;
	private List<Stampers> lhdnStampers;
	private Boolean lhdnSigningOrder;
	private String contractNumber;
	private String remark;
	private String category;
}
