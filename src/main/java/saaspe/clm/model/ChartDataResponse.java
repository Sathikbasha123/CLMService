package saaspe.clm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataResponse {

	    private long completedCount;
	    private long declinedCount;
	    private long createdCount;
	    private long voidedCount;
	    private long sentCount;
	    private long expiredCount;
}
