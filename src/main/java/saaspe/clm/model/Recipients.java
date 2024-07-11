package saaspe.clm.model;

import lombok.Data;

@Data
public class Recipients {

	 private Signer[] signers;
	    private Object[] agents;
	    private Object[] editors;
	    private Object[] intermediaries;
	    private CarbonCopy[] carbonCopies;
	    private Object[] certifiedDeliveries;
	    private Object[] inPersonSigners;
	    private Object[] seals;
	    private Object[] witnesses;
	    private Object[] notaries;
	    private String recipientCount;
	    private String currentRoutingOrder;
}
