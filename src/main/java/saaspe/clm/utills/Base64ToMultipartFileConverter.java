package saaspe.clm.utills;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.web.multipart.MultipartFile;

public class Base64ToMultipartFileConverter {

	public static MultipartFile convert(String base64String, String filename, String contentType) throws IOException {
		byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64String);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);

		MultipartFile multipartFile = new MultipartFile() {
			@Override
			public String getName() {
				return filename;
			}

			@Override
			public String getOriginalFilename() {
				return filename;
			}

			@Override
			public String getContentType() {
				return contentType;
			}

			@Override
			public boolean isEmpty() {
				return decodedBytes.length == 0;
			}

			@Override
			public long getSize() {
				return decodedBytes.length;
			}

			@Override
			public byte[] getBytes() throws IOException {
				return decodedBytes;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return byteArrayInputStream;
			}

			@Override
			public void transferTo(File dest) throws IOException, IllegalStateException {
				try (OutputStream outputStream = new FileOutputStream(dest)) {
					outputStream.write(decodedBytes);
				}
			}
		};

		return multipartFile;
	}
}
