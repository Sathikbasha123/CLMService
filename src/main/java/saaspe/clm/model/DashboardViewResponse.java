package saaspe.clm.model;

import lombok.Data;

@Data
public class DashboardViewResponse {

	private String totalLoa;
	private String reviewedLoa;
	private String pendingLoa;

}
