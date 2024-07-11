package saaspe.clm.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import saaspe.clm.document.CentralRepoDocument;

public interface CentralRepoDocumentRepository extends MongoRepository<CentralRepoDocument,Long> {
        
	CentralRepoDocument findByEnvelopeId(String envelopeId);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<CentralRepoDocument> findByEnvelopeId(List<String> listOfEnvelopeIds);
}