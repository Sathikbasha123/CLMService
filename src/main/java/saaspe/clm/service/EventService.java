package saaspe.clm.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.storage.StorageException;

import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;

public interface EventService {

	String handleEvent(String body) throws JsonProcessingException, ParseException, DataValidationException, IOException, URISyntaxException, StorageException;

	CommonResponse getAuditEventsFromDocusign() throws JsonProcessingException;

	CommonResponse getAuditEvents(String envelopeId);

}
