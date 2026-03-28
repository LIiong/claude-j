package com.claudej.infrastructure.shortlink.service;

import com.claudej.domain.shortlink.model.valobj.ShortCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Base62ShortCodeGeneratorTest {

    private final Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator();

    @Test
    void should_generate6CharCode_when_idIs1() {
        ShortCode code = generator.generate(1L);
        assertThat(code.getValue()).hasSize(6);
        assertThat(code.getValue()).matches("[0-9a-zA-Z]{6}");
    }

    @Test
    void should_generateDifferentCodes_when_consecutiveIds() {
        ShortCode code1 = generator.generate(1L);
        ShortCode code2 = generator.generate(2L);
        ShortCode code3 = generator.generate(3L);

        assertThat(code1).isNotEqualTo(code2);
        assertThat(code2).isNotEqualTo(code3);
    }

    @Test
    void should_generateUniqueCodesForFirst1000Ids() {
        Set<String> codes = new HashSet<>();
        for (long id = 1; id <= 1000; id++) {
            ShortCode code = generator.generate(id);
            assertThat(codes.add(code.getValue()))
                    .as("Duplicate code at id=" + id + ": " + code.getValue())
                    .isTrue();
        }
        assertThat(codes).hasSize(1000);
    }

    @Test
    void should_handleLargeId_when_10MillionthRecord() {
        ShortCode code = generator.generate(10_000_000L);
        assertThat(code.getValue()).hasSize(6);
        assertThat(code.getValue()).matches("[0-9a-zA-Z]{6}");
    }

    @Test
    void should_produceNonSequentialCodes_when_sequentialIds() {
        // Verify obfuscation makes codes look random
        String code1 = generator.generate(1L).getValue();
        String code2 = generator.generate(2L).getValue();

        // First chars should differ (obfuscation scatters the values)
        assertThat(code1.charAt(0)).isNotEqualTo(code2.charAt(0));
    }

    @Test
    void should_obfuscateCorrectly() {
        // Verify obfuscation is deterministic
        long result1 = generator.obfuscate(1L);
        long result2 = generator.obfuscate(1L);
        assertThat(result1).isEqualTo(result2);

        // Verify different IDs produce different obfuscated values
        long result3 = generator.obfuscate(2L);
        assertThat(result1).isNotEqualTo(result3);
    }
}
