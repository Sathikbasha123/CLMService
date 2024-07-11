package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateResponse {

    private String templateId;

    private String templateName;

    private String flowType;

    private String ownerName;

    private String ownerEmail;

    private Date createdOn;

    private String updateByName;

    private String updateByEmail;

    private Date updatedOn;
}
