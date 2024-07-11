package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Folder {

	@JsonProperty("errorDetails")
	private ErrorDetails errorDetails = null;

	@JsonProperty("filter")
	private Filter filter = null;

	@JsonProperty("folderId")
	private String folderId = null;

	@JsonProperty("folderItems")
	private java.util.List<FolderItemV2> folderItems = null;

	@JsonProperty("folders")
	private java.util.List<Folder> folders = null;

	@JsonProperty("hasAccess")
	private String hasAccess = null;

	@JsonProperty("hasSubFolders")
	private String hasSubFolders = null;

	@JsonProperty("itemCount")
	private String itemCount = null;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("owner")
	private UserInfo owner = null;

	@JsonProperty("parentFolderId")
	private String parentFolderId = null;

	@JsonProperty("parentFolderUri")
	private String parentFolderUri = null;

	@JsonProperty("subFolderCount")
	private String subFolderCount = null;

	@JsonProperty("type")
	private String type = null;

	@JsonProperty("uri")
	private String uri = null;

}
