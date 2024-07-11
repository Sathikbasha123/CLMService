package saaspe.clm.configuration.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

@Configuration
public class AzureStorageConfig {

	@Value("${azure.storage.connection}")
	private String azureStorageConnectionString;

	@Value("${azure.storage.container.name}")
	private String azureStorageContainerName;

	@Bean
	public CloudBlobClient cloudBlobClient() throws Exception {
		CloudStorageAccount storageAccount = CloudStorageAccount.parse(azureStorageConnectionString);
		return storageAccount.createCloudBlobClient();
	}

	@Bean
	public CloudBlobContainer cloudBlobContainer(CloudBlobClient cloudBlobClient) throws Exception {
		CloudBlobContainer container = cloudBlobClient.getContainerReference(azureStorageContainerName);
		container.createIfNotExists();
		return container;
	}
}
