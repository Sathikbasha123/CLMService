package saaspe.clm.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import saaspe.clm.document.CvefDocument;

public interface CvefDocumentRepository extends MongoRepository<CvefDocument, Long> {

	@Query(value = "{'isFlowCompleted': ?0}", fields = "{ 'contractTitle' : 1, 'envelopeId' : 1, '_id' : 0 }")
	public List<CvefDocument> findApprovedCVEFContractList(boolean b);

	CvefDocument findByEnvelopeId(String envId);
}
