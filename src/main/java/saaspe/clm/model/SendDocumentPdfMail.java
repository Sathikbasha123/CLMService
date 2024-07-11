package saaspe.clm.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class SendDocumentPdfMail {

	private Long contractId;
	private List<String> toMailAddress;
	private List<String> ccMailAddress;
	
}
