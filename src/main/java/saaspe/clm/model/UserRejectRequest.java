package saaspe.clm.model;

import lombok.Data;

@Data
public class UserRejectRequest {
    private String uniqueId;
    private String rejectionReason;
}