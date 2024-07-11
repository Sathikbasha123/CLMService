package saaspe.clm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import saaspe.clm.document.AuditEventDocument;

public interface AuditEventRepository extends MongoRepository<AuditEventDocument, Long>{

	@Query("{'envelopeId' : :#{#envelopeId}}")
	AuditEventDocument findByenvelopeId(String envelopeId);
}
