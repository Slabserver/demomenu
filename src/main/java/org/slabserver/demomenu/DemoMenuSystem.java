package org.slabserver.demomenu;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * SlabSync prototype — full Dialog API navigation demo.
 *
 * Navigation tree:
 *
 *   /psync-demo
 *       └─▶ Player List (page 0)      key: psync:list/{page}
 *               └─▶ Snapshot List     key: psync:player/{playerIdx}
 *                       └─▶ Snapshot Detail   key: psync:snapshot/{pIdx}/{sIdx}
 *                               └─▶ [Restore preview]  key: psync:restore/{pIdx}/{sIdx}
 *
 * All navigation state is encoded in the Key path.
 * No server-side session maps are used.
 *
 * Key namespace: "psync"
 * Key paths:
 *   list/{page}          → open player list at given page (0-2)
 *   player/{idx}         → open snapshot list for player at index
 *   snapshot/{p}/{s}     → open snapshot detail for player p, snapshot s
 *   restore/{p}/{s}      → trigger restore preview notice
 */
@SuppressWarnings("UnstableApiUsage")
public final class DemoMenuSystem implements Listener {

    // ─── Namespace ────────────────────────────────────────────────────────

    private static final String NS = "psync";

    private static Key listKey(int page)                      { return Key.key(NS, "list/"     + page);           }
    private static Key playerKey(int playerIdx)               { return Key.key(NS, "player/"   + playerIdx);      }
    private static Key snapshotKey(int pIdx, int sIdx)        { return Key.key(NS, "snapshot/" + pIdx + "/" + sIdx); }
    private static Key restoreKey(int pIdx, int sIdx)         { return Key.key(NS, "restore/"  + pIdx + "/" + sIdx); }

    // ─── Color palette ────────────────────────────────────────────────────

    private static final TextColor GOLD        = TextColor.color(0xFFAA00);
    private static final TextColor AQUA        = TextColor.color(0x55FFFF);
    private static final TextColor GREEN        = TextColor.color(0x55FF55);
    private static final TextColor RED          = TextColor.color(0xFF5555);
    private static final TextColor YELLOW       = TextColor.color(0xFFFF55);
    private static final TextColor GRAY         = TextColor.color(0xAAAAAA);
    private static final TextColor DARK_GRAY    = TextColor.color(0x555555);
    private static final TextColor WHITE        = TextColor.color(0xFFFFFF);
    private static final TextColor PURPLE       = TextColor.color(0xAA00AA);

    // ─── Event dispatch ───────────────────────────────────────────────────

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) return;
        Player viewer = conn.getPlayer();

        String path = event.getIdentifier().namespace().equals(NS)
            ? event.getIdentifier().value()
            : null;
        if (path == null) return;

        String[] parts = path.split("/");
        try {
            switch (parts[0]) {
                case "list"     -> showPlayerList(viewer, Integer.parseInt(parts[1]));
                case "player"   -> showSnapshotList(viewer, Integer.parseInt(parts[1]));
                case "snapshot" -> showSnapshotDetail(viewer, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "restore"  -> showRestorePreview(viewer, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            // Malformed key — ignore
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN 1 — Player List (paginated, 6 players per page, 3 pages)
    // ═══════════════════════════════════════════════════════════════════════

    public static void showPlayerList(Player viewer, int page) {
        viewer.showDialog(buildPlayerList(page));
    }

    private static Dialog buildPlayerList(int page) {
        final int PAGE_SIZE  = 6;
        final int TOTAL      = DemoData.PLAYERS.size();   // 18
        final int TOTAL_PAGES = (TOTAL + PAGE_SIZE - 1) / PAGE_SIZE; // 3

        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, TOTAL);
        List<DemoData.DemoPlayer> pagePlayers = DemoData.PLAYERS.subList(from, to);

        List<ActionButton> buttons = new ArrayList<>();

        // ── One button per player ──────────────────────────────────────────
        for (int i = 0; i < pagePlayers.size(); i++) {
            int playerIdx = from + i;
            DemoData.DemoPlayer p = pagePlayers.get(i);
            DemoData.DemoSnapshot latest = p.snapshots().get(0);

            Component label = Component.text()
                .append(Component.text("● ", GREEN))
                .append(Component.text(p.name(), AQUA).decorate(TextDecoration.BOLD))
                .append(Component.text("  —  " + p.snapshots().size() + " snapshots", GRAY))
                .build();

            Component tooltip = Component.join(JoinConfiguration.newlines(),
                Component.text("UUID:       ", GRAY).append(Component.text(p.uuid().toString(), DARK_GRAY)),
                Component.text("Last saved: ", GRAY).append(Component.text(latest.relativeTime(), YELLOW)),
                Component.text("Server:     ", GRAY).append(Component.text(latest.serverName(), WHITE)),
                Component.text("Snapshots:  ", GRAY).append(Component.text(p.snapshots().size() + " stored", GREEN)),
                Component.empty(),
                Component.text("Click to view snapshot history →", AQUA)
            );

            // Width 300 forces one player per row in multiAction layout
            buttons.add(ActionButton.create(label, tooltip, 300, DialogAction.customClick(playerKey(playerIdx), null)));
        }

        // ── Pagination row ─────────────────────────────────────────────────
        // Previous and Next sit side-by-side (width 130 each ≈ half the dialog)
        if (page > 0) {
            buttons.add(ActionButton.create(
                Component.text("← Previous", GRAY),
                Component.text("Page " + page + " of " + TOTAL_PAGES, GRAY),
                130,
                DialogAction.customClick(listKey(page - 1), null)
            ));
        }
        if (page < TOTAL_PAGES - 1) {
            buttons.add(ActionButton.create(
                Component.text("Next →", GRAY),
                Component.text("Page " + (page + 2) + " of " + TOTAL_PAGES, GRAY),
                130,
                DialogAction.customClick(listKey(page + 1), null)
            ));
        }

        return Dialog.create(b -> b.empty()
            .base(DialogBase.builder(
                    Component.text("SlabSync  —  Player List", GOLD).decorate(TextDecoration.BOLD)
                )
                .body(List.of(
                    DialogBody.plainMessage(
                        Component.text("Page " + (page + 1) + " of " + TOTAL_PAGES, GRAY)
                            .append(Component.text("  ·  ", DARK_GRAY))
                            .append(Component.text(TOTAL + " players tracked", GRAY))
                    )
                ))
                .build()
            )
            .type(DialogType.multiAction(buttons).build())
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN 2 — Snapshot List (all snapshots for one player)
    // ═══════════════════════════════════════════════════════════════════════

    public static void showSnapshotList(Player viewer, int playerIdx) {
        viewer.showDialog(buildSnapshotList(playerIdx));
    }

    private static Dialog buildSnapshotList(int playerIdx) {
        DemoData.DemoPlayer p = DemoData.PLAYERS.get(playerIdx);
        List<DemoData.DemoSnapshot> snapshots = p.snapshots();

        List<ActionButton> buttons = new ArrayList<>();

        // ── One button per snapshot ────────────────────────────────────────
        for (int i = 0; i < snapshots.size(); i++) {
            DemoData.DemoSnapshot s = snapshots.get(i);

            // Label: "#1003  ·  2h ago  ·  survival / overworld"
            Component label = Component.text()
                .append(Component.text("#" + s.id(), DARK_GRAY))
                .append(Component.text("  ·  ", DARK_GRAY))
                .append(Component.text(s.relativeTime(), YELLOW))
                .append(Component.text("  ·  ", DARK_GRAY))
                .append(Component.text(s.serverName(), AQUA))
                .append(Component.text(" / " + s.worldName(), GRAY))
                .build();

            // Tooltip: compact stats summary
            TextColor healthColor = s.health() >= 15 ? GREEN : (s.health() >= 8 ? YELLOW : RED);
            Component tooltip = Component.join(JoinConfiguration.newlines(),
                Component.text(s.formattedTime(), GRAY),
                Component.empty(),
                Component.text("Health:  ", GRAY).append(Component.text(formatHealth(s.health()) + "/20 ❤", healthColor)),
                Component.text("Food:    ", GRAY).append(Component.text(s.food() + "/20", YELLOW)),
                Component.text("XP:      ", GRAY).append(Component.text("Level " + s.xpLevel() + "  (" + s.xpPercent() + "%)", GREEN)),
                Component.text("Mode:    ", GRAY).append(Component.text(s.gamemode(), WHITE)),
                Component.text("Location:", GRAY).append(Component.text(
                    " " + s.x() + ", " + s.y() + ", " + s.z(), WHITE)),
                s.vehicle().equals("none") ? Component.empty()
                    : Component.text("Riding:  ", GRAY).append(Component.text(s.vehicle(), PURPLE)),
                Component.empty(),
                Component.text("Click to view full details →", AQUA)
            );

            buttons.add(ActionButton.create(label, tooltip, 300,
                DialogAction.customClick(snapshotKey(playerIdx, i), null)));
        }

        // ── Back button ────────────────────────────────────────────────────
        int originPage = playerIdx / 6;
        buttons.add(ActionButton.create(
            Component.text("← Back to player list", GRAY),
            Component.text("Return to page " + (originPage + 1), GRAY),
            300,
            DialogAction.customClick(listKey(originPage), null)
        ));

        return Dialog.create(b -> b.empty()
            .base(DialogBase.builder(
                    Component.text(p.name() + "  —  Snapshots", AQUA).decorate(TextDecoration.BOLD)
                )
                .body(List.of(
                    DialogBody.plainMessage(
                        Component.text("UUID:  ", GRAY)
                            .append(Component.text(p.uuid().toString(), DARK_GRAY))
                    ),
                    DialogBody.plainMessage(
                        Component.text(snapshots.size() + " snapshots stored", GRAY)
                            .append(Component.text("  ·  ", DARK_GRAY))
                            .append(Component.text("Most recent first", DARK_GRAY))
                    )
                ))
                .build()
            )
            .type(DialogType.multiAction(buttons).build())
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN 3 — Snapshot Detail
    // ═══════════════════════════════════════════════════════════════════════

    public static void showSnapshotDetail(Player viewer, int playerIdx, int snapshotIdx) {
        viewer.showDialog(buildSnapshotDetail(playerIdx, snapshotIdx));
    }

    private static Dialog buildSnapshotDetail(int playerIdx, int snapshotIdx) {
        DemoData.DemoPlayer p  = DemoData.PLAYERS.get(playerIdx);
        DemoData.DemoSnapshot s = p.snapshots().get(snapshotIdx);

        TextColor healthColor = s.health() >= 15 ? GREEN : (s.health() >= 8 ? YELLOW : RED);

        // ── Body: stat lines, mirroring the architecture doc layout ────────
        List<DialogBody> body = List.of(

            // Header row
            line(
                label("Snapshot: "), value("#" + s.id()),
                sep(), label("Saved: "), value(s.formattedTime())
            ),
            line(
                label("Server:   "), value(s.serverName()),
                sep(), label("World: "), value(s.worldName())
            ),
            line(
                label("Location: "),
                value("X:" + s.x() + "  Y:" + s.y() + "  Z:" + s.z())
            ),

            divider(),

            // Stats row 1
            line(
                label("Health:   "), Component.text(formatHealth(s.health()) + " / 20 ❤", healthColor),
                sep(), label("Food:  "), Component.text(s.food() + " / 20", YELLOW)
            ),
            // Stats row 2
            line(
                label("XP Level: "), value(String.valueOf(s.xpLevel())),
                sep(), label("XP: "), value(s.xpPercent() + "%")
            ),
            // Stats row 3
            line(
                label("Gamemode: "), value(s.gamemode()),
                sep(), label("Riding: "),
                    value(s.vehicle().equals("none") ? "—" : s.vehicle())
            ),

            divider(),

            // Inventory row
            line(
                label("Inventory:    "), value(s.inventoryCount() + " / 36 slots used"),
                sep(), label("Ender Chest: "), value(s.enderChestCount() + " / 27 slots used")
            )
        );

        // ── Action buttons ─────────────────────────────────────────────────
        List<ActionButton> buttons = List.of(

            ActionButton.create(
                Component.text("⚠ Restore this snapshot", TextColor.color(0xFF5555)).decorate(TextDecoration.BOLD),
                Component.join(JoinConfiguration.newlines(),
                    Component.text("Overwrite " + p.name() + "'s current state", RED),
                    Component.text("with snapshot #" + s.id() + " from " + s.serverName(), GRAY),
                    Component.empty(),
                    Component.text("This action cannot be undone.", RED)
                ),
                300,
                DialogAction.customClick(restoreKey(playerIdx, snapshotIdx), null)
            ),

            ActionButton.create(
                Component.text("← Back to snapshots", GRAY),
                Component.text("Return to " + p.name() + "'s snapshot list", GRAY),
                200,
                DialogAction.customClick(playerKey(playerIdx), null)
            ),

            ActionButton.create(
                Component.text("✖ Close", DARK_GRAY),
                Component.text("Close this dialog", DARK_GRAY),
                95,
                null  // null action = close with no server round-trip
            )
        );

        return Dialog.create(b -> b.empty()
            .base(DialogBase.builder(
                    Component.text("Snapshot Detail  —  " + p.name(), GOLD).decorate(TextDecoration.BOLD)
                )
                .canCloseWithEscape(true)
                .body(body)
                .build()
            )
            .type(DialogType.multiAction(buttons).build())
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN 4 — Restore Preview (prototype stub — shows a notice)
    // ═══════════════════════════════════════════════════════════════════════

    private static void showRestorePreview(Player viewer, int playerIdx, int snapshotIdx) {
        DemoData.DemoPlayer p  = DemoData.PLAYERS.get(playerIdx);
        DemoData.DemoSnapshot s = p.snapshots().get(snapshotIdx);

        Dialog notice = Dialog.create(b -> b.empty()
            .base(DialogBase.builder(
                    Component.text("Restore — Not Implemented", RED)
                )
                .body(List.of(
                    DialogBody.plainMessage(
                        Component.text("This is a prototype demo.", YELLOW)
                    ),
                    DialogBody.plainMessage(
                        Component.text("In production this would restore:", GRAY)
                    ),
                    DialogBody.plainMessage(Component.empty()),
                    DialogBody.plainMessage(
                        label("Player:   ").append(value(p.name()))
                    ),
                    DialogBody.plainMessage(
                        label("Snapshot: ").append(value("#" + s.id() + "  (" + s.formattedTime() + ")"))
                    ),
                    DialogBody.plainMessage(
                        label("Server:   ").append(value(s.serverName()))
                    ),
                    DialogBody.plainMessage(Component.empty()),
                    DialogBody.plainMessage(
                        Component.text("The restore flow would show a confirmation", DARK_GRAY)
                    ),
                    DialogBody.plainMessage(
                        Component.text("dialog before applying any changes.", DARK_GRAY)
                    )
                ))
                .build()
            )
            .type(DialogType.notice())
        );

        viewer.showDialog(notice);

        // Also echo to chat so the wiring is obviously working
        viewer.sendMessage(
            Component.text("[Demo] ", GOLD)
                .append(Component.text("Restore triggered: ", GRAY))
                .append(Component.text(p.name(), AQUA))
                .append(Component.text(" → snapshot #" + s.id(), YELLOW))
        );
    }

    // ─── Component helpers ─────────────────────────────────────────────────

    /** A labeled stat block: "Label: " in gray + value in white, joined inline. */
    private static DialogBody line(Component... parts) {
        Component combined = Component.empty();
        for (Component part : parts) combined = combined.append(part);
        return DialogBody.plainMessage(combined);
    }

    private static Component label(String text) {
        return Component.text(text, GRAY);
    }

    private static Component value(String text) {
        return Component.text(text, WHITE);
    }

    /** Thin separator between two inline columns. */
    private static Component sep() {
        return Component.text("  │  ", DARK_GRAY);
    }

    /** Visual divider line used between stat sections. */
    private static DialogBody divider() {
        return DialogBody.plainMessage(
            Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", DARK_GRAY)
        );
    }

    /** Format a health double as an integer-like string (e.g. 17.5 → "17.5", 20.0 → "20"). */
    private static String formatHealth(double health) {
        return health == Math.floor(health) ? String.valueOf((int) health) : String.valueOf(health);
    }
}