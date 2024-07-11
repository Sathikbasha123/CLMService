package saaspe.clm.model;

import lombok.Data;

@Data
public class UserAccessRoleResponse extends Response {

    private String role;
    private String emailAddress;
    private String[] access;
    private String currency;
    private Clm clm;
    private Boolean isClmUser;
    private Boolean isActiveClmUser;
    private Boolean isDocusignRequired;
    private Boolean isDocusignAccountActivated;
    private Boolean isConsentSubmitted;
    private String consentUrl;
    private Boolean isOnboardedUser;
    private Boolean isUserOnboardApproved;
    private String name;
    private String status;
    private String rejectionReason;

    public UserAccessRoleResponse(){
        this.isClmUser = false;
        this.isActiveClmUser = false;
        this.isDocusignRequired = false;
        this.isDocusignAccountActivated = false;
        this.isConsentSubmitted = false;
        this.isOnboardedUser = false;
        this.isUserOnboardApproved = false;
    }
}
