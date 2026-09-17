package com.plumora.api.shared.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit, transactional demo fixtures, separate from schema migrations. */
@Component
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private final DataSource dataSource;
    private final PasswordEncoder encoder;
    private final String password;

    public DemoDataSeeder(DataSource dataSource, PasswordEncoder encoder,
                          @Value("${app.demo.password:}") String password) {
        this.dataSource = dataSource;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("DEMO_SEED_PASSWORD must contain at least 12 characters when demo seeding is enabled");
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // Serialize concurrent startup seeds; the transaction also rolls back every fixture on failure.
        jdbc.execute("select pg_advisory_xact_lock(72665001)");
        jdbc.queryForObject("select set_config('plumora.demo_password_hash', ?, true)",
            String.class, encoder.encode(password));
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        for (String script : new String[] {
            "V14__seed_internal_plumora_catalog.sql", "V15__use_local_plumora_demo_covers.sql",
            "V16__add_uninvited_beta_reader.sql", "V19__expand_internal_plumora_catalog.sql",
            "workflows.sql"
        }) {
            populator.addScript(new ClassPathResource("db/demo/" + script));
        }
        populator.execute(dataSource);
        log.info("Optional Plumora demo fixtures initialized; existing accounts and manuscripts preserved");
    }
}
