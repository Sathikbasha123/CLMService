package saaspe.clm.configuration.app;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import saaspe.clm.constant.Constant;
import saaspe.clm.filters.JWTAuthorizationFilter;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.serviceImpl.AuthenticationUserDetailService;
import saaspe.clm.utills.RedisUtility;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

	@Autowired
	private AuthenticationUserDetailService authenticationUserDetailService;

	@Autowired
	private RedisUtility redisUtility;

	@Autowired
	private UserInfoRespository userInfoRespository;

	@Value("${app.encryption.key}")
	private String encryptionKey;

	@Value("${app.jwt.key}")
	private String jwtKey;

	@Value("${docusign.host}")
	private String docusignHost;

	@Value("${admin.roles}")
	private String adminRoles;

	@Value("${clm.ad.group.name}")
	private String clmAdGroupName;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors().and().csrf().disable().authorizeRequests().antMatchers(HttpMethod.POST, Constant.SIGN_UP_URL)
				.permitAll().antMatchers(HttpMethod.POST, Constant.RESET_PASSWORD_URL).permitAll()
				.antMatchers("/v2/api-docs", "/configuration/ui", "/swagger-resources/**", "/configuration/security",
						"/swagger-ui.html", "/webjars/**","/api/v1/clm/recipientView")
				.permitAll().antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
				.antMatchers(HttpMethod.POST, Constant.DOCUSIGN_EVENTS).permitAll()
				.antMatchers(HttpMethod.GET, "/api/v1/clm/get/templates").permitAll()
				.anyRequest().authenticated().and()
				.addFilter(new JWTAuthorizationFilter(authenticationManager(), encryptionKey, jwtKey, redisUtility,
						docusignHost, adminRoles,userInfoRespository, clmAdGroupName))
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().exceptionHandling()
				.accessDeniedHandler(accessDeniedHandler()).and().headers()
				.contentSecurityPolicy("frame-ancestors 'self'").and().frameOptions().deny();
		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return new ProviderManager(Collections.singletonList(authenticationProvider()));
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(authenticationUserDetailService);
		return authenticationProvider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return new CustomAccessDeniedHandler();
	}

}
