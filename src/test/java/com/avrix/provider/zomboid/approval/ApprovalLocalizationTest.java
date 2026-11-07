package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests language discovery and message bundle extensibility.
 */
class ApprovalLocalizationTest {

    @Test
    void shouldDiscoverEveryShippedLanguage() {
        List<String> codes = ApprovalLanguages.available().stream()
                .map(ApprovalLanguage::code)
                .toList();

        assertThat(codes).contains("en", "ru", "de", "fr", "es", "it", "pt", "zh", "ja", "ko", "ar");
        assertThat(codes.get(0)).isEqualTo("en");
    }

    @Test
    void shouldDetectLanguageFromLocale() {
        assertThat(ApprovalLanguages.detect(Locale.of("ru", "RU")).code()).isEqualTo("ru");
        assertThat(ApprovalLanguages.detect(Locale.of("pt", "BR")).code()).isEqualTo("pt");
        assertThat(ApprovalLanguages.detect(Locale.of("zh", "CN")).code()).isEqualTo("zh");
        assertThat(ApprovalLanguages.detect(Locale.of("ja", "JP")).code()).isEqualTo("ja");
    }

    @Test
    void shouldFallBackToEnglishForUnsupportedLocale() {
        assertThat(ApprovalLanguages.detect(Locale.of("xx")).code()).isEqualTo("en");
        assertThat(ApprovalLanguages.detect(null).code()).isEqualTo("en");
    }

    @Test
    void shouldDeclareNativeNameForEveryLanguage() {
        String englishName = ApprovalLanguages.defaultLanguage().displayName();
        for (ApprovalLanguage language : ApprovalLanguages.available()) {
            if (!"en".equals(language.code())) {
                assertThat(language.displayName())
                        .as(language.code())
                        .isNotBlank()
                        .isNotEqualTo(englishName);
            }
        }
    }

    @Test
    void shouldResolveEveryMessageInEveryLanguage() {
        for (ApprovalLanguage language : ApprovalLanguages.available()) {
            ApprovalMessages messages = new ApprovalMessages(language);
            for (ApprovalMessage message : ApprovalMessage.values()) {
                assertThat(messages.get(message))
                        .as("%s / %s", language.code(), message.key())
                        .isNotBlank();
            }
            assertThat(messages.format(ApprovalMessage.LABEL_SHA256, "abc"))
                    .as(language.code())
                    .contains("abc");
        }
    }

    @Test
    void shouldProvideBackgroundInspectionPlaceholderInEveryLanguage() throws IOException {
        String key = ApprovalMessage.DETAIL_INSPECTION_LOADING.key();
        for (ApprovalLanguage language : ApprovalLanguages.available()) {
            String suffix = "en".equals(language.code()) ? "" : "_" + language.code();
            String resource = "com/avrix/provider/zomboid/approval/translations/messages"
                    + suffix + ".properties";
            Properties properties = new Properties();
            try (InputStream input = ApprovalLocalizationTest.class.getClassLoader().getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            }
            assertThat(properties.getProperty(key))
                    .as("%s / %s", language.code(), key)
                    .isNotBlank();
        }
    }

    @Test
    void shouldSwitchLanguageAtRuntime() {
        ApprovalMessages messages = new ApprovalMessages(ApprovalLanguages.detect(Locale.ENGLISH));
        String english = messages.get(ApprovalMessage.BUTTON_APPLY);

        ApprovalLanguage russian = ApprovalLanguages.detect(Locale.of("ru", "RU"));
        messages.setLanguage(russian);

        assertThat(messages.language()).isEqualTo(russian);
        assertThat(messages.get(ApprovalMessage.BUTTON_APPLY)).isNotEqualTo(english);
    }
}
