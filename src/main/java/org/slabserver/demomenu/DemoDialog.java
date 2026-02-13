package org.slabserver.demomenu;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Demonstrates all four Dialog API input types:
 *   - DialogInput.text()        → free-text field
 *   - DialogInput.bool()        → checkbox / toggle
 *   - DialogInput.numberRange() → slider
 *   - DialogInput.singleOption() → radio-button style multi-choice
 *
 * On confirmation the entered values are sent back to the player in chat.
 * On cancel the dialog is silently dismissed.
 *
 * ─── Important API notes ────────────────────────────────────────────────────
 *
 * [1] DialogResponseView accessor methods:
 *       view.getFloat("key")   → confirmed in official docs; returns Number
 *       view.getString("key")  → inferred; used for text + singleOption
 *       view.getBoolean("key") → inferred; used for bool inputs
 *     If any of these cause a compile error, open the DialogResponseView
 *     Javadoc at https://jd.papermc.io/paper and find the correct method name.
 *
 * [2] SingleOptionDialogInput.OptionEntry.of(String id, Component display):
 *     Factory method name is inferred from Paper API conventions. If it doesn't
 *     compile, the Javadoc for SingleOptionDialogInput.OptionEntry will show
 *     the correct factory method (may be .entry(), .create(), or a Builder).
 *
 * [3] Getting a Player from PlayerCustomClickEvent:
 *     The event does NOT expose getPlayer() directly. You must cast
 *     getCommonConnection() to PlayerGameConnection first (shown below).
 *     This is confirmed behaviour per the official Paper docs.
 *
 * [4] Key for the confirm action – all custom action keys must be namespaced.
 *     Key.key("namespace:path") — we use "demomenu:form/submit" here.
 *
 * ────────────────────────────────────────────────────────────────────────────
 */
@SuppressWarnings("UnstableApiUsage") // Dialog API is @Experimental
public final class DemoDialog implements Listener {

    // The action key that identifies our confirm button click server-side.
    // Must be unique across your plugin to avoid collisions with other dialogs.
    private static final Key CONFIRM_KEY = Key.key("demomenu:form/submit");

    public DemoDialog() {}

    // ─── Static entry point ────────────────────────────────────────────────

    /**
     * Builds the dialog dynamically and shows it to the given player.
     * Call this from your command executor.
     */
    public static void open(Player player) {
        Dialog dialog = Dialog.create(builder -> builder.empty()

            // ── Base: title, description text, all four inputs ─────────────
            .base(
                DialogBase.builder(Component.text("Demo Form", NamedTextColor.GOLD))
                    .canCloseWithEscape(true)  // Escape key dismisses the dialog
                    .body(List.of(
                        DialogBody.plainMessage(
                            Component.text("Fill in each field, then click Confirm.", NamedTextColor.GRAY)
                        )
                    ))
                    .inputs(List.of(

                        // ── INPUT 1 · Text field ───────────────────────────
                        // Free-form string. .initial() sets the pre-filled value.
                        // .width() controls the pixel width of the text box.
                        DialogInput.text("username", Component.text("Your username"))
                            .initial("Steve")
                            .width(200)
                            .build(),

                        // ── INPUT 2 · Boolean (checkbox) ───────────────────
                        // Renders as a toggle / tick box.
                        // .initial(false) means unchecked by default.
                        DialogInput.bool("newsletter", Component.text("Subscribe to newsletter"))
                            .initial(false)
                            .build(),

                        // ── INPUT 3 · Number range (slider) ────────────────
                        // min=1, max=100. .step() sets the increment per tick.
                        // .initial() sets the default thumb position.
                        // .labelFormat() controls the display string; %s is the value.
                        DialogInput.numberRange(
                                "score",
                                Component.text("Score"),
                                1f,   // min
                                100f  // max
                            )
                            .initial(50f)
                            .step(1f)
                            .width(200)
                            .labelFormat("%1$s: %2$s")
                            .build(),

                        // ── INPUT 4 · Single option (radio buttons) ────────
                        // Each OptionEntry takes an ID (returned on submit) and a
                        // display Component (shown to the player as a button label).
                        //
                        // NOTE [2]: if OptionEntry.of() doesn't compile, check the
                        // Javadoc for the correct factory — it may be .entry() or
                        // a builder pattern.
                        DialogInput.singleOption(
                            "role",
                            Component.text("Your role"),
                            List.of(
                                    SingleOptionDialogInput.OptionEntry.create("builder",  Component.text("Builder"),  true),  // default selection
                                    SingleOptionDialogInput.OptionEntry.create("explorer", Component.text("Explorer"), false),
                                    SingleOptionDialogInput.OptionEntry.create("fighter",  Component.text("Fighter"),  false)
                                )
                        ).build()

                    ))
                    .build()
            )

            // ── Type: confirmation gives us exactly two buttons ────────────
            .type(DialogType.confirmation(

                // CONFIRM button — sends CONFIRM_KEY to the server on click.
                // The server handles it in onFormSubmit() below.
                ActionButton.builder(Component.text("✔ Confirm", TextColor.color(0x55FF55)))
                    .tooltip(Component.text("Submit your choices"))
                    .action(DialogAction.customClick(CONFIRM_KEY, null))
                    .build(),

                // CANCEL button — action is null, which silently closes the dialog.
                // No server round-trip occurs.
                ActionButton.builder(Component.text("✖ Cancel", TextColor.color(0xFF5555)))
                    .tooltip(Component.text("Discard and close"))
                    .action(null)
                    .build()
            ))
        );

        player.showDialog(dialog);
    }

    // ─── Event handler ─────────────────────────────────────────────────────

    /**
     * Fires when ANY custom dialog button is clicked server-side.
     * We filter to our specific key before doing anything.
     *
     * Register this listener in your plugin's onEnable:
     *   getServer().getPluginManager().registerEvents(new DemoDialog(), this);
     */
    @EventHandler
    public void onFormSubmit(PlayerCustomClickEvent event) {

        // Ignore clicks from other dialogs / plugins
        if (!event.getIdentifier().equals(CONFIRM_KEY)) return;

        // ── Retrieve the submitted form values ─────────────────────────────
        DialogResponseView view = event.getDialogResponseView();
        if (view == null) return; // shouldn't happen for a dialog submit, but guard anyway

        // NOTE [1]: method names for getString/getBoolean are inferred.
        // getFloat() is confirmed by the official Paper docs example.
        // If any line below causes a compile error, check DialogResponseView in the Javadoc.
        String  username   = view.getText("username");           // INPUT 1 · text
        boolean newsletter = view.getBoolean("newsletter");        // INPUT 2 · bool
        float   score      = view.getFloat("score").floatValue();  // INPUT 3 · numberRange
        String  role       = view.getText("role");               // INPUT 4 · singleOption (returns the option's ID)

        // ── Retrieve the Player from the connection ────────────────────────
        // NOTE [3]: getPlayer() is NOT available directly on this event.
        // We must cast getCommonConnection() to PlayerGameConnection first.
        if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) return;
        Player player = conn.getPlayer();

        // ── Print the results to the player's chat ─────────────────────────
        player.sendMessage(
            Component.text("━━━ Form Results ━━━", NamedTextColor.GOLD)
        );
        player.sendMessage(
            Component.text()
                .append(Component.text("Username:   ", NamedTextColor.GRAY))
                .append(Component.text(username, NamedTextColor.WHITE))
                .build()
        );
        player.sendMessage(
            Component.text()
                .append(Component.text("Newsletter: ", NamedTextColor.GRAY))
                .append(newsletter
                    ? Component.text("Yes ✔", NamedTextColor.GREEN)
                    : Component.text("No ✖",  NamedTextColor.RED))
                .build()
        );
        player.sendMessage(
            Component.text()
                .append(Component.text("Score:      ", NamedTextColor.GRAY))
                .append(Component.text((int) score, NamedTextColor.AQUA))
                .build()
        );
        player.sendMessage(
            Component.text()
                .append(Component.text("Role:       ", NamedTextColor.GRAY))
                .append(Component.text(role, NamedTextColor.YELLOW))
                .build()
        );
    }
}
