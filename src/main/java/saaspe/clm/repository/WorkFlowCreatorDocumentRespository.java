package saaspe.clm.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import saaspe.clm.document.WorkFlowCreatorDocument;

public interface WorkFlowCreatorDocumentRespository extends MongoRepository<WorkFlowCreatorDocument, Long> {

	@Query("{'email': ?0, 'flowType': ?1}")
	List<WorkFlowCreatorDocument> findByEmailAndFlowType(String email, String flowType, Pageable pageable);

	@Query(value = "{email:?0,'flowType': ?1}", count = true)
	long findByEmail(String email, String flowType);

	@Query(value = "{envelopeId:?0}")
	WorkFlowCreatorDocument findByEnvelopeId(String envelopeId);

	long countByEmail(String email);

	@Query("{'senderEmail':?0}")
	List<WorkFlowCreatorDocument> findAllDocumentsBySenderEmail(PageRequest pageable, String email);

	@Query("{ $and :[ {'$or':[ {'contractName': { $regex:?1,$options:'i' } },{'subsidiary': { $regex:?1,$options:'i' } },{'projectId': { $regex:?1,$options:'i' }},{'createdOn': { $regex:?1,$options:'i' } }] },{ 'email' : ?2  } ,{'flowType': ?3}] }")
	List<WorkFlowCreatorDocument> findByField(String orderBy, String string, String email, String flowType,
			PageRequest pageable, Collation collation);

	@Query(value = "{ $and :[ {'$or':[ {'contractName': { $regex:?1,$options:'i' } },{'subsidiary': { $regex:?1,$options:'i' } },{'projectId': { $regex:?1,$options:'i' } }] },{ 'email' : ?2},{'flowType':?3 }  ] }", count = true)
	long findDocumentCountByField(String orderBy, String searchText, String email, String flowType,
			PageRequest pageable, Collation collation);

}
