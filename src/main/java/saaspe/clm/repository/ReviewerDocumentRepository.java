package saaspe.clm.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.ReviewerDocument;

public interface ReviewerDocumentRepository extends MongoRepository<ReviewerDocument, Long> {

	@Query(value = "{ '$match': { 'email': ?0 }, '$group': { '_id': '$envelopeId', 'maxDocVersion': { '$max': '$docVersion' } } }")
	List<ReviewerDocument> findLatestDocumentsByEnvelopeAndEmail(String email, Pageable pageable);

	List<ReviewerDocument> findByEnvelopeId(String envelopeId);

	List<ReviewerDocument> findByCreatedByIsNotAndEnvelopeId(String createdBy, String envelopeId);

	@Aggregation({ "{$match: {orderFlag: ?2, email: ?0, flowType: ?1, isCompleted: ?3}}" })
	List<ReviewerDocument> findLatestDocumentsByEnvelopeAndEmail(String email, String flowType, boolean orderFlag,
			Pageable pageable, boolean isCompleted);

	@Aggregation({ "{$match: {orderFlag: ?1, email: ?0, isCompleted: ?2,flowType:?3}}" })
	List<ReviewerDocument> findLatestDocumentsByEnvelopeAndEmail(String email, boolean orderFlag, boolean isCompleted,
			String flowType);
	
	@Aggregation({ "{$match: {orderFlag: ?1, email: ?0,flowType:?2}}" })
	List<ReviewerDocument> findLatestDocumentsByEnvelopeAndEmail(String email, boolean isCompleted,
			String flowType);

	@Query(value = "{isCompleted:?1, email :?0, flowType:?2}", count = true)
	long findReviewedDocumentCount(String email, boolean flag, String flowType);
	
	@Query(value = "{email: ?0, flowType: ?1}", count = true)
	long getTotalCount(String email, String flowType);

	@Query(value = "{isCompleted:?1, email :?0}", count = true)
	long findReviewedDocumentCount(String email, boolean flag);

	@Query(value = "{ 'email': ?0, 'isCompleted': ?1, 'flowType': ?2 }")
	List<ReviewerDocument> findReviewedDocument(String email, boolean isCompleted, String flowType,
			PageRequest pageable);

	@Query("{ email:?0,envelopeId:?1}")
	ReviewerDocument findByEmailAndEnvelopeId(String email, String envelopeId);

	@Aggregation({
	    "{$match: {orderFlag: ?2, email: ?0, flowType: ?1, isCompleted: ?3, $or: [{creatorName: {$regex: ?5, $options: 'i'}},{contractName: {$regex: ?5, $options: 'i'}}]}}"
	})	List<ReviewerDocument> findLatestDocumentByField(String email, String flowType, boolean orderFlag,
			Pageable pageable, boolean isCompleted, String orderBy, String searchText, Collation collation);

	@Aggregation({"{$match: {orderFlag: ?2, email: ?0, flowType: ?1, isCompleted: ?3, $or: [{creatorName: {$regex: ?5, $options: 'i'}},{contractName: {$regex: ?5, $options: 'i'}}]}}"})
	List<ReviewerDocument> findLatestDocumentCountByField(String email, String flowType, boolean orderFlag,
			boolean isCompleted, String orderBy, String searchText, Collation collation);

	@Query("{ $and :[ {'$or':[ {'creatorName': { $regex:?4,$options:'i' } },{'subsidiary': { $regex:?4,$options:'i' } }, {'contractName': { $regex:?4,$options:'i' } }] },{ 'email': ?0},{ 'isCompleted': ?1},{ 'flowType': ?2 }] } ")
	List<ReviewerDocument> findReviewedDocumentByField(String email, boolean isCompleted, String flowType,
			PageRequest pageable, String orderBy, String searchText, Collation collation);

	@Query(value = "{ $and :[ {'$or':[ {'creatorName': { $regex:?4,$options:'i' } }, {'contractName': { $regex:?4,$options:'i' } }] },{ 'email': ?0},{ 'isCompleted': ?2},{ 'flowType': ?1 }] } ", count = true)
	long findReviewedDocumentCountByField(String email, String flowType, boolean b, String orderBy, String searchText,
			Collation collation);

	List<ReviewerDocument> findByEnvelopeIdIn(List<String> envelopeId);

	@Query(value = "{$and: [{$or: [{projectId: {$regex: ?0, $options: 'i'}},{contractName: {$regex: ?0, $options: 'i'}},{'contractNumber': { $regex:?0,$options:'i' } }]},{'status': { $regex:?5,$options:'i' }},{'subsidiary': { $regex:?6,$options:'i' }},{email: ?1},{flowType: ?2},{orderFlag: ?3},{isCompleted: ?4}]}")
	List<ReviewerDocument> findByEmailAndFlowTypeAndSearchText(String searchText, String email, String flowType, boolean orderFlag, boolean isCompleted,String status,String subsidiary,Pageable pageable, Collation collation);

	@Query(value = "{$and: [{$or: [{projectId: {$regex: ?0, $options: 'i'}},{contractName: {$regex: ?0, $options: 'i'}},{'contractNumber': { $regex:?0,$options:'i' } }]},{'status': { $regex:?5,$options:'i' }},{'subsidiary': { $regex:?6,$options:'i' }},{email: ?1},{flowType: ?2},{orderFlag: ?3},{isCompleted: ?4}]}",count = true)
	long countByEmailAndFlowTypeAndSearchText(String searchText, String email, String flowType, boolean orderFlag, boolean isCompleted,String status,String subsidiary, Collation collation);

	@Query(value = "{$and: [{$or: [{projectId: {$regex: ?0, $options: 'i'}},{contractName: {$regex: ?0, $options: 'i'}},{'contractNumber': { $regex:?0,$options:'i' } }]},{'status': { $regex:?4,$options:'i' }},{'subsidiary': { $regex:?5,$options:'i' }},{email: ?1},{flowType: ?2},{isCompleted: ?3}]}")
	List<ReviewerDocument> findByEmailAndFlowTypeAndSearchTextAndCompleted(String searchText, String email, String flowType, boolean isCompleted,String status,String subsidiary,Pageable pageable, Collation collation);

	@Query(value = "{$and: [{$or: [{projectId: {$regex: ?0, $options: 'i'}},{contractName: {$regex: ?0, $options: 'i'}},{'contractNumber': { $regex:?0,$options:'i' } }]},{'status': { $regex:?4,$options:'i' }},{'subsidiary': { $regex:?5,$options:'i' }},{email: ?1},{flowType: ?2},{isCompleted: ?3}]}",count = true)
	long countByEmailAndFlowTypeAndSearchTextAndCompleted(String searchText, String email, String flowType, boolean isCompleted,String status,String subsidiary, Collation collation);

}