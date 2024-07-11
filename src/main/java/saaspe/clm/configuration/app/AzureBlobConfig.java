package saaspe.clm.configuration.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import freemarker.template.utility.NullArgumentException;
import saaspe.clm.constant.Constant;
import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.DocumentUploadResponse;
import saaspe.clm.model.Response;

@Component
public class AzureBlobConfig {

	@Autowired
	private CloudBlobClient cloudBlobClient;

	@Autowired
	private CloudBlobContainer cloudBlobContainer;

	public boolean createContainer(String containerName) throws StorageException, URISyntaxException {
		boolean containerCreated = false;
		CloudBlobContainer container = null;
		try {
			container = cloudBlobClient.getContainerReference(containerName);
		} catch (URISyntaxException e) {
			throw new URISyntaxException(Constant.URL_ERROR, Constant.UNABLE_TO_CONNECT_TO_AZURE);
		} catch (StorageException e) {
			throw new StorageException(Constant.INSUFFICIENT_STORAGE, Constant.CLIENT_ERROR, e);
		}
		if (container == null) {
			throw new NullArgumentException(containerName, "No Files!");
		}
		try {
			containerCreated = container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER,
					new BlobRequestOptions(), new OperationContext());
		} catch (StorageException ex) {
			throw new StorageException(Constant.INSUFFICIENT_STORAGE, Constant.CLIENT_ERROR, ex);
		}
		return containerCreated;
	}

	public List<URI> listBlobs(String containerName) throws StorageException, URISyntaxException {
		List<URI> uris = new ArrayList<>();
		try {
			CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
			for (ListBlobItem blobItem : container.listBlobs()) {
				uris.add(blobItem.getUri());
			}
		} catch (URISyntaxException e) {
			throw new URISyntaxException(Constant.URL_ERROR, Constant.UNABLE_TO_CONNECT_TO_AZURE);
		} catch (StorageException ex) {
			throw new StorageException(Constant.INSUFFICIENT_STORAGE, Constant.CLIENT_ERROR, ex);
		}
		return uris;
	}

	public void deleteBlob(String containerName, String blobName) throws StorageException, URISyntaxException {
		try {
			CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
			CloudBlockBlob blobToBeDeleted = container.getBlockBlobReference(blobName);
			blobToBeDeleted.deleteIfExists();
		} catch (URISyntaxException e) {
			throw new URISyntaxException(Constant.URL_ERROR, Constant.UNABLE_TO_CONNECT_TO_AZURE);
		} catch (StorageException ex) {
			throw new StorageException(Constant.INSUFFICIENT_STORAGE, Constant.CLIENT_ERROR, ex);
		}
	}

	public CommonResponse upload(List<MultipartFile> multipartFiles, String fileName) throws URISyntaxException {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		List<DocumentUploadResponse> list = new ArrayList<>();
		CloudBlockBlob blob = null;
		try {
			String path = fileName;
			for (MultipartFile file : multipartFiles) {
				DocumentUploadResponse uploadResponse = new DocumentUploadResponse();
				blob = cloudBlobContainer.getBlockBlobReference(path + "/" + file.getOriginalFilename());
				blob.upload(file.getInputStream(), -1);
				uploadResponse.setFilePath(blob.getName());
				list.add(uploadResponse);
				response.setData(list);
			}
			response.setAction("MultipartFile");
			commonResponse.setMessage("File Upload Success");
			commonResponse.setStatus(HttpStatus.OK);
			commonResponse.setResponse(response);
			return commonResponse;
		} catch (StorageException e) {
			response.setAction("MultipartFile");
			response.setData(e);
			commonResponse.setMessage("Storage Exception");
			commonResponse.setStatus(HttpStatus.CONFLICT);
			commonResponse.setResponse(response);
			return commonResponse;
		} catch (IOException e) {
			response.setAction("MultipartFile");
			response.setData(e);
			commonResponse.setMessage("I/O Exception");
			commonResponse.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
			commonResponse.setResponse(response);
			return commonResponse;

		}
	}
}
