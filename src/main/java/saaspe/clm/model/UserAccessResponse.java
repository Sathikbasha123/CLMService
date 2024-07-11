package saaspe.clm.model;

import lombok.Data;

@Data
public class UserAccessResponse {
    private boolean isUserExistInGroup;

    private boolean isClmUser;

    private boolean isActiveClmUser;

    private boolean isDocusignRequired;

    private boolean isDocusignAccountActivated;

    private boolean isConsentSubmitted;

    private String consentUrl;
}
