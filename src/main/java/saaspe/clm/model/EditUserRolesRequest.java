package saaspe.clm.model;

import lombok.Data;

import java.util.List;

@Data
public class EditUserRolesRequest {
    private String uniqueId;
    private List<String> newRoles;
    private String division;
}
