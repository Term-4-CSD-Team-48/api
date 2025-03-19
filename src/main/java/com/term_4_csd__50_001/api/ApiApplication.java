package com.term_4_csd__50_001.api;

import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.FilterChainProxy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return _ -> {

			log.info("Let's inspect the beans provided by Spring Boot:");

			// Retrieve the FilterChainProxy (it holds all the filters)
			FilterChainProxy filterChainProxy = ctx.getBean(FilterChainProxy.class);

			// Print out all filters in the proxy
			log.info("SFC filters");
			filterChainProxy.getFilters("/**").forEach(filter -> {
				log.info("Filter: " + filter.getClass().getName());
			});

			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String _ : beanNames) {
				// log.info(beanName);
			}

		};
	}

}
