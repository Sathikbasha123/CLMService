package saaspe.clm.utills;

import lombok.Data;

import java.util.Date;

@Data
public class TokenCache {
    private String token;
    private String emailAddress;
    private String displayname;
    private Date expiryDate;
}
