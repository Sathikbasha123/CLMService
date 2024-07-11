package saaspe.clm.model;

import lombok.Data;

import java.util.List;

@Data
public class GraphGroupsResponse {

	private String odataContext;
	private List<Value> value;

}
