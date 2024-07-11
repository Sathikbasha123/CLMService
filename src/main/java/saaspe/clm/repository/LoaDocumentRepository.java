package saaspe.clm.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.LoaDocument;

public interface LoaDocumentRepository extends MongoRepository<LoaDocument, Long> {

	@Query("{ $and: [ {'?0': { $regex: ?1, $options: 'i' }},{'senderEmail':?2} ]}")
	List<LoaDocument> findByCollatedField(String fieldname, String regex, String email, Pageable page,
			Collation collation);

	@Query("{ $and: [ {'?0': { $regex: ?1, $options: 'i' }},{'senderEmail':?2}, {'status': ?3} ] }")
	List<LoaDocument> findByCollatedFieldByStatus(String fieldName, String regex, String email, String status,
			Pageable page, Collation collation);

	@Query("{ $and: [ {'status':?0},{'senderEmail':?1} ]}")
	List<LoaDocument> findAllLoaDocumentsByStatus(Pageable pageablee, String status, String email);

	@Query("{'senderEmail':?0}")
	List<LoaDocument> findAllLoaDocuments(String email, Pageable pageable);

	@Query(value = "{ $and: [ { ?0: { $regex: ?1, $options: 'i' } }, { 'senderEmail': ?2 }, { 'status': ?3 } ] }", count = true)
	long countByCollatedFieldByStatus(String fieldName, String regex, String email, String status, Collation collation);

	@Query(value = "{ $and: [ { ?0: { $regex: ?1, $options: 'i' } }, { 'senderEmail': ?2 } ] }", count = true)
	long countByCollatedField(String fieldname, String regex, String email, Collation collation);

	long countByStatusAndSenderEmail(String status, String email);

	long countBySenderEmail(String email);

	LoaDocument findByEnvelopeId(String envId);

	List<LoaDocument> findByUniqueString(String uniqueString);

	@Query("{'envelopeId':?0}")
	List<LoaDocument> findByEnvelopeIds(String envelopeId);

	@Query(value = "{$and: [ {'isFlowCompleted': ?0},{'isCompleted': ?1},{ 'subsidiary': { $regex: ?2, $options: 'i' } } ] }", fields = "{ 'projectName' : 1, 'projectId' : 1, 'envelopeId' : 1, '_id' : 0,'subsidiary': 1, 'contractNumber': 1 }",sort = "{ 'createdOn' : -1 }")
	List<LoaDocument> findApprovedLoaList(boolean isFlowCompleted,boolean isCompleted, String subsidiary);

	@Query(value = "{$and: [ {'isFlowCompleted': ?0},{'isCompleted': ?1} ] }")
	List<LoaDocument> findApprovedAllLoaList(boolean isFlowCompleted,boolean isCompleted, Pageable pageable);

	@Query(value = "{'senderEmail': ?0}", sort = "{'createdOn': -1}")
	List<LoaDocument> findBySenderEmail(String email);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<LoaDocument> findByEnvelopeIdIn(List<String> envelopeId);

	@Query(value = "{$and: [ {'isFlowCompleted': ?0},{'isCompleted': ?1} ] }",count = true)
	long countAllLoaList(boolean isFlowCompleted,boolean isCompleted);

	@Query(value = "{ $and: [ {'?0': { $regex: ?1, $options: 'i' }},{'isFlowCompleted': ?2},{'isCompleted': ?3}]}")
	List<LoaDocument> findAllLoaListMatchingText(String field, String searchText, boolean isFlowCompleted,boolean isCompleted, Pageable pageable);

	@Query(value = "{ $and: [ {'?0': { $regex: ?1, $options: 'i' }},{'isFlowCompleted': ?2},{'isCompleted': ?3} ]}",count = true)
	long countAllLoaListMatchingText(String field, String searchText, boolean isFlowCompleted,boolean isCompleted);

	@Query(value = "{ $and: [ { $or: [ { 'envelopeId': { $regex: ?0, $options: 'i' } }, { 'projectId': { $regex: ?0, $options: 'i' } }, { 'projectName': { $regex: ?0, $options: 'i' } }, { 'senderName': { $regex: ?0, $options: 'i' } }, { 'createdOn': { $regex: ?0, $options: 'i' } }, { 'subsidiary': { $regex: ?0, $options: 'i' } } ] }, { 'isFlowCompleted': ?1 },{ 'isCompleted': ?2 },{'status': { $regex:?3,$options:'i' }},{'subsidiary': { $regex:?4,$options:'i' }} ] }")
	List<LoaDocument> findAllLoaListMatchingSearchText(String search, boolean isFlowCompleted,boolean isCompleted, String status,String subsidiary, Pageable pageable, Collation collation);

	@Query(value = "{ $and: [ { $or: [ { 'envelopeId': { $regex: ?0, $options: 'i' } }, { 'projectId': { $regex: ?0, $options: 'i' } }, { 'projectName': { $regex: ?0, $options: 'i' } }, { 'senderName': { $regex: ?0, $options: 'i' } }, { 'createdOn': { $regex: ?0, $options: 'i' } }, { 'subsidiary': { $regex: ?0, $options: 'i' } } ] }, { 'isFlowCompleted': ?1 },{ 'isCompleted': ?2 },{'status': { $regex:?3,$options:'i' }},{'subsidiary': { $regex:?4,$options:'i' }} ] }",count = true)
	long countAllLoaListMatchingSearchText(String search, boolean isFlowCompleted,boolean isCompleted, String status,String subsidiary);

	@Query("{$and :[{ 'envelopeId' : { $in: ?0 } },{'createdBy': ?1}]}")
	List<LoaDocument> findByEnvelopeIdInAndCreatedBy(List<String> envelopeId,String createdBy);

	List<LoaDocument> findByCreatedBy(String createdBy,Pageable pageable);

	@Query(value = "{'createdBy': ?0}",count = true)
	long findByCreatedByCount(String createdBy);

	@Query("{ $and :[ {'$or':[ {'projectName': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }},{'contractNumber': { $regex:?0,$options:'i' } }] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }")
	List<LoaDocument> findBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy,String status,String subsidiary, PageRequest pageable, Collation collation);

	@Query(value = "{ $and :[ {'$or':[ {'projectName': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }},{'contractNumber': { $regex:?0,$options:'i' } }] },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }",count = true)
	long countBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy,String status,String subsidiary);

	List<LoaDocument> findByCreatedOnBeforeAndStatus(Date start, String string);

}