package org.springframework.data.neo4j.integration.jsr303;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@ComponentScan({"org.springframework.data.neo4j.integration.jsr303.controller"})
@EnableWebMvc
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @Bean(name="validator")
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

}
