package saaspe.clm.model;

import java.util.List;

import lombok.Data;

@Data
public class EnvelopeResponse {

	private Object envelope;

	private String envelopeId;

	private List<DocumentResponse> documents;

}
