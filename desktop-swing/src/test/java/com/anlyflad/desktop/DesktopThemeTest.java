package com.anlyflad.desktop;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class DesktopThemeTest {
    @Test
    public void shouldUseBundledNotoSansForEveryFont() {
        String[] families={DesktopTheme.TITLE_FONT.getFamily(), DesktopTheme.DISPLAY_FONT.getFamily(), DesktopTheme.LABEL_FONT.getFamily(), DesktopTheme.BODY_FONT.getFamily(), DesktopTheme.CAPTION_FONT.getFamily()};
        for (int index=0;index<families.length;index++) {
            assertTrue(families[index].toLowerCase(Locale.ROOT).contains("noto sans"),"expected Noto Sans but was "+families[index]);
        }
    }
}
