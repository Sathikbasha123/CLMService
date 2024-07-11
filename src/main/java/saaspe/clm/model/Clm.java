package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Clm {
	private boolean enabled;
	private boolean consentGiven;
	private String consentUrl;
	private String activated;
	// @JsonIgnore
	private String error;
	private boolean isActive;
}
