package ir.aut.logmonitor.evaluator.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RulesConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsAllThreeRuleTypesCorrectly() throws IOException {
        String yamlContent = """
                rules:
                  - name: error-log-rule
                    type: LOG_TYPE
                    logLevel: ERROR

                  - name: warning-rate-rule
                    type: TYPE_RATE
                    logLevel: WARNING
                    windowSeconds: 300
                    threshold: 10

                  - name: overall-rate-rule
                    type: OVERALL_RATE
                    windowSeconds: 300
                    threshold: 50
                """;
        Path rulesFile = writeTempRulesFile(yamlContent);

        RulesConfigLoader loader = new RulesConfigLoader(rulesFile.toString());
        loader.load();

        List<RuleDefinition> rules = loader.getRuleDefinitions();
        assertThat(rules).hasSize(3);

        RuleDefinition logTypeRule = rules.get(0);
        assertThat(logTypeRule.getName()).isEqualTo("error-log-rule");
        assertThat(logTypeRule.getType()).isEqualTo(RuleType.LOG_TYPE);
        assertThat(logTypeRule.getLogLevel()).isEqualTo("ERROR");

        RuleDefinition typeRateRule = rules.get(1);
        assertThat(typeRateRule.getName()).isEqualTo("warning-rate-rule");
        assertThat(typeRateRule.getType()).isEqualTo(RuleType.TYPE_RATE);
        assertThat(typeRateRule.getLogLevel()).isEqualTo("WARNING");
        assertThat(typeRateRule.getWindowSeconds()).isEqualTo(300);
        assertThat(typeRateRule.getThreshold()).isEqualTo(10);

        RuleDefinition overallRateRule = rules.get(2);
        assertThat(overallRateRule.getName()).isEqualTo("overall-rate-rule");
        assertThat(overallRateRule.getType()).isEqualTo(RuleType.OVERALL_RATE);
        assertThat(overallRateRule.getWindowSeconds()).isEqualTo(300);
        assertThat(overallRateRule.getThreshold()).isEqualTo(50);
    }

    @Test
    void returnsEmptyListWhenConfigFileDoesNotExist() {
        String missingPath = tempDir.resolve("does-not-exist.yml").toString();

        RulesConfigLoader loader = new RulesConfigLoader(missingPath);
        loader.load(); // should not throw

        assertThat(loader.getRuleDefinitions()).isEmpty();
    }

    @Test
    void returnsEmptyListWhenRulesKeyIsMissing() throws IOException {
        Path rulesFile = writeTempRulesFile("some_unrelated_key: 123");

        RulesConfigLoader loader = new RulesConfigLoader(rulesFile.toString());
        loader.load();

        assertThat(loader.getRuleDefinitions()).isEmpty();
    }

    private Path writeTempRulesFile(String content) throws IOException {
        Path file = tempDir.resolve("rules.yml");
        Files.writeString(file, content);
        return file;
    }
}