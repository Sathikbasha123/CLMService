package saaspe.clm.utills;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AllRoleFlowMapper {
	public static final Map<String, String> roleFlowMappings = new HashMap<>();
	public static final List<String> REVIEWERS = Arrays.asList("LOA_REVIEWER", "CONTRACT_REVIEWER", "LOO_REVIEWER",
			"TA_REVIEWER", "CVEF_REVIEWER", "CMU_REVIEWER");
	static {
		roleFlowMappings.put("LOA_CREATOR", "LOA_Create");
		roleFlowMappings.put("CONTRACT_CREATOR", "CONTRACT_create");
		roleFlowMappings.put("CONTRACT_REVIEWER", "CONTRACT_review");
		roleFlowMappings.put("LOA_REVIEWER", "LOA_Review");
		roleFlowMappings.put("CMU_CREATOR", "CMU_Create");
		roleFlowMappings.put("CMU_REVIEWER", "CMU_Review");
		roleFlowMappings.put("LOO_CREATOR", "LOO_Create");
		roleFlowMappings.put("LOO_REVIEWER", "LOO_Review");
		roleFlowMappings.put("TA_CREATOR", "TA_Create");
		roleFlowMappings.put("TA_REVIEWER", "TA_Review");
		roleFlowMappings.put("PCD_ADMIN", "PCD_ADMIN");
		roleFlowMappings.put("COMMERCIAL_ADMIN", "PCD_ADMIN");

	}

	public static String getFlowType(String roleName) {
		return roleFlowMappings.getOrDefault(roleName.toUpperCase(), "null");
	}

}
