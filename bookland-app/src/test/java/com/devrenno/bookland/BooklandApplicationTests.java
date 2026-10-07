package com.devrenno.bookland;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@BooklandIntegrationTest
class BooklandApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * The services that left took their tables with them: identity's in step 3, the catalog's
     * (books, categories, stock reservations, inventory, its outbox and inbox) in step 5b. A table
     * here that another service owns would be a second, stale copy of its data.
     */
    @Test
    void holdsNoTableOfTheServicesThatLeft() {
        assertThat(jdbcTemplate.queryForList("""
                select lower(table_name) from information_schema.tables where lower(table_schema) = 'public'
                """, String.class))
                .doesNotContain("users", "oauth2_authorization", "books", "book_authors", "categories",
                        "stock_reservations", "stock_reservation_items", "inventory_entries",
                        "catalog_outbox", "catalog_inbox")
                .contains("orders", "carts", "payments", "reviews", "wishlists");
    }
}
