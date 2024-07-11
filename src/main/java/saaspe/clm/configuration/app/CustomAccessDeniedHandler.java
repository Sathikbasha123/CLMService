package saaspe.clm.configuration.app;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");
		String json1 = "{\r\n" + "    \"status\": \"Forbidden\",\r\n" + "    \"response\": {\r\n"
				+ "        \"action\": \"AccessResponse\",\r\n" + "        \"data\": [\r\n" + "        ]\r\n"
				+ "    },\r\n" + "    \"message\": \"Access denied\"\r\n" + "}";
		response.getWriter().write(json1);
	}
}
