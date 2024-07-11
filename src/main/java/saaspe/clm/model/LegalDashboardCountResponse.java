package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalDashboardCountResponse {
    private long totalDocument;
    private long completedCvef;
    private long rejectedCvef;
    private long signaturePendingCvef;
    private long stampedContract;
    private long stamperPendingContract;
    private long lhdnCompletedContract;
    private long lhdnPendingContract;

}
