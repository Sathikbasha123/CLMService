package saaspe.clm.model;

import lombok.Data;

@Data
public class EventLogs {
	
	private String event;
	private String apiVersion;
	private String uri;
	private String retryCount;
	private String configurationId;
	private String generatedDateTime;
	private EventData data;
	
	
	

}
