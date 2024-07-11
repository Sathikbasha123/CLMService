package saaspe.clm.repository;

import java.util.Date;

import org.springframework.data.mongodb.repository.MongoRepository;

import saaspe.clm.document.EnvelopeLockDocument;

public interface EnvelopeLockDocumentRepository extends MongoRepository<EnvelopeLockDocument, Long> {

	EnvelopeLockDocument findByEnvelopeIdAndExpiryAtAfter(String envelopeId, Date date);

	
}
