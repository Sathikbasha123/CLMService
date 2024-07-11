package saaspe.clm.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.LooContractDocument;

public interface LooContractDocumentRepository extends MongoRepository<LooContractDocument, Long> {

	@Query(value = "{$and: [ {'isFlowCompleted': ?0},{'isCompleted': ?1} ] }", fields = "{ 'contractTitle' : 1, 'envelopeId' : 1, '_id' : 0 }",sort = "{ 'createdOn' : -1 }")
	List<LooContractDocument> findApprovedLOOContractList(boolean b,boolean b1);
	
	LooContractDocument findByEnvelopeId(String envId);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<LooContractDocument> findByEnvelopeIdIn(List<String> listOfEnvelopeIds);

	@Query(value = "{'createdBy': ?0}",count = true)
	long countByCreatedBy(String createdBy);

	@Query(value = "{'createdBy': ?0}",count = true)
	List<LooContractDocument> findByCreatedBy(String createdBy, Pageable pageable);

	@Query(value = "{ $and :[ {'$or':[ {'contractTitle': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }",count = true)
	long countBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy,String status,String subsidiary);

	@Query("{ $and :[ {'$or':[ {'contractTitle': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }")
	List<LooContractDocument> findBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy, String status, String subsidiary, PageRequest pageable, Collation collation);

	List<LooContractDocument> findByCreatedOnBeforeAndStatus(Date endDate, String string);

}
