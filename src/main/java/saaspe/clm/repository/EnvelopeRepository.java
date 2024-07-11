package saaspe.clm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import saaspe.clm.document.EnvelopeDocument;

import java.util.List;

@Repository
public interface EnvelopeRepository extends MongoRepository<EnvelopeDocument, Long> {

	@Query("{'envelopeId' : :#{#envelopeId}}")
	EnvelopeDocument findByenvelopeId(String envelopeId);

	EnvelopeDocument findByEnvelopeId(String envelopeId);

	@Query("{'envelope.status' : :#{#status}}")
	Page<EnvelopeDocument> findByCustomStatusQuery(String status, Pageable pageable);

	@Query("{ 'envelope.sender.email' : ?0, 'envelope.status' : ?1 }")
	Page<EnvelopeDocument> findByEnvelopeIdAndStatus(String email, String status, Pageable pageable);

	List<EnvelopeDocument> findByEnvelopeIdIn(List<String> envelopeIds);
}
