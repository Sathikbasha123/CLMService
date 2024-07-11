package saaspe.clm.model;

import java.util.List;
import lombok.Data;

@Data
public class UserOnboardingRequest {

	private String name;
	private String email;
	private String division;
	private List<String> requestRoles;
	private String status;

}
