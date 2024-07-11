package saaspe.clm.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardViewResponce {

	List<ExpiringContractResponce> expiredContracts;
	List<LatestContractResponce> latestContracts;
	private int totalContracts;
	private int signCompleted;
	private int signInProgress;
}
