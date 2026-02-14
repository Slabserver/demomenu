package org.slabserver.demomenu;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point.
 *
 * Commands registered:
 *   /demomenu      — opens the four-input form demo (DemoDialog)
 *   /psync-demo    — opens the paginated player list (DemoMenuSystem)
 *
 * Listeners registered:
 *   DemoDialog      — handles form submission
 *   DemoMenuSystem  — handles all psync:list/player/snapshot/restore navigation
 */
@SuppressWarnings("UnstableApiUsage")
public class MenuDemoPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        // ── Register listeners ─────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new DemoDialog(),     this);
        getServer().getPluginManager().registerEvents(new DemoMenuSystem(), this);

        // ── Register commands ──────────────────────────────────────────────
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {

            // /demomenu — opens the four-input form
            event.registrar().register(
                Commands.literal("demomenu")
                    .requires(src -> src.getSender() instanceof Player)
                    .executes(ctx -> {
                        DemoDialog.open((Player) ctx.getSource().getSender());
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "Opens the Dialog API input-types demo form"
            );

            // /psync-demo — opens the player list browser at page 0
            event.registrar().register(
                Commands.literal("slabsync-demo")
                    .requires(src -> src.getSender() instanceof Player)
                    .executes(ctx -> {
                        DemoMenuSystem.showPlayerList((Player) ctx.getSource().getSender(), 0);
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "Opens the SlabSync data snapshot browser demo"
            );
        });
    }
}