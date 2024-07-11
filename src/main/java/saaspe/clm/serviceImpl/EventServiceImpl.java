package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.StorageException;

import freemarker.template.TemplateException;
import lombok.Data;
import saaspe.clm.configuration.mongo.SequenceGeneratorService;
import saaspe.clm.constant.Constant;
import saaspe.clm.document.AuditEventDocument;
import saaspe.clm.document.CentralRepoDocument;
import saaspe.clm.document.CmuContractDocument;
import saaspe.clm.document.CvefDocument;
import saaspe.clm.document.EnvelopeDocument;
import saaspe.clm.document.LoaContractDocument;
import saaspe.clm.document.LoaDocument;
import saaspe.clm.document.LooContractDocument;
import saaspe.clm.document.ReviewerDocument;
import saaspe.clm.document.TADocument;
import saaspe.clm.document.WorkFlowCreatorDocument;
import saaspe.clm.exception.DataValidationException;
import saaspe.clm.model.AllSigners;
import saaspe.clm.model.AuditEvent;
import saaspe.clm.model.AuditEventsResponse;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.DocumentResponse;
import saaspe.clm.model.DocusignUrls;
import saaspe.clm.model.EnvelopeResponse;
import saaspe.clm.model.EventFieldsList;
import saaspe.clm.model.EventLogs;
import saaspe.clm.model.Response;
import saaspe.clm.model.Signer;
import saaspe.clm.repository.AuditEventRepository;
import saaspe.clm.repository.CentralRepoDocumentRepository;
import saaspe.clm.repository.CmuContractDocumentRepository;
import saaspe.clm.repository.CvefDocumentRepository;
import saaspe.clm.repository.EnvelopeRepository;
import saaspe.clm.repository.LoaContractDocumentRepository;
import saaspe.clm.repository.LoaDocumentRepository;
import saaspe.clm.repository.LooContractDocumentRepository;
import saaspe.clm.repository.ReviewerDocumentRepository;
import saaspe.clm.repository.TADocumentRepository;
import saaspe.clm.repository.WorkFlowCreatorDocumentRespository;
import saaspe.clm.service.EventService;
import saaspe.clm.service.MailSenderService;

@Service
public class EventServiceImpl implements EventService {

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Autowired
	private EnvelopeRepository envelopeRepository;

	@Autowired
	private AuditEventRepository auditEventRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private LoaDocumentRepository loaDocumentRepository;

	@Autowired
	private LoaContractDocumentRepository loaContractDocumentRepository;

	@Autowired
	private CmuContractDocumentRepository cmuContractDocumentRepository;

	@Autowired
	private LooContractDocumentRepository looContractDocumentRepository;

	@Autowired
	private CvefDocumentRepository cvefDocumentRepository;

	@Autowired
	private TADocumentRepository documentRepository;

	@Autowired
	private CentralRepoDocumentRepository centralRepoDocumentRepository;

	@Autowired
	private ReviewerDocumentRepository reviewerDocumentRepository;

	@Autowired
	private WorkFlowCreatorDocumentRespository workFlowCreatorDocumentRespository;

//	@Autowired
//	private DocumentVersionDocumentRepository documentVersionDocumentRepository;
//
//	@Autowired
//	private CLMServiceImpl clmServiceImpl;

	@Autowired
	private MailSenderService mailSenderService;

//	@Autowired
//	private CloudBlobClient cloudBlobClient;

	@Value("${azure.storage.container.name}")
	private String containerName;

	@Value("${docusign-urls-file}")
	private String docusignUrls;

	@Value("${docusign.host}")
	private String docusignHost;

	private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

	public DocusignUrls getDousignUrl() {
		ClassPathResource resource = new ClassPathResource(docusignUrls);
		DocusignUrls docusignUrl = null;
		try {
			docusignUrl = mapper.readValue(resource.getInputStream(), DocusignUrls.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return docusignUrl;
	}

	@Override
	public String handleEvent(String body)
			throws ParseException, DataValidationException, IOException, URISyntaxException, StorageException {
		System.out.println(body);
		EventLogs eventLogs = mapper.readValue(body, EventLogs.class);
// EventDocument document = new EventDocument();
//		document.setId(sequenceGeneratorService.generateSequence(EventDocument.SEQUENCE_NAME));
//		document.setCreatedOn(new Date());
//		document.setStartDate(new Date());
//		document.setEvent(eventLogs.getEvent());
//		document.setUri(eventLogs.getUri());
//		document.setApiVersion(eventLogs.getApiVersion());
//		document.setConfigurationId(eventLogs.getConfigurationId());
//		document.setGeneratedDateTime(eventLogs.getGeneratedDateTime());
//		document.setRetryCount(eventLogs.getRetryCount());
//		EventData logs = new EventData();
//		logs.setAccountId(eventLogs.getData().getAccountId());
//		logs.setCreated(eventLogs.getData().getCreated());
//		logs.setEnvelopeId(eventLogs.getData().getEnvelopeId());
//		logs.setName(eventLogs.getData().getName());
//		logs.setRecipientId(eventLogs.getData().getRecipientId());
//		logs.setTemplateId(eventLogs.getData().getTemplateId());
//		logs.setUserId(eventLogs.getData().getUserId());
		// document.setData(logs);
		// eventRepository.save(document);
		if (eventLogs.getData().getEnvelopeId() != null) {
			EnvelopeDocument existingEnvelopeDocument = envelopeRepository
					.findByenvelopeId(eventLogs.getData().getEnvelopeId());
			String getEnvelopeById = getDousignUrl().getGetEnvelopeById().replace("{docusignHost}", docusignHost.trim())
					+ eventLogs.getData().getEnvelopeId();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
			try {
				envelopeDataResponse = restTemplate.exchange(getEnvelopeById, HttpMethod.GET, httpEntity,
						EnvelopeResponse.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				return "Unable to save updated envelope from event";
			}
			String envelopeId = eventLogs.getData().getEnvelopeId();
			String json1 = mapper.writeValueAsString(envelopeDataResponse.getBody().getEnvelope());
			JsonNode rootNode = mapper.readTree(json1);
			String lastModifiedText = rootNode.path("lastModifiedDateTime").asText();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			Date lastModifiedDate = null;
			lastModifiedDate = dateFormat.parse(lastModifiedText);
			CentralRepoDocument centralRepoDocument = centralRepoDocumentRepository.findByEnvelopeId(envelopeId);
			String repositoryName = (centralRepoDocument != null) ? centralRepoDocument.getRepositoryName() : "null";
			List<Signer> signersList = new ArrayList<>();
			List<Signer> watchersList = new ArrayList<>();
			Set<String> uniqueMail = new HashSet<>();
			switch (repositoryName) {
			case Constant.LOA_DOCUMENT_REPOSITORY:
				LoaDocument loaDocument = loaDocumentRepository.findByEnvelopeId(envelopeId);
				if (loaDocument != null) {
					JsonNode eventDataNode = mapper.valueToTree(eventLogs.getData());
					JsonNode signersNode = eventDataNode.path("envelopeSummary").path("recipients").path("signers");
					JsonNode watchersNode = eventDataNode.path("envelopeSummary").path("recipients")
							.path("carbonCopies");
					if (!watchersNode.isMissingNode() && watchersNode.isArray()) {
						for (JsonNode watcherNode : watchersNode) {
							Signer signer = new Signer();
							signer.setEmail(watcherNode.path(Constant.EMAIL).asText());
							signer.setName(watcherNode.path("name").asText());
							signer.setRoutingOrder(watcherNode.path("routingOrder").asText());
							signer.setStatus(watcherNode.path(Constant.STATUS).asText());
							watchersList.add(signer);
							uniqueMail.add(watcherNode.path(Constant.EMAIL).asText());
						}
					}
					
					log.info("WatchersList size : {}",watchersList.size());
					log.info("MailList size : {}",uniqueMail.size());
					if (!signersNode.isMissingNode() && signersNode.isArray()) {
						for (JsonNode signerNode : signersNode) {
							Signer signer = new Signer();
							signer.setEmail(signerNode.path(Constant.EMAIL).asText());
							signer.setName(signerNode.path("name").asText());
							signer.setRoutingOrder(signerNode.path("routingOrder").asText());
							signer.setStatus(signerNode.path(Constant.STATUS).asText());
							signersList.add(signer);
						}
					}
					for (Signer signer : signersList) {
						String signerStatus = signer.getStatus().trim();
						if (signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2))
								|| signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))) {
							loaDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(signer.getStatus()));

						} else {
							loaDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(Constant.IN_PROGRESS));
						}
					}
					if (loaDocument.getIsSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = loaDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Signer")
										&& e.getSigningStatus().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners a = loaDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Signer") && te
											.getSigningStatus().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findAny().orElse(null);
							if (a == null) {
								loaDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								loaDocument.setIsVendorsCompleted(Constant.IN_PROGRESS);
								if (!watchersList.isEmpty() && !loaDocument.isWatcherEmailStatus()) {
									for (Signer watcher : watchersList) {
										if (uniqueMail.contains(watcher.getEmail()))
											try {
												log.info("watcher :",watcher.getEmail());
												mailSenderService.sendMailToWatchers(watcher.getName(),
														loaDocument.getProjectName(), watcher.getEmail(), envelopeId,
														"pcd", Constant.LOA_MODULE);
												uniqueMail.remove(watcher.getEmail());
												loaDocument.setWatcherEmailStatus(true);
											} catch (IOException | TemplateException | MessagingException e1) {
												e1.printStackTrace();
											}
									}
								}
							} else {
								loaDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								loaDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								loaDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								loaDocument.getAllSigners().stream().filter(
										e -> (e.getRecipientType().equalsIgnoreCase("Vendor") || e.getRecipientType()
												.replaceAll("[_]", "").equalsIgnoreCase("LHDNStamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (loaDocument.getIsVendorsCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = loaDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Vendor")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = loaDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Vendor")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								loaDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								loaDocument.setIsLhdnSignersCompleted(Constant.IN_PROGRESS);
							} else {
								loaDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								loaDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								loaDocument.getAllSigners().stream()
										.filter(e -> (e.getRecipientType().replaceAll("[_]", "")
												.equalsIgnoreCase("LHDNStamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (loaDocument.getIsLhdnSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = loaDocument.getAllSigners().stream().filter(
								e -> e.getRecipientType().replaceAll("[_]", "").trim().equalsIgnoreCase("LHDNStamper")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = loaDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().replaceAll("[_]", "").trim()
											.equalsIgnoreCase("LHDNStamper")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								loaDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
							} else {
								loaDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
							}
						}
					}
					loaDocument.setStatus(rootNode.path("status").asText());
					if (rootNode.path("status").asText().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))) {
//						List<DocumentVersionDocument> documentLatestVersion = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
//						DocumentVersionDocument latestDocumentVersion = documentLatestVersion.stream()
//								.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion))
//								.orElseThrow(() -> new NoSuchElementException("No document version found"));
//						for(Document document:latestDocumentVersion.getDocuments())
//						{
//							CommonResponse documentResponse=clmServiceImpl.getEnvelopeDocument(envelopeId, document.getDocumentId());
//							String documentBase64 = mapper.readValue(
//									objectMapper.writeValueAsString(documentResponse.getResponse().getData()),String.class);
//							MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentBase64,
//									document.getName(), "text/plain");
//							uploadFilesIntoBlob(multipartFile, envelopeId,document.getDocumentId());
// 						}
						loaDocument.setCompleted(true);
					}
					if (lastModifiedDate != null) {
						loaDocument.setLastModifiedDateTime(lastModifiedDate);
					}
					loaDocumentRepository.save(loaDocument);
					List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository
							.findByEnvelopeId(envelopeId);
					reviewerDocumentList.stream().forEach(reviewDoc -> {
						reviewDoc.setStatus(rootNode.path(Constant.STATUS).asText());
						reviewerDocumentRepository.save(reviewDoc);
					});
					WorkFlowCreatorDocument workFlowDocument = workFlowCreatorDocumentRespository
							.findByEnvelopeId(envelopeId);
					workFlowDocument.setStatus(rootNode.path(Constant.STATUS).asText());
					workFlowCreatorDocumentRespository.save(workFlowDocument);
				}
				break;
			case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
				LoaContractDocument contractDocument = loaContractDocumentRepository.findByEnvelopeId(envelopeId);
				if (contractDocument != null) {
					JsonNode eventDataNode = mapper.valueToTree(eventLogs.getData());
					JsonNode signersNode = eventDataNode.path("envelopeSummary").path("recipients").path("signers");
					JsonNode watchersNode = eventDataNode.path("envelopeSummary").path("recipients")
							.path("carbonCopies");
					if (!watchersNode.isMissingNode() && watchersNode.isArray()) {
						for (JsonNode watcherNode : watchersNode) {
							Signer signer = new Signer();
							signer.setEmail(watcherNode.path(Constant.EMAIL).asText());
							signer.setName(watcherNode.path("name").asText());
							signer.setRoutingOrder(watcherNode.path("routingOrder").asText());
							signer.setStatus(watcherNode.path(Constant.STATUS).asText());
							watchersList.add(signer);
							uniqueMail.add(watcherNode.path(Constant.EMAIL).asText());
						}
					}
					log.info("WatchersList size : {}",watchersList.size());
					log.info("MailList size : {}",uniqueMail.size());
					if (!signersNode.isMissingNode() && signersNode.isArray()) {
						for (JsonNode signerNode : signersNode) {
							Signer signer = new Signer();
							signer.setEmail(signerNode.path("email").asText());
							signer.setName(signerNode.path("name").asText());
							signer.setRoutingOrder(signerNode.path("routingOrder").asText());
							signer.setStatus(signerNode.path("status").asText());
							signersList.add(signer);
						}
					}
					for (Signer signer : signersList) {
						String signerStatus = signer.getStatus();
						if (signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2))
								|| signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))) {
							contractDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(signer.getStatus()));
						} else {
							contractDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(Constant.IN_PROGRESS));
						}

					}
					if (contractDocument.getIsCvefSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = contractDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("CVEF_Signer")
										&& e.getSigningStatus().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners a = contractDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("CVEF_Signer") && te
											.getSigningStatus().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findAny().orElse(null);
							if (a == null) {
								contractDocument.setIsCvefSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								contractDocument.setIsStampersCompleted(Constant.IN_PROGRESS);
							} else {
								contractDocument.setIsCvefSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsStampersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.getAllSigners().stream()
										.filter(e -> (e.getRecipientType().equalsIgnoreCase("Signer")
												|| e.getRecipientType().equalsIgnoreCase("Stamper")
												|| e.getRecipientType().equalsIgnoreCase("Vendor")
												|| e.getRecipientType().equalsIgnoreCase("LHDN_Stamper")))

										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (contractDocument.getIsStampersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = contractDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Stamper")
										&& e.getSigningStatus().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = contractDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Stamper") && te
											.getSigningStatus().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								contractDocument.setIsStampersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								contractDocument.setIsSignersCompleted(Constant.IN_PROGRESS);
							} else {
								contractDocument.setIsStampersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.getAllSigners().stream()
										.filter(e -> (e.getRecipientType().equalsIgnoreCase("Signer")
												|| e.getRecipientType().equalsIgnoreCase("Vendor")
												|| e.getRecipientType().equalsIgnoreCase("LHDN_Stamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (contractDocument.getIsSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = contractDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Signer")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = contractDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Signer")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								contractDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								contractDocument.setIsVendorsCompleted(Constant.IN_PROGRESS);
							} else {
								contractDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.getAllSigners().stream().filter(
										e -> (e.getRecipientType().equalsIgnoreCase("Vendor") || e.getRecipientType()
												.replaceAll("[_]", "").equalsIgnoreCase("LHDNStamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (contractDocument.getIsVendorsCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = contractDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Vendor")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = contractDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Vendor")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								contractDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								contractDocument.setIsLhdnSignersCompleted(Constant.IN_PROGRESS);
							} else {
								contractDocument.setIsVendorsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								contractDocument.getAllSigners().stream()
										.filter(e -> (e.getRecipientType().replaceAll("[_]", "")
												.equalsIgnoreCase("LHDNStamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (contractDocument.getIsLhdnSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = contractDocument.getAllSigners().stream().filter(
								e -> e.getRecipientType().replaceAll("[_]", "").trim().equalsIgnoreCase("LHDNStamper")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = contractDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().replaceAll("[_]", "").trim()
											.equalsIgnoreCase("LHDNStamper")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								contractDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
							} else {
								contractDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
							}
						}
					}
					if (contractDocument.getIsSignersCompleted().trim()
							.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))
							&& contractDocument.getIsStampersCompleted().trim()
									.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))
							&& contractDocument.getIsCvefSignersCompleted().trim()
									.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0)) && !contractDocument.isWatcherEmailStatus()&&!watchersList.isEmpty()) {
							for (Signer watcher : watchersList) {
								if (uniqueMail.contains(watcher.getEmail())) {
									try {
										log.info("watcher :",watcher.getEmail());
										mailSenderService.sendMailToWatchers(watcher.getName(),
												contractDocument.getProjectName(), watcher.getEmail(), envelopeId,
												"pcd", Constant.CONTRACT_MODULE);
										uniqueMail.remove(watcher.getEmail());
										contractDocument.setWatcherEmailStatus(true);
										} catch (IOException | TemplateException | MessagingException e1) {
										e1.printStackTrace();
									}
								}
							}
					}
					contractDocument.setStatus(rootNode.path("status").asText());
//					if (rootNode.path("status").asText().equalsIgnoreCase("completed")) {
//						List<DocumentVersionDocument> documentLatestVersion = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
//						DocumentVersionDocument latestDocumentVersion = documentLatestVersion.stream()
//								.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion))
//								.orElseThrow(() -> new NoSuchElementException("No document version found"));
//						for(Document document:latestDocumentVersion.getDocuments())
//						{
//							CommonResponse documentResponse=clmServiceImpl.getEnvelopeDocument(envelopeId, document.getDocumentId());
//							String documentBase64 = mapper.readValue(
//									objectMapper.writeValueAsString(documentResponse.getResponse().getData()),String.class);
//							MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentBase64,
//									document.getName(), "text/plain");
//							uploadFilesIntoBlob(multipartFile, envelopeId,document.getDocumentId());
// 						}
//					}
					if (lastModifiedDate != null) {
						contractDocument.setLastModifiedDateTime(lastModifiedDate);
					}
					loaContractDocumentRepository.save(contractDocument);
					List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository
							.findByEnvelopeId(envelopeId);
					reviewerDocumentList.stream().forEach(reviewDoc -> {
						reviewDoc.setStatus(rootNode.path("status").asText());
						reviewerDocumentRepository.save(reviewDoc);
					});
					WorkFlowCreatorDocument workFlowDocument = workFlowCreatorDocumentRespository
							.findByEnvelopeId(envelopeId);
					workFlowDocument.setStatus(rootNode.path("status").asText());
					workFlowCreatorDocumentRespository.save(workFlowDocument);
				}
				break;
			case Constant.CMU_DOCUMENT_REPOSITORY:
				CmuContractDocument cmuDocument = cmuContractDocumentRepository.findByEnvelopeId(envelopeId);
				if (cmuDocument != null) {
					cmuDocument.setStatus(rootNode.path("status").asText());
					if (lastModifiedDate != null) {
						cmuDocument.setLastModifiedDateTime(lastModifiedDate);
					}
					cmuContractDocumentRepository.save(cmuDocument);
					List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository
							.findByEnvelopeId(envelopeId);
					reviewerDocumentList.stream().forEach(reviewDoc -> {
						reviewDoc.setStatus(rootNode.path("status").asText());
						reviewerDocumentRepository.save(reviewDoc);
					});
					WorkFlowCreatorDocument workFlowDocument = workFlowCreatorDocumentRespository
							.findByEnvelopeId(envelopeId);
					workFlowDocument.setStatus(rootNode.path("status").asText());
//					if (rootNode.path("status").asText().equalsIgnoreCase("completed")) {
//						List<DocumentVersionDocument> documentLatestVersion = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
//						DocumentVersionDocument latestDocumentVersion = documentLatestVersion.stream()
//								.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion))
//								.orElseThrow(() -> new NoSuchElementException("No document version found"));
//						for(Document document:latestDocumentVersion.getDocuments())
//						{
//							CommonResponse documentResponse=clmServiceImpl.getEnvelopeDocument(envelopeId, document.getDocumentId());
//							String documentBase64 = mapper.readValue(
//									objectMapper.writeValueAsString(documentResponse.getResponse().getData()),String.class);
//							MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentBase64,
//									document.getName(), "text/plain");
//							uploadFilesIntoBlob(multipartFile, envelopeId,document.getDocumentId());
// 						}
//					}
					workFlowCreatorDocumentRespository.save(workFlowDocument);
				}
				break;
			case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
				LooContractDocument looDocument = looContractDocumentRepository.findByEnvelopeId(envelopeId);
				if (looDocument != null) {
					JsonNode eventDataNode = mapper.valueToTree(eventLogs.getData());
					JsonNode signersNode = eventDataNode.path("envelopeSummary").path("recipients").path("signers");
					JsonNode watchersNode = eventDataNode.path("envelopeSummary").path("recipients")
							.path("carbonCopies");
					if (!watchersNode.isMissingNode() && watchersNode.isArray()) {
						for (JsonNode watcherNode : watchersNode) {
							Signer signer = new Signer();
							signer.setEmail(watcherNode.path("email").asText());
							signer.setName(watcherNode.path("name").asText());
							signer.setRoutingOrder(watcherNode.path("routingOrder").asText());
							signer.setStatus(watcherNode.path("status").asText());
							watchersList.add(signer);
							uniqueMail.add(watcherNode.path(Constant.EMAIL).asText());
						}
					}
					log.info("WatchersList size : {}",watchersList.size());
					log.info("MailList size : {}",uniqueMail.size());
					if (!signersNode.isMissingNode() && signersNode.isArray()) {
						for (JsonNode signerNode : signersNode) {
							Signer signer = new Signer();
							signer.setEmail(signerNode.path("email").asText());
							signer.setName(signerNode.path("name").asText());
							signer.setRoutingOrder(signerNode.path("routingOrder").asText());
							signer.setStatus(signerNode.path("status").asText());
							signersList.add(signer);
						}
					}
					for (Signer signer : signersList) {
						String signerStatus = signer.getStatus();
						if (signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2))
								|| signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))) {
							looDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(signer.getStatus()));

						} else {
							looDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(Constant.IN_PROGRESS));
						}
					}
					if (looDocument.getIsSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = looDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Signer")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners a = looDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Signer")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findAny().orElse(null);
							if (a == null) {
								looDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								looDocument.setIsTenantsCompleted(Constant.IN_PROGRESS);
								if (!watchersList.isEmpty()&& !looDocument.isWatcherEmailStatus()) {
									for (Signer watcher : watchersList) {
										if (uniqueMail.contains(watcher.getEmail())) {
											try {
												log.info("watcher :",watcher.getEmail());
												mailSenderService.sendMailToWatchers(watcher.getName(),
														looDocument.getContractTitle(), watcher.getEmail(), envelopeId,
														"commercial", Constant.LOO_MODULE);
												uniqueMail.remove(watcher.getEmail());
												looDocument.setWatcherEmailStatus(true);
											} catch (IOException | TemplateException | MessagingException e1) {
												e1.printStackTrace();
											}
										}
									}
								}
							} else {
								looDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								looDocument.setIsTenantsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								looDocument.getAllSigners().stream()
										.filter(e -> e.getRecipientType().equalsIgnoreCase("Tenant"))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (looDocument.getIsTenantsCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = looDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Tenant")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = looDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Tenant")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								looDocument.setIsTenantsCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
							} else {
								looDocument.setIsTenantsCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
							}
						}
					}
					looDocument.setStatus(rootNode.path("status").asText());
					if (rootNode.path("status").asText().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))) {
//							List<DocumentVersionDocument> documentLatestVersion = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
//							DocumentVersionDocument latestDocumentVersion = documentLatestVersion.stream()
//									.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion))
//									.orElseThrow(() -> new NoSuchElementException("No document version found"));
//							for(Document document:latestDocumentVersion.getDocuments())
//							{
//								CommonResponse documentResponse=clmServiceImpl.getEnvelopeDocument(envelopeId, document.getDocumentId());
//								String documentBase64 = mapper.readValue(
//										objectMapper.writeValueAsString(documentResponse.getResponse().getData()),String.class);
//								MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentBase64,
//										document.getName(), "text/plain");
//								uploadFilesIntoBlob(multipartFile, envelopeId,document.getDocumentId());
//	 						}
						looDocument.setCompleted(true);
					}
					if (lastModifiedDate != null) {
						looDocument.setLastModifiedDateTime(lastModifiedDate);
					}
					looContractDocumentRepository.save(looDocument);
					List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository
							.findByEnvelopeId(envelopeId);
					reviewerDocumentList.stream().forEach(reviewDoc -> {
						reviewDoc.setStatus(rootNode.path("status").asText());
						reviewerDocumentRepository.save(reviewDoc);
					});
					WorkFlowCreatorDocument workFlowDocument = workFlowCreatorDocumentRespository
							.findByEnvelopeId(envelopeId);
					workFlowDocument.setStatus(rootNode.path("status").asText());
					workFlowCreatorDocumentRespository.save(workFlowDocument);
				}
				break;
			case Constant.CVEF_DOCUMENT_REPOSITORY:
				CvefDocument cvefDocument = cvefDocumentRepository.findByEnvelopeId(envelopeId);
				if (cvefDocument != null) {
					cvefDocument.setStatus(rootNode.path("status").asText());
					if (lastModifiedDate != null) {
						cvefDocument.setLastModifiedDateTime(lastModifiedDate);
					}
					cvefDocumentRepository.save(cvefDocument);
					List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository
							.findByEnvelopeId(envelopeId);
					reviewerDocumentList.stream().forEach(reviewDoc -> {
						reviewDoc.setStatus(rootNode.path("status").asText());
						reviewerDocumentRepository.save(reviewDoc);
					});
					WorkFlowCreatorDocument workFlowDocument = workFlowCreatorDocumentRespository
							.findByEnvelopeId(envelopeId);
					workFlowDocument.setStatus(rootNode.path("status").asText());
					workFlowCreatorDocumentRespository.save(workFlowDocument);
				}
				break;
			case Constant.TA_DOCUMENT_REPOSITORY:
				TADocument taDocument = documentRepository.findByEnvelopeId(envelopeId);

				if (taDocument != null) {
					JsonNode eventDataNode = mapper.valueToTree(eventLogs.getData());
					JsonNode signersNode = eventDataNode.path("envelopeSummary").path("recipients").path("signers");
					JsonNode watchersNode = eventDataNode.path("envelopeSummary").path("recipients")
							.path("carbonCopies");
					if (!watchersNode.isMissingNode() && watchersNode.isArray()) {
						for (JsonNode watcherNode : watchersNode) {
							Signer signer = new Signer();
							signer.setEmail(watcherNode.path("email").asText());
							signer.setName(watcherNode.path("name").asText());
							signer.setRoutingOrder(watcherNode.path("routingOrder").asText());
							signer.setStatus(watcherNode.path("status").asText());
							watchersList.add(signer);
							uniqueMail.add(watcherNode.path(Constant.EMAIL).asText());
						}
					}
					log.info("WatchersList size : {}",watchersList.size());
					log.info("MailList size : {}",uniqueMail.size());
					if (!signersNode.isMissingNode() && signersNode.isArray()) {
						for (JsonNode signerNode : signersNode) {
							Signer signer = new Signer();
							signer.setEmail(signerNode.path("email").asText());
							signer.setName(signerNode.path("name").asText());
							signer.setRoutingOrder(signerNode.path("routingOrder").asText());
							signer.setStatus(signerNode.path("status").asText());
							signersList.add(signer);

						}
					}
					for (Signer signer : signersList) {
						String signerStatus = signer.getStatus();
						if (signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2))
								|| signerStatus.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))) {
							taDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(signer.getStatus()));
						} else {
							taDocument.getAllSigners().stream()
									.filter(e -> e.getEmail().equalsIgnoreCase(signer.getEmail())
											&& e.getRoutingOrder().equalsIgnoreCase(signer.getRoutingOrder()))
									.findFirst().ifPresent(e -> e.setSigningStatus(Constant.IN_PROGRESS));
						}

					}
					if (taDocument.getIsSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = taDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().equalsIgnoreCase("Signer")
										&& e.getSigningStatus().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners a = taDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().equalsIgnoreCase("Signer") && te
											.getSigningStatus().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findAny().orElse(null);
							if (a == null) {
								taDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								taDocument.setIsCvefSignersCompleted(Constant.IN_PROGRESS);
							} else {
								taDocument.setIsSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.setIsCvefSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.getAllSigners().stream()
										.filter(e -> e.getRecipientType().equalsIgnoreCase("CVEF_Signer"))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (taDocument.getIsCvefSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = taDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().equalsIgnoreCase("CVEF_Signer")
										&& e.getSigningStatus().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = taDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().equalsIgnoreCase("CVEF_Signer") && te
											.getSigningStatus().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								taDocument.setIsCvefSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								taDocument.setIsStampersCompleted(Constant.IN_PROGRESS);
							} else {
								taDocument.setIsCvefSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.setIsStampersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.getAllSigners().stream()
										.filter(e -> e.getRecipientType().equalsIgnoreCase("Stamper"))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (taDocument.getIsStampersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = taDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().equalsIgnoreCase("Stamper")
										&& e.getSigningStatus().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = taDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().equalsIgnoreCase("Stamper") && te
											.getSigningStatus().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								taDocument.setIsStampersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								taDocument.setIsTenantsCompleted(Constant.IN_PROGRESS);
							} else {
								taDocument.setIsStampersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.setIsTenantsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.getAllSigners().stream().filter(
										e -> (e.getRecipientType().equalsIgnoreCase("Tenant") || e.getRecipientType()
												.replaceAll("[_]", "").trim().equalsIgnoreCase("LHDNStamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (taDocument.getIsTenantsCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = taDocument.getAllSigners().stream()
								.filter(e -> e.getRecipientType().trim().equalsIgnoreCase("Tenant")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = taDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().trim().equalsIgnoreCase("Tenant")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								taDocument.setIsTenantsCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
								taDocument.setIsLhdnSignersCompleted(Constant.IN_PROGRESS);
							} else {
								taDocument.setIsTenantsCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
								taDocument.getAllSigners().stream()
										.filter(e -> (e.getRecipientType().replaceAll("[_]", "")
												.equalsIgnoreCase("LHDNStamper")))
										.forEach(e -> e.setSigningStatus(Constant.CLM_ENVELOPE_STATUS.get(2)));
							}
						}
					} else if (taDocument.getIsLhdnSignersCompleted().equalsIgnoreCase(Constant.IN_PROGRESS)) {
						AllSigners notCompleted = taDocument.getAllSigners().stream().filter(
								e -> e.getRecipientType().replaceAll("[_]", "").trim().equalsIgnoreCase("LHDNStamper")
										&& e.getSigningStatus().trim().equalsIgnoreCase(Constant.IN_PROGRESS))
								.findFirst().orElse(null);
						if (notCompleted == null) {
							AllSigners declinedEnvelope = taDocument.getAllSigners().stream()
									.filter(te -> te.getRecipientType().replaceAll("[_]", "").trim()
											.equalsIgnoreCase("LHDNStamper")
											&& te.getSigningStatus().trim()
													.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(2)))
									.findFirst().orElse(null);
							if (declinedEnvelope == null) {
								taDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(0));
							} else {
								taDocument.setIsLhdnSignersCompleted(Constant.CLM_ENVELOPE_STATUS.get(2));
							}
						}
					}
					if (taDocument.getIsSignersCompleted().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))
							&& taDocument.getIsStampersCompleted().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))
							&& taDocument.getIsCvefSignersCompleted().trim()
									.equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0))&&!taDocument.isWatcherEmailStatus()&&!watchersList.isEmpty()) {
							for (Signer watcher : watchersList) {
								if (uniqueMail.contains(watcher.getEmail())) {
									try {
										log.info("watcher :{}",watcher.getEmail());
										mailSenderService.sendMailToWatchers(watcher.getName(),
												taDocument.getContractTitle(), watcher.getEmail(), envelopeId,
												"commercial", Constant.TA_MODULE);
										uniqueMail.remove(watcher.getEmail());
										taDocument.setWatcherEmailStatus(true);
										} catch (IOException | TemplateException | MessagingException e1) {
										e1.printStackTrace();
									}
								}
							}
					}

					taDocument.setStatus(rootNode.path("status").asText());
//					if (rootNode.path("status").asText().equalsIgnoreCase("completed")) {
////						List<DocumentVersionDocument> documentLatestVersion = documentVersionDocumentRepository.findByEnvelopeId(envelopeId);
////						DocumentVersionDocument latestDocumentVersion = documentLatestVersion.stream()
////								.max(Comparator.comparingInt(DocumentVersionDocument::getDocVersion))
////								.orElseThrow(() -> new NoSuchElementException("No document version found"));
////						for(Document document:latestDocumentVersion.getDocuments())
////						{
////							CommonResponse documentResponse=clmServiceImpl.getEnvelopeDocument(envelopeId, document.getDocumentId());
////							String documentBase64 = mapper.readValue(
////									objectMapper.writeValueAsString(documentResponse.getResponse().getData()),String.class);
////							MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(documentBase64,
////									document.getName(), "text/plain");
////							uploadFilesIntoBlob(multipartFile, envelopeId,document.getDocumentId());
//// 						}
//					}
					if (lastModifiedDate != null) {
						taDocument.setLastModifiedDateTime(lastModifiedDate);
					}
					documentRepository.save(taDocument);
					List<ReviewerDocument> reviewerDocumentList = reviewerDocumentRepository
							.findByEnvelopeId(envelopeId);
					reviewerDocumentList.stream().forEach(reviewDoc -> {
						reviewDoc.setStatus(rootNode.path("status").asText());
						reviewerDocumentRepository.save(reviewDoc);
					});
					System.out.println("ReviewerList " + reviewerDocumentList.size());
					WorkFlowCreatorDocument workFlowDocument = workFlowCreatorDocumentRespository
							.findByEnvelopeId(envelopeId);
					workFlowDocument.setStatus(rootNode.path("status").asText());
					workFlowCreatorDocumentRespository.save(workFlowDocument);
				}
				break;
			}

			EnvelopeDocument envelopeDocument = new EnvelopeDocument();
			if (existingEnvelopeDocument == null) {
				envelopeDocument.setId(sequenceGeneratorService.generateSequence(EnvelopeDocument.SEQUENCE_NAME));
				envelopeDocument.setEnvelopeId(eventLogs.getData().getEnvelopeId());
				envelopeDocument.setCreatedOn(new Date());
				envelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
//				List<DocumentResponse> documentResponses = new ArrayList<>();
//				for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
//					DocumentResponse docResponse = new DocumentResponse();
//					docResponse.setDocumentId(documentResponse.getDocumentId());
//					docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
//					docResponse.setName(documentResponse.getName());
//					docResponse.setDocumentBase64(documentResponse.getDocumentBase64());
//					documentResponses.add(documentResponse);
//				}
//				envelopeDocument.setDocuments(documentResponses);
				envelopeDocument.setStartDate(new Date());
				envelopeRepository.save(envelopeDocument);
			} else {
				existingEnvelopeDocument.setUpdatedOn(new Date());
				existingEnvelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
//				List<DocumentResponse> documentResponses = new ArrayList<>();
//				for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
//					DocumentResponse docResponse = new DocumentResponse();
//					docResponse.setDocumentId(documentResponse.getDocumentId());
//					docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
//					docResponse.setName(documentResponse.getName());
//					docResponse.setDocumentBase64(documentResponse.getDocumentBase64());
//					documentResponses.add(documentResponse);
//				}
//				existingEnvelopeDocument.setDocuments(documentResponses);
				envelopeRepository.save(existingEnvelopeDocument);
			}
		}
		return null;
	}

	@Override
	public CommonResponse getAuditEventsFromDocusign() throws JsonProcessingException {
		AuditEventDocument auditEventDocument = new AuditEventDocument();
		List<EnvelopeDocument> envelopeDocuments = envelopeRepository.findAll();
		ResponseEntity<AuditEvent> response = null;
		for (EnvelopeDocument document : envelopeDocuments) {
			AuditEventDocument eventDocument = auditEventRepository.findByenvelopeId(document.getEnvelopeId());
			String url = getDousignUrl().getAuditLogs().replace("{docusignHost}", docusignHost.trim())
					+ document.getEnvelopeId();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			try {
				response = restTemplate.getForEntity(url, AuditEvent.class);
				log.info("{}", response.getBody().getAuditEvents().size());
				if (eventDocument == null) {
					if (response.getBody() != null) {
						auditEventDocument.setAuditEvents(response.getBody().getAuditEvents());
						auditEventDocument
								.setId(sequenceGeneratorService.generateSequence(AuditEventDocument.SEQUENCE_NAME));
						auditEventDocument.setCreatedOn(new Date());
						auditEventDocument.setStartDate(new Date());
						auditEventDocument.setEnvelopeId(document.getEnvelopeId());
						auditEventRepository.save(auditEventDocument);
					}
				} else {
					if (response.getBody() != null) {
						eventDocument.setAuditEvents(response.getBody().getAuditEvents());
						eventDocument.setUpdatedOn(new Date());
						eventDocument.setEnvelopeId(document.getEnvelopeId());
						auditEventRepository.save(eventDocument);
					}
				}
			} catch (HttpClientErrorException.BadRequest ex) {
				String responseBody = ex.getResponseBodyAsString();
				Object errorResponse = mapper.readValue(responseBody, AuditEvent.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("CLM Contract Response", errorResponse),
						"Contract Creation Failed");
			}
			AuditEventDocument auditDocCheckStatus = auditEventRepository.findByenvelopeId(document.getEnvelopeId());
			if (auditDocCheckStatus != null) {
				boolean isCompleted = auditDocCheckStatus.getAuditEvents().stream()
						.flatMap(event -> event.getEventFields().stream())
						.anyMatch(field -> field.getName().equalsIgnoreCase("EnvelopeStatus")
								&& field.getValue().equalsIgnoreCase(Constant.CLM_ENVELOPE_STATUS.get(0)));
				if (isCompleted) {
					checkEnvelopeStatus(document.getEnvelopeId());
				}
			}
		}
		return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("CLM Contract Response", response),
				"Contract Creation Failed");
	}

	private String checkEnvelopeStatus(String envelopeId) {
		EnvelopeDocument existingEnvelopeDocument = envelopeRepository.findByenvelopeId(envelopeId);
		String getEnvelopeById = getDousignUrl().getGetEnvelopeById().replace("{docusignHost}", docusignHost.trim())
				+ envelopeId;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<?> httpEntity = new HttpEntity<>(headers);
		ResponseEntity<EnvelopeResponse> envelopeDataResponse = null;
		try {
			envelopeDataResponse = restTemplate.exchange(getEnvelopeById, HttpMethod.GET, httpEntity,
					EnvelopeResponse.class);
		} catch (HttpClientErrorException.BadRequest ex) {
			// String responseBody = ex.getResponseBodyAsString();
			return "Unable to save updated envelope from event";
		}
		existingEnvelopeDocument.setUpdatedOn(new Date());
		existingEnvelopeDocument.setEnvelope(envelopeDataResponse.getBody().getEnvelope());
		List<DocumentResponse> documentResponses = new ArrayList<>();
		for (DocumentResponse documentResponse : envelopeDataResponse.getBody().getDocuments()) {
			DocumentResponse docResponse = new DocumentResponse();
			docResponse.setDocumentId(documentResponse.getDocumentId());
			docResponse.setDocumentIdGuid(documentResponse.getDocumentIdGuid());
			docResponse.setName(documentResponse.getName());
//			docResponse.setDocumentBase64(documentResponse.getDocumentBase64());
			documentResponses.add(documentResponse);
		}
		existingEnvelopeDocument.setDocuments(documentResponses);
		envelopeRepository.save(existingEnvelopeDocument);
		return null;
	}

	@Data
	public static class AuditEventsListResponse {
		private String envelopeId;
		private List<AuditEventsResponse> eventFields;
	}

	@Override
	public CommonResponse getAuditEvents(String envelopeId) {
		AuditEventDocument auditEventDocument = auditEventRepository.findByenvelopeId(envelopeId);
		List<EventFieldsList> fieldsLists = auditEventDocument.getAuditEvents();
		AuditEventsListResponse auditEventsListResponse = new AuditEventsListResponse();
		List<AuditEventsResponse> auditEventsResponsesList = new ArrayList<>();
		for (EventFieldsList eventField : fieldsLists) {
			for (int i = 0; i < eventField.getEventFields().size(); i += 12) {
				AuditEventsResponse auditEventsResponse = new AuditEventsResponse();
				auditEventsResponse.setLogTime(eventField.getEventFields().get(i).getValue());
				auditEventsResponse.setSource(eventField.getEventFields().get(i + 1).getValue());
				auditEventsResponse.setUserName(eventField.getEventFields().get(i + 2).getValue());
				auditEventsResponse.setUserId(eventField.getEventFields().get(i + 3).getValue());
				auditEventsResponse.setAction(eventField.getEventFields().get(i + 4).getValue());
				auditEventsResponse.setMessage(eventField.getEventFields().get(i + 5).getValue());
				auditEventsResponse.setEnvelopeStatus(eventField.getEventFields().get(i + 6).getValue());
				auditEventsResponse.setClientIPAddress(eventField.getEventFields().get(i + 7).getValue());
				auditEventsResponse.setInformation(eventField.getEventFields().get(i + 8).getValue());
				auditEventsResponse.setInformationLocalized(eventField.getEventFields().get(i + 9).getValue());
				auditEventsResponse.setGeoLocation(eventField.getEventFields().get(i + 10).getValue());
				auditEventsResponse.setLanguage(eventField.getEventFields().get(i + 11).getValue());
				auditEventsResponsesList.add(auditEventsResponse);
			}
		}
		auditEventsListResponse.setEnvelopeId(envelopeId);
		auditEventsListResponse.setEventFields(auditEventsResponsesList);
		return new CommonResponse(HttpStatus.OK, new Response("AuditEventResponse", auditEventsListResponse),
				"Details Retrieved Successfully");
	}

//	private void uploadFilesIntoBlob(MultipartFile multipartFiles, String envelopeId, String documentId)
//			throws URISyntaxException, StorageException, IOException {
//		String path = envelopeId;
//		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
//		CloudBlockBlob blob = container.getBlockBlobReference(containerName + path +"/completed/" + documentId);
//		blob.getProperties().setContentType("application/pdf");
//		try (InputStream inputStream = multipartFiles.getInputStream()) {
//			blob.upload(inputStream, multipartFiles.getSize());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}
