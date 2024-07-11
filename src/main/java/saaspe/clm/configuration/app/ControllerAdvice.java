package saaspe.clm.configuration.app;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;

@RestControllerAdvice
public class ControllerAdvice {

	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<CommonResponse> handleMultipartt(MissingServletRequestParameterException e) {

		return new ResponseEntity<CommonResponse>(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Invalid Parameter", new ArrayList<>()),
				e.getMessage()), HttpStatus.BAD_REQUEST);

	}
	
	@ExceptionHandler(MissingServletRequestPartException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<CommonResponse> handleMultipart(MissingServletRequestPartException e) {

		return new ResponseEntity<CommonResponse>(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Invalid Multipart", null),
				"File Upload Failed"), HttpStatus.BAD_REQUEST);

	}
	
	
}
