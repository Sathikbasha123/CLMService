package saaspe.clm.model;

import lombok.Data;

@Data
public class UserCurrentRoleResponse {
    private String uniqueId;
    private String currentRole;
}
