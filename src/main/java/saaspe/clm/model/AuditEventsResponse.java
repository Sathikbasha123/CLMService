package saaspe.clm.model;

import lombok.Data;

@Data
public class AuditEventsResponse {
	
	private String logTime;
	
	private String source;
	
	private String userName;
	
	private String userId;
	
	private String action;
	
	private String message;
	
	private String envelopeStatus;
	
	private String clientIPAddress;
	
	private String information;
	
	private String informationLocalized;
	
	private String geoLocation;
	
	private String language;

}
