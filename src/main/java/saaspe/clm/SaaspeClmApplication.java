package saaspe.clm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaaspeClmApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaaspeClmApplication.class, args);
	}

}
