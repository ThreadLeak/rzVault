package dev.raze.rzvault.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TextUtilTest {
    String plain(Component value) { return PlainTextComponentSerializer.plainText().serialize(value); }
    @Test void supportsPrefixHexLegacyColorsSectionColorsAndLiteralNewlines() {
        var value = TextUtil.render("%prefix%&#00D8FFName &aReady\u00a7f\\nNext", "&6Prefix ", Map.of());
        assertEquals("Prefix Name Ready\nNext", plain(value));
    }
    @Test void substitutedValuesAreNotRecursivelyInterpretedAsOtherTokens() {
        var value = TextUtil.render("%provider% %state% %unknown%", "", Map.of("provider", "%state%", "state", "$ready\\done"));
        assertEquals("%state% $ready\\done %unknown%", plain(value));
    }
    @Test void emptyTextStaysEmptyAndPrefixIsOnlyAddedWhenRequested() {
        assertEquals("", plain(TextUtil.render("", "Prefix", Map.of())));
        assertEquals("Text", plain(TextUtil.render("Text", "Prefix", Map.of())));
    }
}
