package com.brex.demo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * SQLite's JDBC driver refuses to create missing parent directories for the
 * database file, so make sure the configured directory exists before the
 * DataSource bean tries to open a connection.
 */
public class SqliteDataDirInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String url = Binder.get(applicationContext.getEnvironment())
                .bind("spring.datasource.url", Bindable.of(String.class))
                .orElse(null);
        if (url == null || !url.startsWith("jdbc:sqlite:") || url.contains(":memory:")) {
            return;
        }
        String path = url.substring("jdbc:sqlite:".length());
        Path parent = Path.of(path).toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException("Could not create SQLite data directory: " + parent, e);
            }
        }
    }
}
