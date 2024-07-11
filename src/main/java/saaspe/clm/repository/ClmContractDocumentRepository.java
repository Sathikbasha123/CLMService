package saaspe.clm.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.ClmContractDocument;

public interface ClmContractDocumentRepository extends MongoRepository<ClmContractDocument, Long> {

	@Query("{'envelopeId' : :#{#envelopeId}}")
	ClmContractDocument findByEnvelopeId(String envelopeId);

	List<ClmContractDocument> findByUniqueString(String uniqueString);

	List<ClmContractDocument> findTop10BySenderEmailOrderByCreatedOnDesc(String senderEmail);

	@Query("{'senderEmail': ?0, $or: [ {'status': {$ne: 'voided'}},{'newEnvelopeId': {$exists: false}},{$and: [ {'status': 'voided'}, {'newEnvelopeId': {$exists: true, $eq: null}}]}]}")
	List<ClmContractDocument> findTop10BySenderEmailOrderByCreatedOnDesc(String senderEmail, Sort sort);
	
	@Query("{'contractEndDate' : { $lt: ?0 }}")
	List<ClmContractDocument> findTop10ContractsBeforeTodayOrderByContractEndDateDesc(Date today);

	@Query("{'contractEndDate': { $lt: ?0 }, 'senderEmail': ?1, $or: [ {'status': {$ne: 'voided'}}, {'newEnvelopeId': {$exists: false}}, {$and: [ {'status': 'voided'}, {'newEnvelopeId': {$exists: true, $eq: null}}]}]}")
	List<ClmContractDocument> findTop10ByContractEndDateLessThanAndSenderEmailOrderByContractEndDateDesc(Date today, String senderEmail);


	List<ClmContractDocument> findAllBySenderEmail(String email);

	Page<ClmContractDocument> findBySenderEmail(String senderEmail, Pageable pageable);


	@Query("{ $and: [ {'?0': { $regex: ?1, $options: 'i' }},{'senderEmail':?2} ]}")
	List<ClmContractDocument> findByCollatedField(String fieldname, String regex,String email, Pageable page, Collation collation);

	@Query("{ $and: [ {'?0': { $regex: ?1, $options: 'i' }},{'senderEmail':?2}, {'status': ?3} ] }")
	List<ClmContractDocument> findByCollatedFieldByStatus(String fieldName, String regex,String email, String status, Pageable page,
			Collation collation);

	@Query("{ $and: [ {'status':?0},{'senderEmail':?1} ]}")
	List<ClmContractDocument> findAllContractsByStatus(Pageable pageablee, String status,String email);

	
	@Query("{'senderEmail':?0}")
	List<ClmContractDocument> findAllContracts(String email,Pageable pageable);
	
	@Query(value = "{ $and: [ { ?0: { $regex: ?1, $options: 'i' } }, { 'senderEmail': ?2 }, { 'status': ?3 } ] }", count = true)
	long countByCollatedFieldByStatus(String fieldName, String regex, String email, String status,Collation collation);
	
	@Query(value = "{ $and: [ { ?0: { $regex: ?1, $options: 'i' } }, { 'senderEmail': ?2 } ] }", count = true)
	long countByCollatedField(String fieldname, String regex, String email, Collation collation);
	
	long countByStatusAndSenderEmail(String status, String email);
	
	long countBySenderEmail(String email);

	@Query("{ 'envelopeId' : { $in: ?0 } }")
	List<ClmContractDocument> findByEnvelopeIdIn(List<String> listOfEnvelopeIds);

}
