package saaspe.clm.model;

import lombok.Data;

@Data
public class CurrentUserRoleUpdateRequest {
    private String uniqueId;
    private String currentRole;
}