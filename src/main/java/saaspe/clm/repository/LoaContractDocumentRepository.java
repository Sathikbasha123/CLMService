package saaspe.clm.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import saaspe.clm.document.LoaContractDocument;
import saaspe.clm.document.LoaDocument;

public interface LoaContractDocumentRepository extends MongoRepository<LoaContractDocument, Long> {

	@Query("{'envelopeId' : :#{#envId}}")
	LoaContractDocument findByEnvelopeId(String envId);

	@Query("{'isFlowCompleted':?0}")
	List<LoaContractDocument> findByIsFlowCompleted(Pageable pageable, Boolean isFlowCompleted);

	@Query(value = "{'isFlowCompleted':?0}", count = true)
	long findByFlowCompleted(Boolean isFlowCompleted);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<LoaContractDocument> findByEnvelopeIdIn(List<String> listOfEnvelopeIds);

	@Query(value = "{'createdBy': ?0}",count = true)
	long countByCreatedBy(String createdBy);

	@Query(value = "{'createdBy': ?0}",count = true)
	List<LoaContractDocument> findByCreatedBy(String createdBy,Pageable pageable);

	@Query(value = "{ $and :[ {'$or':[ {'projectName': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }",count = true)
	long countBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy,String status,String subsidiary);

	@Query("{ $and :[ {'$or':[ {'projectName': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }")
	List<LoaContractDocument> findBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy, String status, String subsidiary, PageRequest pageable, Collation collation);

	List<LoaContractDocument> findByCreatedOnBeforeAndStatus(Date endDate, String string);


}
