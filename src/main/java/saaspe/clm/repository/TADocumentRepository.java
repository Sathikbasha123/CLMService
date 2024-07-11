package saaspe.clm.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.TADocument;

import java.util.Date;
import java.util.List;

public interface TADocumentRepository extends MongoRepository<TADocument, Long> {
	
	TADocument findByEnvelopeId(String envId);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<TADocument> findByEnvelopeIdIn(List<String> listOfEnvelopeIds);

	@Query(value = "{'createdBy': ?0}",count = true)
	long countByCreatedBy(String createdBy);

	@Query(value = "{'createdBy': ?0}",count = true)
	List<TADocument> findByCreatedBy(String createdBy, Pageable pageable);

	@Query(value = "{ $and :[ {'$or':[ {'contractTitle': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }",count = true)
	long countBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy,String status,String subsidiary);

	@Query("{ $and :[ {'$or':[ {'contractTitle': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }")
	List<TADocument> findBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy, String status, String subsidiary, PageRequest pageable, Collation collation);

	List<TADocument> findByCreatedOnBeforeAndStatus(Date endDate, String string);

}