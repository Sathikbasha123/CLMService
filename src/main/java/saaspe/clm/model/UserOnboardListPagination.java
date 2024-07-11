package saaspe.clm.model;

import java.util.List;

import lombok.Data;

@Data
public class UserOnboardListPagination {
	private long total;
	private List<UserOnboardingListResponse> records;

}
