package saaspe.clm.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.storage.StorageException;
import saaspe.clm.aspect.ControllerLogging;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.service.EventService;

@RestController
@RequestMapping("/docusign")
@ControllerLogging
public class EventController {

	@Autowired
	private EventService eventService;

	private static final Logger log = LoggerFactory.getLogger(EventController.class);

	@PostMapping("/events")
	public String handleEvent(@RequestBody String body)
			throws JsonProcessingException, ParseException, DataValidationException, URISyntaxException, StorageException {
		log.info("handleEvent start");
		try {
			eventService.handleEvent(body);
		} catch (ParseException | DataValidationException | IOException e) {
			e.printStackTrace();
		}
		log.info("handleEvent end");
		return null;
	}

	//@Scheduled(cron = "0 */15 * ? * *")
	public CommonResponse getAuditEventsFromDocusign() throws JsonProcessingException {
		log.info("getAuditEventsFromDocusign log");
		return eventService.getAuditEventsFromDocusign();
	}

	@GetMapping("/audit/{envelopeId}")
	public CommonResponse getAuditEvents(@PathVariable String envelopeId) {
		log.info("getAuditEvents log");
		return eventService.getAuditEvents(envelopeId);
	}

}
