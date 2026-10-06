package com.devrenno.bookland;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@IdentityIntegrationTest
class IdentityApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    /**
     * Boot 4 moved the H2 console into its own module; without it {@code spring.h2.console.enabled}
     * is ignored and the console answers 404 with no error anywhere. Checked on the registration
     * bean because the console is a servlet of its own, which MockMvc cannot reach.
     */
    @Test
    void registersTheH2ConsoleInDev() {
        assertThat(context.getBean("h2Console", ServletRegistrationBean.class).getUrlMappings())
                .containsExactly("/h2-console/*");
    }
}
