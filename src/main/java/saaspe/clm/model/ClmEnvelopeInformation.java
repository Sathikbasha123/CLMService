package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ClmEnvelopeInformation {

	@JsonProperty("continuationToken")
	private String continuationToken = null;

	@JsonProperty("endPosition")
	private String endPosition = null;

	@JsonProperty("envelopes")
	private java.util.List<Envelope> envelopes = null;

	@JsonProperty("envelopeTransactionStatuses")
	private java.util.List<ClmEnvelopeTransaction> envelopeTransactionStatuses = null;

	@JsonProperty("folders")
	private java.util.List<Folder> folders = null;

	@JsonProperty("lastQueriedDateTime")
	private String lastQueriedDateTime = null;

	@JsonProperty("nextUri")
	private String nextUri = null;

	@JsonProperty("previousUri")
	private String previousUri = null;

	@JsonProperty("resultSetSize")
	private String resultSetSize = null;

	@JsonProperty("startPosition")
	private String startPosition = null;

	@JsonProperty("totalSetSize")
	private String totalSetSize = null;

}
