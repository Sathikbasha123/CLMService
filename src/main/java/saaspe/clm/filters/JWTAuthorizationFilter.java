package saaspe.clm.filters;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import saaspe.clm.configuration.app.AzureConfig;
import saaspe.clm.constant.Constant;
import saaspe.clm.custom.CustomUserDetails;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.exception.CustomAuthenticationException;
import saaspe.clm.model.DocusignCreateUserResponse;
import saaspe.clm.model.DocusignUserCache;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.utills.RedisUtility;
import saaspe.clm.utills.TokenCache;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
	private String docusignHost;
	private RedisUtility redisUtility;
	private RestTemplate restTemplate;
	private String adminRoles;
	private String clmAdGroupName;

	private UserInfoRespository userInfoRespository;

	private static final Logger log = LoggerFactory.getLogger(JWTAuthorizationFilter.class);
	
	public JWTAuthorizationFilter(AuthenticationManager authenticationManager, String encryptionKey, String jwtKey,
			RedisUtility redisUtility, String docusignHost, String adminRoles, UserInfoRespository userInfoRespository,
			String clmAdGroupName) {
		super(authenticationManager);
		this.restTemplate = new RestTemplate();
		this.redisUtility = redisUtility;
		this.docusignHost = docusignHost;
		this.adminRoles = adminRoles;
		this.userInfoRespository = userInfoRespository;
		this.clmAdGroupName = clmAdGroupName;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException, BadCredentialsException {
		String header = request.getHeader(Constant.HEADER_STRING);
		if (header == null || !header.startsWith(Constant.TOKEN_PREFIX)) {
			chain.doFilter(request, response);
			return;
		}
		try {
			UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
			if (authentication != null) {
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			chain.doFilter(request, response);
		} catch (BadCredentialsException e) {
			sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", e.getLocalizedMessage());
		} catch (CustomAuthenticationException authenticationException) {
			sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
					authenticationException.getLocalizedMessage());
		}
	}

	private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request)
			throws JsonProcessingException, BadCredentialsException {
		
		log.info("Inside getAuthentication method");
		String token = request.getHeader(Constant.HEADER_STRING);
		String provider = request.getHeader(Constant.HEADER_PROVIDER_STRING);
		log.info("Auth provider:{}",provider);
		if (provider!=null && provider.equalsIgnoreCase(Constant.AZURE)) {
			log.info("Inside provider condition");
			return authenticateAzureUser(token,request);
		} else {
			throw new BadCredentialsException("Vendor Should be in the list or Vendor is Null");
		}
	}

	private UsernamePasswordAuthenticationToken authenticateAzureUser(String token,HttpServletRequest request)
			throws JsonProcessingException, BadCredentialsException, CustomAuthenticationException {
		if (token != null) {
			log.info("Inside authenticateAzureUser method");
			DecodedJWT jwt = JWT.decode(token.replace(Constant.TOKEN_PREFIX, ""));
			String email = null;
			if (jwt.getClaim(Constant.EMAIL).asString() != null) {
				email = jwt.getClaim(Constant.EMAIL).asString();
			} else if (jwt.getClaim("upn").asString() != null) {
				email = jwt.getClaim("upn").asString();
			}
			TokenCache cacheValue = redisUtility.getValue(Constant.AZURE + email);
			TokenCache docChcek = redisUtility.getValue(Constant.AZURE_TOKEN + email);
			DocusignUserCache docusignUserCheck = redisUtility.getDocusignValue(Constant.DOCUSIGN_REDIS_PREFIX + email);
			System.out.println(docusignUserCheck);
			if (cacheValue == null) {
				log.info("Inside cacheValue null condition");
				validateAzureToken(token);
				log.info("Success in validateAzureToken method");
//				GraphGroupsResponse groupsResponse = fetchUserGroups(token);
//				boolean isUser = false;
//				for (Value value : groupsResponse.getValue()) {
//					if(clmAdGroupName.equalsIgnoreCase(value.getDisplayName())){  // Constant value need to be replaced after group created
//						isUser = true;
//						String userRole = value.getDisplayName().toUpperCase().replace("-", "_");
//					}
//				}
//				if (isUser) {
				Set<SimpleGrantedAuthority> authorities = new HashSet<>();
				UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmail(email);
				log.info("UserInfoDocument {}",usersInfoDocument);
				if (usersInfoDocument != null && usersInfoDocument.getCurrentRole() != null) {
					authorities.add(new SimpleGrantedAuthority(usersInfoDocument.getCurrentRole()));
				} else {
					if(!(request.getRequestURI().contains("user/onboarding/request")||request.getRequestURI().contains("/userprofile/access")
							||request.getRequestURI().contains("user/fetch/user-roles")||request.getRequestURI().contains("user/fetch/current-role")))
					throw new CustomAuthenticationException("User not found or removed by Admin");
				}
				TokenCache cache = new TokenCache();
				cache.setEmailAddress(email);
				cache.setDisplayname(usersInfoDocument != null ? usersInfoDocument.getCurrentRole() : null);
				cache.setExpiryDate(jwt.getExpiresAt());
				cache.setToken(token.replace(Constant.TOKEN_PREFIX, ""));
				redisUtility.setValue(Constant.AZURE + email, cache, jwt.getExpiresAt());
				if (docChcek == null) {
					if (docusignUserCheck == null) {
						yourMethodToNotify(token);
					}
					TokenCache docCache = new TokenCache();
					docCache.setEmailAddress("token" + email);
					docCache.setDisplayname(usersInfoDocument != null ? usersInfoDocument.getCurrentRole() : null);
					docCache.setExpiryDate(jwt.getExpiresAt());
					docCache.setToken(token.replace(Constant.TOKEN_PREFIX, ""));
					redisUtility.setValue(Constant.AZURE_TOKEN + email, docCache, jwt.getExpiresAt());
				}
				CustomUserDetails userDetails = new CustomUserDetails(jwt.getClaim(Constant.EMAIL).asString(), null);
				log.info("UserDetails email {}",userDetails.getEmail());
				return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
//				} else {
//					throw new BadCredentialsException("User Not present in the group");
//				}
			} else {
				log.info("Inside cacheValue condition");
				validateAzureToken(token);
				log.info("Success in validateAzureToken method");
				Set<SimpleGrantedAuthority> authorities = new HashSet<>();
				UsersInfoDocument usersInfoDocument = userInfoRespository.findByEmailAndActive(email, true);
				if (usersInfoDocument != null && usersInfoDocument.getCurrentRole() != null) {
					authorities.add(new SimpleGrantedAuthority(usersInfoDocument.getCurrentRole()));
				} else {
					if(!(request.getRequestURI().contains("user/onboarding/request")||request.getRequestURI().contains("/userprofile/access")
							||request.getRequestURI().contains("user/fetch/user-roles")||request.getRequestURI().contains("user/fetch/current-role")))
					throw new CustomAuthenticationException("User not found or removed by Admin");
				}
				if (docusignUserCheck == null) {
					yourMethodToNotify(token);
				}
				if (docChcek == null) {
					TokenCache docCache = new TokenCache();
					docCache.setEmailAddress("token" + email);
					docCache.setDisplayname(usersInfoDocument != null ? usersInfoDocument.getCurrentRole() : null);
					docCache.setExpiryDate(jwt.getExpiresAt());
					docCache.setToken(token.replace(Constant.TOKEN_PREFIX, ""));
					redisUtility.setValue(Constant.AZURE_TOKEN + email, docCache, jwt.getExpiresAt());
				}
				redisUtility.deleteKeyformredis(Constant.AZURE + email);
				TokenCache cache = new TokenCache();
				cache.setEmailAddress(email);
				cache.setDisplayname(usersInfoDocument != null ? usersInfoDocument.getCurrentRole() : null);
				cache.setExpiryDate(jwt.getExpiresAt());
				cache.setToken(token.replace(Constant.TOKEN_PREFIX, ""));
				redisUtility.setValue(Constant.AZURE + email, cache, jwt.getExpiresAt());
				CustomUserDetails userDetails = new CustomUserDetails(email, null);
				log.info("UserDetails email {}",userDetails.getEmail());
				return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
			}
		}
		return null;
	}

	private void validateAzureToken(String token) {
		log.info("Inside validateAzureToken method");
		boolean valid = AzureConfig.isValidToken(token);
		if (!valid) {
			throw new BadCredentialsException("Token Already Expired");
		}
	}

	public void yourMethodToNotify(String message) throws JsonProcessingException {
		Map<String, String> userDetails = getUserDetailsForSSOUser(message);
		String firstName = null;
		if (!userDetails.get(Constant.FIRST_NAME).equalsIgnoreCase("null")) {
			firstName = userDetails.get(Constant.FIRST_NAME);
		} else {
			firstName = userDetails.get(Constant.DISPLAY_NAME);
		}
		getClmUsersList(userDetails.get(Constant.EMAIL), firstName, userDetails.get(Constant.LAST_NAME));
	}

	private void getClmUsersList(String userEmail, String firstName, String lastName) throws JsonProcessingException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = null;
		try {
			URI uri;
			uri = UriComponentsBuilder.fromUriString(docusignHost + "/create/user").queryParam("userEmail", userEmail)
					.queryParam(Constant.FIRST_NAME, firstName).queryParam(Constant.LAST_NAME, lastName).build()
					.toUri();
			String URL = URLDecoder.decode(uri.toString(), "UTF-8");
			response = restTemplate.exchange(URL, HttpMethod.GET, entity, String.class);
			ObjectMapper mapper = new ObjectMapper();
			DocusignCreateUserResponse docusingCreateUserResponse = mapper.readValue(response.getBody(),
					DocusignCreateUserResponse.class);
			System.out.println("Error :"+docusingCreateUserResponse.getError());
			DocusignUserCache doCache = new DocusignUserCache();
			doCache.setUserName(
					docusingCreateUserResponse.getUserName() != null ? docusingCreateUserResponse.getUserName()
							: docusingCreateUserResponse.getUser_name());
			doCache.setUserId(docusingCreateUserResponse.getId());
			doCache.setUserEmail(docusingCreateUserResponse.getEmail());
			redisUtility.setDocusingValue(Constant.DOCUSIGN_REDIS_PREFIX + docusingCreateUserResponse.getEmail(),
					doCache);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, String> getUserDetailsForSSOUser(String token) {
		Map<String, String> userDetails = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(token.replace(Constant.TOKEN_PREFIX, ""));
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(Constant.GRAPH_GROUP_URL_ME, HttpMethod.GET, entity,
				String.class);
		String responseBody = response.getBody();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = objectMapper.readTree(responseBody);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		userDetails.put(Constant.DISPLAY_NAME, rootNode.get(Constant.DISPLAY_NAME).asText());
		userDetails.put(Constant.FIRST_NAME, rootNode.get("givenName").asText());
		userDetails.put(Constant.EMAIL, rootNode.get("mail").asText());
		userDetails.put(Constant.LAST_NAME, rootNode.get("surname").asText());
		return userDetails;
	}

//	private GraphGroupsResponse fetchUserGroups(String token) {
//		HttpHeaders headers = new HttpHeaders();
//		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
//		headers.setBearerAuth(token.replace(Constant.TOKEN_PREFIX, ""));
//		HttpEntity<String> entity = new HttpEntity<>(headers);
//		ResponseEntity<GraphGroupsResponse> response = restTemplate.exchange(Constant.GRAPH_GROUP_URL, HttpMethod.GET,
//				entity, GraphGroupsResponse.class);
//		return response.getBody();
//	}

	private void sendErrorResponse(HttpServletResponse response, int statusCode, String status, String message)
			throws IOException {
		response.setStatus(statusCode);
		response.setContentType("application/json");
		Map<String, String> object = new HashMap<>();
		object.put("message", message);
		object.put("status", status);
		String json = new Gson().toJson(object);
		response.getWriter().write(json);
	}

}