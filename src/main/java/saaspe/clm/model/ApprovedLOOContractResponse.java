package saaspe.clm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ApprovedLOOContractResponse {
	
	@NonNull
	private String envelopeId;
	private String contractName;

}
