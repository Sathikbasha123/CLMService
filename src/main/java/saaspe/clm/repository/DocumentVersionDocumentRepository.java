package saaspe.clm.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import saaspe.clm.document.DocumentVersionDocument;

public interface DocumentVersionDocumentRepository extends MongoRepository<DocumentVersionDocument, Long> {

	List<DocumentVersionDocument> findByEnvelopeId(String envelopeId);

//	@Query(value = "[{$match:{envelopeId: ?0}},{$sort:{docVersion: -1}},{$limit:1}]")
//	DocumentVersionDocument findLatestEnvelope(String envelopeId);
	
	DocumentVersionDocument findTopByEnvelopeIdOrderByDocVersionDesc(String envelopeId);

	@Query(value="{ $and: [ {'envelopeId':?0},{'docVersion':?1} ]}")
	DocumentVersionDocument findEnvelopeIdAndDocVersion(String envelopeId, int docVersion);
}
