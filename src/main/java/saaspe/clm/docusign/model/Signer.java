package saaspe.clm.docusign.model;

import lombok.Data;

@Data
public class Signer {

	private String email;
	private String name;
	private String routingOrder;
	private String recipientType;
	private Tabs tabs;
}
