package saaspe.clm.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ClmContractListResponce {

	private Long contractId;

	private String templateId;

	private String contractName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date contractStartDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date contractEndDate;

	private int renewalReminderNotification;

	private String startDate;

	private String compeletedDate;

	private String envelopeId;

	private String senderName;

	private String senderMail;

	private String status;
	
	private int contractPeriod;

}
