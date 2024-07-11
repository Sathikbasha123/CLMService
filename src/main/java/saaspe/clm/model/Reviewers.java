package saaspe.clm.model;

import java.util.Date;

import lombok.Data;

@Data
public class Reviewers {
	private String name;
	private String email;
	private int routingOrder;
	private String status;
	private Date modifiedAt;
}
