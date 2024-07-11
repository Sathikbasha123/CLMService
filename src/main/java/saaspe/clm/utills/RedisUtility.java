
package saaspe.clm.utills;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import saaspe.clm.model.DocusignUserCache;

@Component
public class RedisUtility {

	@Autowired
	private RedisTemplate<String, String> template;

	@Autowired
	private Gson gson;

	public void setValue(final String key, TokenCache cache, final Date date) {
		template.opsForValue().set(key, gson.toJson(cache));
		template.expireAt(key, date);
	}

	public TokenCache getValue(final String key) {
		String json = template.opsForValue().get(key);
		if (json != null) {
			try {
				return gson.fromJson(json, TokenCache.class);
			} catch (JsonSyntaxException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void setDocusingValue(final String key, final DocusignUserCache cache) {
		template.opsForValue().set(key, gson.toJson(cache));
	}

	public DocusignUserCache getDocusignValue(final String key) {
		String json = template.opsForValue().get(key);
		if (json != null) {
			try {
				return gson.fromJson(json, DocusignUserCache.class);
			} catch (JsonSyntaxException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void deleteKeyformredis(String key) {
		template.delete(key);
	}
}
