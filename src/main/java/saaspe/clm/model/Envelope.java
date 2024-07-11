package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import saaspe.clm.document.EnvelopeDocument;

@Data
public class Envelope {

	@JsonProperty("accessControlListBase64")
	private String accessControlListBase64 = null;

	@JsonProperty("allowComments")
	private String allowComments = null;

	@JsonProperty("allowMarkup")
	private String allowMarkup = null;

	@JsonProperty("allowReassign")
	private String allowReassign = null;

	@JsonProperty("allowViewHistory")
	private String allowViewHistory = null;

	@JsonProperty("anySigner")
	private String anySigner = null;

	@JsonProperty("asynchronous")
	private String asynchronous = null;

	@JsonProperty("attachmentsUri")
	private String attachmentsUri = null;

	@JsonProperty("authoritativeCopy")
	private String authoritativeCopy = null;

	@JsonProperty("authoritativeCopyDefault")
	private String authoritativeCopyDefault = null;

	@JsonProperty("autoNavigation")
	private String autoNavigation = null;

	@JsonProperty("brandId")
	private String brandId = null;

	@JsonProperty("brandLock")
	private String brandLock = null;

	@JsonProperty("certificateUri")
	private String certificateUri = null;

	@JsonProperty("completedDateTime")
	private String completedDateTime = null;

	@JsonProperty("copyRecipientData")
	private String copyRecipientData = null;

	@JsonProperty("createdDateTime")
	private String createdDateTime = null;

	@JsonProperty("customFields")
	private CustomFields customFields = null;

	@JsonProperty("customFieldsUri")
	private String customFieldsUri = null;

	@JsonProperty("declinedDateTime")
	private String declinedDateTime = null;

	@JsonProperty("deletedDateTime")
	private String deletedDateTime = null;

	@JsonProperty("deliveredDateTime")
	private String deliveredDateTime = null;

	@JsonProperty("disableResponsiveDocument")
	private String disableResponsiveDocument = null;

	@JsonProperty("documentBase64")
	private String documentBase64 = null;

	@JsonProperty("documentsCombinedUri")
	private String documentsCombinedUri = null;

	@JsonProperty("documentsUri")
	private String documentsUri = null;

	@JsonProperty("emailBlurb")
	private String emailBlurb = null;

	@JsonProperty("emailSettings")
	private EmailSettings emailSettings = null;

	@JsonProperty("emailSubject")
	private String emailSubject = null;

	@JsonProperty("enableWetSign")
	private String enableWetSign = null;

	@JsonProperty("enforceSignerVisibility")
	private String enforceSignerVisibility = null;

	@JsonProperty("envelopeAttachments")
	private java.util.List<Attachment> envelopeAttachments = null;

	@JsonProperty("envelopeDocuments")
	private java.util.List<EnvelopeDocument> envelopeDocuments = null;

	@JsonProperty("envelopeId")
	private String envelopeId = null;

	@JsonProperty("envelopeIdStamping")
	private String envelopeIdStamping = null;

	@JsonProperty("envelopeLocation")
	private String envelopeLocation = null;

	@JsonProperty("envelopeMetadata")
	private EnvelopeMetadata envelopeMetadata = null;

	@JsonProperty("envelopeUri")
	private String envelopeUri = null;

	@JsonProperty("expireAfter")
	private String expireAfter = null;

	@JsonProperty("expireDateTime")
	private String expireDateTime = null;

	@JsonProperty("expireEnabled")
	private String expireEnabled = null;

	@JsonProperty("externalEnvelopeId")
	private String externalEnvelopeId = null;

	@JsonProperty("folders")
	private java.util.List<Folder> folders = null;

	@JsonProperty("hasComments")
	private String hasComments = null;

	@JsonProperty("hasFormDataChanged")
	private String hasFormDataChanged = null;

	@JsonProperty("hasWavFile")
	private String hasWavFile = null;

	@JsonProperty("holder")
	private String holder = null;

	@JsonProperty("initialSentDateTime")
	private String initialSentDateTime = null;

	@JsonProperty("is21CFRPart11")
	private String is21CFRPart11 = null;

	@JsonProperty("isDynamicEnvelope")
	private String isDynamicEnvelope = null;

	@JsonProperty("isSignatureProviderEnvelope")
	private String isSignatureProviderEnvelope = null;

	@JsonProperty("lastModifiedDateTime")
	private String lastModifiedDateTime = null;

	@JsonProperty("location")
	private String location = null;

	@JsonProperty("lockInformation")
	private LockInformation lockInformation = null;

	@JsonProperty("messageLock")
	private String messageLock = null;

	@JsonProperty("notification")
	private Notification notification = null;

	@JsonProperty("notificationUri")
	private String notificationUri = null;

	@JsonProperty("powerForm")
	private PowerForm powerForm = null;

	@JsonProperty("purgeCompletedDate")
	private String purgeCompletedDate = null;

	@JsonProperty("purgeRequestDate")
	private String purgeRequestDate = null;

	@JsonProperty("purgeState")
	private String purgeState = null;

	@JsonProperty("recipients")
	private Recipients recipients = null;

	@JsonProperty("recipientsLock")
	private String recipientsLock = null;

	@JsonProperty("recipientsUri")
	private String recipientsUri = null;

	@JsonProperty("sender")
	private UserInfo sender = null;

	@JsonProperty("sentDateTime")
	private String sentDateTime = null;

	@JsonProperty("signerCanSignOnMobile")
	private String signerCanSignOnMobile = null;

	@JsonProperty("signingLocation")
	private String signingLocation = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("statusChangedDateTime")
	private String statusChangedDateTime = null;

	@JsonProperty("statusDateTime")
	private String statusDateTime = null;

	@JsonProperty("templatesUri")
	private String templatesUri = null;

	@JsonProperty("transactionId")
	private String transactionId = null;

	@JsonProperty("useDisclosure")
	private String useDisclosure = null;

	@JsonProperty("voidedDateTime")
	private String voidedDateTime = null;

	@JsonProperty("voidedReason")
	private String voidedReason = null;

	@JsonProperty("workflow")
	private Workflow workflow = null;

}
