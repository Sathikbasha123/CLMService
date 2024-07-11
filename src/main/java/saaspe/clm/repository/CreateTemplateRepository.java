package saaspe.clm.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import saaspe.clm.document.CreateTemplate;

public interface CreateTemplateRepository extends MongoRepository<CreateTemplate, Long> {

	CreateTemplate findByTemplateId(String templateId);

	@Query(value="{'flowType' : { $regex: ?0, $options: 'i' }}",sort = "{ 'createdOn' : -1 }")
	List<CreateTemplate> findByCustomFlowTypeQuery(String flowType);

	@Query("{ $and: [{'$or':[ {'templateName': { $regex:?0,$options:'i' } }] }, {flowType: { $regex: ?1, $options: 'i' }}]}")
	List<CreateTemplate> findByTemplate(String templateName, Pageable pageable,String flowType);

	@Query(value="{ $and: [{'$or':[ {'templateName': { $regex:?0,$options:'i' } }] }, {flowType: { $regex: ?1, $options: 'i' }}]}",count=true)
	long findByTemplateNameCount(String templateName,String flowType);

	
	@Query("{'flowType' : { $regex: ?0, $options: 'i' }}")
	List<CreateTemplate> findByCustomFlowTypePageable(String flowType,Pageable pageable);
	
	@Query(value="{'flowType' : { $regex: ?0, $options: 'i' }}",count=true)
	long findByCustomFlowTypePageableCount(String flowType);
}
