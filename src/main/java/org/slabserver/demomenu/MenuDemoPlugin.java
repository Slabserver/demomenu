package org.slabserver.demomenu;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point. Registers the /demomenu command via the Paper Brigadier API.
 * No plugin.yml command entries are needed.
 *
 * Requires: Paper 1.21.7+ (Dialog API), Java 21, paper-api 1.21.7-R0.1-SNAPSHOT
 */
@SuppressWarnings("UnstableApiUsage") // Dialog API is @Experimental
public class MenuDemoPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register the dialog event listener
        getServer().getPluginManager().registerEvents(new DemoDialog(), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register(
                Commands.literal("demomenu")
                    .requires(src -> src.getSender() instanceof Player)
                    .executes(ctx -> {
                        Player player = (Player) ctx.getSource().getSender();
                        DemoDialog.open(player);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "Opens the demo dialog with all four input types"
            )
        );
    }
}
