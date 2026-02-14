package org.slabserver.demomenu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Static dummy data for the PlayerSync dialog API prototype.
 * No Paper API imports — pure Java records and collections.
 */
public final class DemoData {

    private DemoData() {}

    // ── Records ────────────────────────────────────────────────────────────

    public record DemoPlayer(
        String name,
        UUID   uuid,
        List<DemoSnapshot> snapshots
    ) {}

    public record DemoSnapshot(
        int    id,
        String serverName,
        String worldName,
        int    x, int y, int z,
        double health,
        int    food,
        int    xpLevel,
        int    xpPercent,
        String gamemode,
        String vehicle,
        int    inventoryCount,
        int    enderChestCount,
        long   epochSecond
    ) {
        private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(ZoneId.of("UTC"));

        /** ISO-style timestamp for display in the detail dialog body. */
        public String formattedTime() {
            return FMT.format(Instant.ofEpochSecond(epochSecond));
        }

        /** Human-readable relative age for the snapshot list button labels. */
        public String relativeTime() {
            long ago = (System.currentTimeMillis() / 1000L) - epochSecond;
            if (ago < 60)    return "just now";
            if (ago < 3600)  return (ago / 60)   + "m ago";
            if (ago < 86400) return (ago / 3600)  + "h ago";
            return                  (ago / 86400) + "d ago";
        }
    }

    // ── Generated dataset: 18 players, 4-8 snapshots each ─────────────────

    public static final List<DemoPlayer> PLAYERS = generatePlayers();

    private static List<DemoPlayer> generatePlayers() {
        String[] names = {
            // Page 0
            "AlphaWolf99",  "CrystalQueen", "DarkMatter",
            "EpicBuilder",  "FrozenLake",   "GlitchMaster",
            // Page 1
            "HeroicPanda",  "IcyArrow",     "JadeWarrior",
            "KingSlayer",   "LunaRider",    "MagicStorm",
            // Page 2
            "NightOwl",     "OceanWave",    "PixelKnight",
            "QuickSilver",  "RedDragon",    "ShadowFox"
        };

        long now = System.currentTimeMillis() / 1000L;
        var list = new ArrayList<DemoPlayer>(names.length);

        for (int i = 0; i < names.length; i++) {
            list.add(new DemoPlayer(
                names[i],
                UUID.nameUUIDFromBytes(names[i].getBytes()),
                generateSnapshots(i, now)
            ));
        }
        return Collections.unmodifiableList(list);
    }

    private static List<DemoSnapshot> generateSnapshots(int pIdx, long now) {
        String[] servers  = { "survival", "creative", "skyblock", "minigames" };
        String[] worlds   = { "overworld", "the_nether", "the_end", "skyblock_world" };
        String[] modes    = { "SURVIVAL", "CREATIVE", "ADVENTURE" };
        String[] vehicles = { "none", "none", "Horse", "Boat", "Minecart", "none", "Llama" };

        int count = 4 + (pIdx % 5); // 4–8 snapshots per player
        var snaps = new ArrayList<DemoSnapshot>(count);

        for (int i = 0; i < count; i++) {
            double healthRaw = 20.0 - (i % 5) * 2.5;

            snaps.add(new DemoSnapshot(
                1000 + pIdx * 10 + i,
                servers[(pIdx + i) % servers.length],
                worlds[(pIdx + i)  % worlds.length],
                (pIdx * 37 + i * 13) % 4000 - 2000,      // x
                60 + (i * 7) % 100,                        // y
                (pIdx * 53 + i * 17) % 4000 - 2000,      // z
                Math.max(1.0, healthRaw),                  // health (min 1)
                Math.max(0, 20 - (i % 4) * 3),            // food
                pIdx + i * 3,                              // xp level
                (pIdx * 7 + i * 19) % 100,               // xp %
                modes[(pIdx + i) % modes.length],
                vehicles[(pIdx + i) % vehicles.length],
                18 + (pIdx + i) % 18,                    // inventory slots used
                (pIdx + i) % 27,                          // ender chest slots used
                now - (i * 3600L + pIdx * 600L)          // staggered save times
            ));
        }
        return Collections.unmodifiableList(snaps);
    }
}