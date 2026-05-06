package com.compute.rental.common.i18n;

import com.compute.rental.common.enums.DocLanguage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LanguageResolver {

    public static final String DEFAULT_LANGUAGE = DocLanguage.DEFAULT_VALUE;
    public static final String EN_US = "en-US";

    public String resolve(String language, String acceptLanguage) {
        var explicitLanguage = normalize(language);
        if (explicitLanguage != null) {
            return explicitLanguage;
        }
        if (StringUtils.hasText(acceptLanguage)) {
            for (var candidate : acceptLanguage.split(",")) {
                var normalized = normalize(candidate.split(";", 2)[0]);
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return DEFAULT_LANGUAGE;
    }

    public boolean isDefaultLanguage(String language) {
        return DEFAULT_LANGUAGE.equals(language);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        var normalized = value.trim().replace('_', '-').toLowerCase();
        if (normalized.equals("zh") || normalized.startsWith("zh-")) {
            return DEFAULT_LANGUAGE;
        }
        if (normalized.equals("en") || normalized.startsWith("en-")) {
            return EN_US;
        }
        return null;
    }
}
