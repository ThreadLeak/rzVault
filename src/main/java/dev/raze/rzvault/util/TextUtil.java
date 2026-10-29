/*
 * rzVault modifications Copyright (c) 2026 Raze.
 * Derived from Vault; see NOTICE.md for upstream attribution.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.raze.rzvault.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final LegacyComponentSerializer COLORS =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final Pattern TOKENS = Pattern.compile("%[a-z_-]+%");

    private TextUtil() {}

    public static Component render(String template, String prefix, Map<String, String> values) {
        String expanded =
                TOKENS.matcher(template.replace("%prefix%", prefix))
                        .replaceAll(
                                match ->
                                        Matcher.quoteReplacement(
                                                values.getOrDefault(
                                                        match.group()
                                                                .substring(
                                                                        1,
                                                                        match.group().length() - 1),
                                                        match.group())));
        return COLORS.deserialize(expanded.replace('\u00a7', '&').replace("\\n", "\n"));
    }
}
