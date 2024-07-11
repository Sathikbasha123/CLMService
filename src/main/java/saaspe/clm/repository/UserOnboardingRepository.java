package saaspe.clm.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.UserOnboardingDocument;

public interface UserOnboardingRepository extends MongoRepository<UserOnboardingDocument, Long> {

	UserOnboardingDocument findByUniqueId(String uniqueId);

	@Query(value = "{'?0': ?1}")
	List<UserOnboardingDocument> findByAnyField(String fieldName, String value, Pageable pageable);

	@Query("{ $and: [ { '$or': [ { 'name': { $regex: ?1, $options: 'i' } }, { 'createdOn': { $regex: ?1, $options: 'i' } }, { 'updatedOn': { $regex: ?1, $options: 'i' } } ] } ] }")
	List<UserOnboardingDocument> findByCollatedField(String orderBy, String regex, Pageable page, Collation collation);

	@Query(sort = "{'createdOn':-1}")
	List<UserOnboardingDocument> findByEmail(String email);

//	UserOnboardingDocument findByEmail(String email);
}
