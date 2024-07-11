package saaspe.clm.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.CmuContractDocument;

public interface CmuContractDocumentRepository extends MongoRepository<CmuContractDocument, Long> {
	
	//internal
	@Query(value = "{'isFlowCompleted': ?0}", fields = "{ 'contractTitle' : 1, 'envelopeId' : 1, '_id' : 0 }",sort = "{ 'createdOn' : -1 }")
	List<CmuContractDocument> findApprovedCMUContractList(boolean b);

	@Query("{'envelopeId' : :#{#envId}}")
	CmuContractDocument findByEnvelopeId(String envId);

	@Query("{'envelopeId':?0}")
	List<CmuContractDocument> findByEnvelopeIds(String envelopeId);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<CmuContractDocument> findByEnvelopeIdIn(List<String> listOfEnvelopeIds);

	CmuContractDocument findByTemplateId(String templateId);

	@Query(value = "{$and: [ {'isFlowCompleted': ?0} ] }",count = true)
	long countAllByIsFlowCompleted(boolean isFlowCompleted);

	@Query(value = "{$and: [ {'isFlowCompleted': ?0} ] }")
	List<CmuContractDocument> findByIsFlowCompleted(boolean isFlowCompleted, Pageable pageable);

	@Query(value = "{ $and: [ { $or: [ { 'projectId': { $regex: ?0, $options: 'i' } }, { 'contractTitle': { $regex: ?0, $options: 'i' } }, { 'subsidiary': { $regex: ?0, $options: 'i' } }, { 'status': { $regex: ?0, $options: 'i' } } ] }, { 'isFlowCompleted': ?1 },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }",count = true)
	long countAllLoaListMatchingSearchText(String search, boolean isFlowCompleted, String status, String subsidiary);

	@Query(value = "{ $and: [ { $or: [ { 'projectId': { $regex: ?0, $options: 'i' } }, { 'contractTitle': { $regex: ?0, $options: 'i' } }, { 'subsidiary': { $regex: ?0, $options: 'i' } }, { 'status': { $regex: ?0, $options: 'i' } }] }, { 'isFlowCompleted': ?1 },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }")
	Page<CmuContractDocument> findAllLoaListMatchingSearchText(String search, boolean isFlowCompleted, String status, String subsidiary,  Pageable pageable);

	@Query(value = "{'createdBy': ?0}",count = true)
	long countByCreatedBy(String createdBy);

	@Query(value = "{'createdBy': ?0}",count = true)
	List<CmuContractDocument> findByCreatedBy(String createdBy,Pageable pageable);

	@Query(value = "{ $and :[ {'$or':[ {'contractTitle': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }",count = true)
	long countBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy,String status,String subsidiary);

	@Query("{ $and :[ {'$or':[{'contractTitle': { $regex:?0,$options:'i' } },{'projectId': { $regex:?0,$options:'i' }}] },{ 'createdBy' : ?1  },{'status': { $regex:?2,$options:'i' }},{'subsidiary': { $regex:?3,$options:'i' }}] }")
	List<CmuContractDocument> findBySearchTextAndCreatedByAndStatusAndSubsidiary(String searchText, String createdBy, String status, String subsidiary, PageRequest pageable, Collation collation);



}
