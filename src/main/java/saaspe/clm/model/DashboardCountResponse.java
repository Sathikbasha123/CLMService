package saaspe.clm.model;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardCountResponse {

	
    private long totalDocument;
    private long inProgressDocument;
    private long completedDocument;
    private long reviewed;
    private long pendingForReviewDocument;
    private long totalReview;
    private ChartDataResponse chartData;
}
