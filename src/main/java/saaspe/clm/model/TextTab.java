package saaspe.clm.model;

import java.util.UUID;

import lombok.Data;

@Data
public class TextTab {

	 private String requireAll;
	    private String value;
	    private String concealValueOnDocument;
	    private String disableAutoSize;
	    private String tabLabel;
	    private LocalePolicy localePolicy;
	    private String documentID;
	    private String recipientID;
	    private String pageNumber;
	    private String xPosition;
	    private String yPosition;
	    private String width;
	    private String height;
	    private UUID tabID;
	    private String tabType;
}
