package saaspe.clm.model;

import lombok.Data;

import java.util.List;

@Data
public class UserRoleResponse {
    private String uniqueId;
    private String division;
    private List<String> roles;
}
