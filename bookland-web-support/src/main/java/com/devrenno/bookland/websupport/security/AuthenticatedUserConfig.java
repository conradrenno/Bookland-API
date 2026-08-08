package com.devrenno.bookland.websupport.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers {@link AuthenticatedUserArgumentResolver} for every module's web layer at once.
 *
 * <p>Custom resolvers are consulted after the annotation- and type-based built-ins but before the
 * catch-all {@code ServletModelAttributeMethodProcessor}, so an unannotated
 * {@link AuthenticatedUser} parameter reaches this resolver instead of being bound from request
 * parameters.
 */
@Configuration
public class AuthenticatedUserConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticatedUserArgumentResolver());
    }
}
