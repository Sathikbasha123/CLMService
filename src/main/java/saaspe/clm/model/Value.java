package saaspe.clm.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class Value {
	
	private String id;
    private Object deletedDateTime;
    private Object classification;
    private OffsetDateTime createdDateTime;
    private Object[] creationOptions;
    private String description;
    private String displayName;
    private Object expirationDateTime;
    private Object[] groupTypes;
    private boolean isAssignableToRole;
    private Object mail;
    private boolean mailEnabled;
    private String mailNickname;
    private Object membershipRule;
    private Object membershipRuleProcessingState;
    private Object onPremisesDomainName;
    private Object onPremisesLastSyncDateTime;
    private Object onPremisesNetBIOSName;
    private Object onPremisesSamAccountName;
    private Object onPremisesSecurityIdentifier;
    private Object onPremisesSyncEnabled;
    private Object preferredDataLocation;
    private Object preferredLanguage;
    private Object[] proxyAddresses;
    private OffsetDateTime renewedDateTime;
    private Object[] resourceBehaviorOptions;
    private Object[] resourceProvisioningOptions;
    private boolean securityEnabled;
    private String securityIdentifier;
    private Object theme;
    private String visibility;
    private Object[] onPremisesProvisioningErrors;

}
