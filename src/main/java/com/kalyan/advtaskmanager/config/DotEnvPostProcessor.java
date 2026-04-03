package com.kalyan.advtaskmanager.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a .env file from the working directory into Spring's Environment.
 *
 * Priority rules:
 *  • OS / Railway / Docker env vars  ──→  always win  (added with addLast)
 *  • .env file values                ──→  lowest priority fallback
 *  • application.properties defaults ──→  used only when neither above is set
 *
 * Safe in every environment:
 *  • Local IntelliJ / mvnw   → reads .env  ✅
 *  • Docker Compose          → env_file: .env already injects vars at OS level,
 *                              so .env file values are ignored  ✅
 *  • Railway / CI            → no .env file present → silently skips  ✅
 */
public class DotEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DOT_ENV    = ".env";
    private static final String SOURCE_NAME = "dotEnvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Path envFile = Path.of(DOT_ENV);
        if (!Files.exists(envFile)) {
            return; // no .env → nothing to do (Docker / Railway / CI)
        }

        Map<String, Object> props = new LinkedHashMap<>();
        try (var lines = Files.lines(envFile)) {
            lines.forEach(raw -> {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) return;

                int eq = line.indexOf('=');
                if (eq <= 0) return;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                if (!environment.containsProperty(key)) {
                    props.put(key, value);
                }
            });
        } catch (IOException ignored) {
            // Can't read .env → silently skip
        }

        if (!props.isEmpty()) {
            // addLast = lowest priority in the property source chain
            environment.getPropertySources().addLast(
                    new MapPropertySource(SOURCE_NAME, props));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // run last so real env vars are already present
    }
}


