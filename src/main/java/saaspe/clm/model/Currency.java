package saaspe.clm.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

	private String currencyCode;
	private BigDecimal totalCost;
	private String tax;
}
