package saaspe.clm.configuration.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoConfig {

	@Autowired
	public void setMapKeyDotReplacement(MappingMongoConverter mongoConverter) {
		mongoConverter.setMapKeyDotReplacement("-");
	}

}
