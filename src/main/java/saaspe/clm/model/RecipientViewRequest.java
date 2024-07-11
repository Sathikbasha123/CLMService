package saaspe.clm.model;

import lombok.Data;

@Data
public class RecipientViewRequest {

	private String userEmail;
	private String userId;
	private String clientUserId;
}
