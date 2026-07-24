package ir.aut.logmonitor.evaluator.rules;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Loads rule definitions from the YAML config file specified by
 * `app.rules.config-path`. The list of parsed {@link RuleDefinition}s is
 * read once at startup and kept in memory for the RuleEngine to use.
 */
@Component
@Profile({"evaluator", "default"})
public class RulesConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(RulesConfigLoader.class);

    private final String configPath;
    private List<RuleDefinition> ruleDefinitions = Collections.emptyList();

    public RulesConfigLoader(@Value("${app.rules.config-path}") String configPath) {
        this.configPath = configPath;
    }

    @PostConstruct
    public void load() {
        Path path = Paths.get(configPath);

        if (!Files.exists(path)) {
            log.warn("Rules config file not found at {}. No rules will be evaluated.", path);
            return;
        }

        Yaml yaml = new Yaml(new Constructor(RulesConfig.class, new LoaderOptions()));
        try (InputStream input = Files.newInputStream(path)) {
            RulesConfig config = yaml.load(input);
            this.ruleDefinitions = (config != null && config.getRules() != null)
                    ? config.getRules()
                    : Collections.emptyList();
            log.info("Loaded {} rule definition(s) from {}", ruleDefinitions.size(), path);
        } catch (IOException e) {
            log.error("Failed to read rules config file at {}", path, e);
        } catch (Exception e) {
            // Covers YAMLException and other parsing errors from malformed config content.
            log.error("Failed to parse rules config file at {}. No rules will be evaluated.", path, e);
            this.ruleDefinitions = Collections.emptyList();
        }
    }

    public List<RuleDefinition> getRuleDefinitions() {
        return ruleDefinitions;
    }
}