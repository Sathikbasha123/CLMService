package saaspe.clm.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.UsersInfoDocument;

public interface UserInfoRespository extends MongoRepository<UsersInfoDocument, Long> {

	UsersInfoDocument findByUniqueId(String uniqueId);

	@Query("{'isActive':?0}")
	List<UsersInfoDocument> findByActiveAndDeActive(Boolean isActive, Pageable pageable);

	@Query("{'createdThrough':?0}")
	List<UsersInfoDocument> findBycreatedThrough(String isActive, Pageable pageable);

	@Query("{'division':?0}")
	List<UsersInfoDocument> findBydivision(String division, Pageable pageable);

	@Query(value = "{'?0': ?1}")
	List<UsersInfoDocument> findByAnyField(String fieldName, String value, Pageable pageable);

	@Query("{ $and: [ { '$or': [ { 'name': { $regex: ?1, $options: 'i' } }, { 'createdOn': { $regex: ?1, $options: 'i' } }, { 'updatedOn': { $regex: ?1, $options: 'i' } } ] } ] }")
	List<UsersInfoDocument> findByCollatedField(String orderBy, String regex, Pageable page, Collation collation);

	@Query(value = "{'?0': ?1}")
	List<UsersInfoDocument> findByAnyFieldBoolean(String fieldName, boolean value, Pageable pageable);

	@Query(value = "{$and:[{'email':?0},{'isActive': ?1}]}")
	UsersInfoDocument findByEmailAndActive(String email, boolean isActive);

	@Query(value ="{'email':?0}")
	UsersInfoDocument findByEmail(String email);

	@Query(value = "{'currentRole':?0}",fields = "{'email':1}")
	List<UsersInfoDocument> findByCurrentRole(String string);

}
