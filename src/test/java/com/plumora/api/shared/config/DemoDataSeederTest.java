package com.plumora.api.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoDataSeederTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(DemoDataSeeder.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class));

    @Test
    void seedIsAbsentByDefaultAndWhenDisabled() {
        context.run(c -> assertThat(c).doesNotHaveBean(DemoDataSeeder.class));
        context.withPropertyValues("app.demo.seed-enabled=false")
            .run(c -> assertThat(c).doesNotHaveBean(DemoDataSeeder.class));
    }

    @Test
    void explicitOptInRegistersSeeder() {
        context.withPropertyValues("app.demo.seed-enabled=true")
            .run(c -> assertThat(c).hasSingleBean(DemoDataSeeder.class));
    }

    @Test
    void missingPasswordFailsBeforeAnyDatabaseWrite() {
        DataSource dataSource = mock(DataSource.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        assertThatThrownBy(() -> new DemoDataSeeder(dataSource, encoder, "").run(null))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("DEMO_SEED_PASSWORD");
        verifyNoInteractions(dataSource, encoder);
    }
}
