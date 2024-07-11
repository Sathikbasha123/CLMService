package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ClmEnvelopeTransaction {
	@JsonProperty("envelopeId")
	private String envelopeId = null;

	@JsonProperty("errorDetails")
	private ErrorDetails errorDetails = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("transactionId")
	private String transactionId = null;
}
