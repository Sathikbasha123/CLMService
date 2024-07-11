package saaspe.clm.model;

import lombok.Data;

@Data
public class DocusignCreateUserResponse {
	private String id;
	private float siteId;
	private String firstName;
	private String userName;
	private String lastName;
	private String email;
	private String languageCulture;
	private Object accounts;
	private String site_id;
	private String user_name;
	private String first_name;
	private String last_name;
	private String language_culture;
	private Object error;

}
