package saaspe.clm.configuration.mongo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MultipleMongoConfig {


	@Value("${spring.data.mongodb.docusign.host}")
	private String docusignHost;

	@Value("${spring.data.mongodb.docusign.port}")
	private int docusignPort;

	@Value("${spring.data.mongodb.docusign.database}")
	private String docusignDataBaseName;

	@Value("${spring.data.mongodb.docusign.username}")
	private String docusignUserName;

	@Value("${spring.data.mongodb.docusign.password}")
	private String docusignPaasowrd;

	@Primary
	@Bean(name = "DocusginProperties")
	@ConfigurationProperties(prefix = "spring.data.mongodb.docusign")
	public MongoProperties getDocusignProperties() {
		return new MongoProperties();
	}

	@Bean(name = "DocusigMongoTemplate")
	public MongoTemplate docusignMongoTemplate() throws Exception {
		return new MongoTemplate(docusginMongoDatabaseFactory(getDocusignProperties()));
	}

	@Bean
	public MongoDatabaseFactory docusginMongoDatabaseFactory(MongoProperties mongo) {
		String prefix = "mongodb://";
		String url = prefix + docusignUserName + ":" + docusignPaasowrd + "@" + docusignHost + ":" + docusignPort + "/"
				+ docusignDataBaseName + "?authMechanism=SCRAM-SHA-256&authSource=admin";
		return new SimpleMongoClientDatabaseFactory(url);
	}

}
