package dev.pumpkinesp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PumpkinEspMod implements ClientModInitializer {
    public static final String MOD_ID = "pumpkin-esp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean enabled = true;
    /** Detection + render range in blocks (default 250). */
    public static double range = 250.0;
    public static boolean tracers = true;
    public static boolean boxes = true;

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pumpkin-esp.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.pumpkin-esp"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            net.minecraft.text.Text.literal(
                                    "\u00a76[Pumpkin ESP] \u00a7f" + (enabled ? "\u00a7aON" : "\u00a7cOFF")
                                            + " \u00a77(range " + (int) range + ")"
                            ),
                            true
                    );
                }
            }
            if (enabled && client.player != null && client.world != null) {
                PumpkinScanner.tick(client);
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(PumpkinRenderer::render);

        LOGGER.info("Pumpkin ESP loaded — range {} blocks, toggle key P", (int) range);
    }
}
