package saaspe.clm.model;

import java.util.List;

import lombok.Data;

@Data
public class UserApprovalRequest {
	private String uniqueId;
	private List<String> approved_roles;

}
