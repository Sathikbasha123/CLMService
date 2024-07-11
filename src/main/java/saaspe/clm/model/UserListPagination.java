package saaspe.clm.model;

import java.util.List;

import lombok.Data;
@Data
public class UserListPagination {
	private long total;
	private List<UserListResponse> records;

}
