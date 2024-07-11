package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EmailSettings {
	
	@JsonProperty("bccEmailAddresses")
	  private java.util.List<BccEmailAddress> bccEmailAddresses = null;

	  @JsonProperty("replyEmailAddressOverride")
	  private String replyEmailAddressOverride = null;

	  @JsonProperty("replyEmailNameOverride")
	  private String replyEmailNameOverride = null;

}
