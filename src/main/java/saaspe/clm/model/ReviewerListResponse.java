package saaspe.clm.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ReviewerListResponse {
	private String name;
	private String emailAddress;
	private boolean status;
	private Integer reviewingOrder;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date reviewedAt;

}
