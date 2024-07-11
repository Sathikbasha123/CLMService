package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonSetter;

public class CreateTemplateModel {
	private String name;
	private String templateId;

	public String getName() {
		return name;
	}

	@JsonSetter("name")
	public void setName(String name) {
		this.name = name;
	}

	public String getTemplateId() {
		return templateId;
	}

	@JsonSetter("templateId")
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

}
