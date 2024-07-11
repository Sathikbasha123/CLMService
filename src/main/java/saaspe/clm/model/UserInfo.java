package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class UserInfo {

	@JsonProperty("accountId")
	private String accountId = null;

	@JsonProperty("accountName")
	private String accountName = null;

	@JsonProperty("activationAccessCode")
	private String activationAccessCode = null;

	@JsonProperty("email")
	private String email = null;

	@JsonProperty("errorDetails")
	private ErrorDetails errorDetails = null;

	@JsonProperty("loginStatus")
	private String loginStatus = null;

	@JsonProperty("membershipId")
	private String membershipId = null;

	@JsonProperty("sendActivationEmail")
	private String sendActivationEmail = null;

	@JsonProperty("uri")
	private String uri = null;

	@JsonProperty("userId")
	private String userId = null;

	@JsonProperty("userName")
	private String userName = null;

	@JsonProperty("userStatus")
	private String userStatus = null;

	@JsonProperty("userType")
	private String userType = null;

}
