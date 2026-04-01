package com.idbiintech.OICR;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OicrApplication {

	public static void main(String[] args) {
		SpringApplication.run(OicrApplication.class, args);
	}

}
