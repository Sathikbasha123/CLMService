package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BccEmailAddress {

	@JsonProperty("bccEmailAddressId")
	private String bccEmailAddressId = null;

	@JsonProperty("email")
	private String email = null;

}
