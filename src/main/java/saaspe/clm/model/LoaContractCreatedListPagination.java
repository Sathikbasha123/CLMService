package saaspe.clm.model;

import java.util.List;

import lombok.Data;

@Data
public class LoaContractCreatedListPagination {

	private Long total;
	private List<LoaContractCreatedResponse> records;
}
