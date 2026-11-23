package com.headqlink.link;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class C10SensorsPanelMigrationTest {
    @Test public void replacesOldGamesAndKaraokeWithSensorsAtFirstPosition() {
        List<String> ids = Config.panelButtonsFrom("car,games,radio,karaoke,web");
        assertEquals(Arrays.asList("car","sensors","radio","web"), ids);
        assertFalse(ids.contains("games"));
        assertFalse(ids.contains("karaoke"));
    }

    @Test public void defaultPanelHasSensorsButNoGames() {
        List<String> ids = Config.panelButtonsFrom(null);
        assertTrue(ids.contains("sensors"));
        assertFalse(ids.contains("games"));
        assertFalse(ids.contains("karaoke"));
    }

    @Test public void ignoresInvalidAndDuplicateNames() {
        List<String> ids = Config.panelButtonsFrom("games,karaoke,sensors,bogus,sensors");
        assertEquals(Arrays.asList("sensors"), ids);
    }
}
