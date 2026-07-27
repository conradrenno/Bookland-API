package com.devrenno.bookland.websupport.validation;

import jakarta.validation.MessageInterpolator;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Locale;

/**
 * Replaces Boot's auto-configured validator with the same thing plus a fixed English locale.
 * Declaring a {@code Validator} bean makes {@code ValidationAutoConfiguration} back off, and
 * {@link MessageInterpolatorFactory} is what it would have used, so nothing else about the
 * validator changes.
 */
@Configuration
public class ValidationConfig {

    @Bean
    public static LocalValidatorFactoryBean defaultValidator(ApplicationContext applicationContext) {
        MessageInterpolator interpolator = new MessageInterpolatorFactory(applicationContext).getObject();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new FixedLocaleMessageInterpolator(interpolator, Locale.ENGLISH));
        return validator;
    }
}
