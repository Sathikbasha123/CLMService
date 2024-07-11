package saaspe.clm.model;

import lombok.Data;

@Data
public class EventField {
	private Object errorDetails;
	private String name;
	private Object originalValue;
	private String value;
}
