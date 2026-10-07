package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalog service boots with every module its pom brings — catalog and inventory — wired, its own
 * schema migrated and its dev seed loaded.
 */
@CatalogIntegrationTest
class CatalogApplicationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("the context loads; Flyway created the catalog's schema and the categories")
    void contextLoads() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from categories", Integer.class)).isEqualTo(8);
    }

    @Test
    @DisplayName("the dev seed is there: public book reads answer 200 with books")
    void devSeedIsLoaded() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select count(*) from books", Integer.class)).isPositive();
        mockMvc.perform(get("/api/v1/books")).andExpect(status().isOk());
    }

    /**
     * Boot 4 moved the H2 console into its own module; without it the path is a silent 404. Checked
     * on the registration bean: the console is a servlet of its own, which MockMvc cannot reach.
     */
    @Test
    @DisplayName("the H2 console is registered in dev")
    void registersTheH2ConsoleInDev() {
        assertThat(context.getBean("h2Console", ServletRegistrationBean.class).getUrlMappings())
                .containsExactly("/h2-console/*");
    }

    /** The catalog's tables only: nothing of orders, payments or reviews lives here. */
    @Test
    @DisplayName("the database holds the catalog's tables and no one else's")
    void onlyTheCatalogsTables() {
        assertThat(jdbcTemplate.queryForList("""
                select lower(table_name) from information_schema.tables
                where lower(table_schema) = 'public' and lower(table_name) <> 'flyway_schema_history'
                """, String.class))
                .containsExactlyInAnyOrder("categories", "books", "book_authors", "stock_reservations",
                        "stock_reservation_items", "inventory_entries", "catalog_outbox", "catalog_inbox");
    }
}
