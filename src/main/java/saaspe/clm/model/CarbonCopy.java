package saaspe.clm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class CarbonCopy {

	private String name;
	private String email;
	private String recipientID;
	private UUID recipientIDGUID;
	private String requireIDLookup;
	private UUID userID;
	private String routingOrder;
	private String status;
	private String completedCount;
	private OffsetDateTime sentDateTime;
	private String deliveryMethod;
	private String totalTabCount;
	private String recipientType;
}
