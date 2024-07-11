package saaspe.clm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import saaspe.clm.document.EventDocument;

public interface EventRepository extends MongoRepository<EventDocument, Long> {

}
