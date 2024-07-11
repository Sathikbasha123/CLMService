package saaspe.clm.configuration.app;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class AzureConfig {

	AzureConfig() {
	}

	public static boolean isValidToken(String token) {
		if (token != null && token.startsWith("Bearer ")) {
			int index = token.indexOf("Bearer ") + 7;
			token = token.substring(index);
		} else {
			return false;
		}
		DecodedJWT jwt = JWT.decode(token);
		try {
			Date now = new Date();
			Date notBefore = jwt.getNotBefore();
			Date expiresAt = jwt.getExpiresAt();
			return notBefore != null && expiresAt != null && now.toInstant().compareTo(notBefore.toInstant()) >= 0
					&& now.toInstant().isBefore(expiresAt.toInstant());
		} catch (SignatureVerificationException e) {
			throw new SignatureVerificationException(null, e);
		}
	}

}
