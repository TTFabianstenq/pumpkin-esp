package dev.pumpkinesp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PumpkinEspMod implements ClientModInitializer {
    public static final String MOD_ID = "pumpkin-esp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean enabled = true;
    public static double range = 250.0;
    public static boolean tracers = true;
    public static boolean boxes = true;

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pumpkin-esp.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.pumpkin-esp"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.displayClientMessage(
                            Component.literal(
                                    "\u00a76[Pumpkin ESP] \u00a7f" + (enabled ? "\u00a7aON" : "\u00a7cOFF")
                                            + " \u00a77(range " + (int) range + ", through walls)"
                            ),
                            true
                    );
                }
            }
            if (enabled && client.player != null && client.level != null) {
                PumpkinScanner.tick(client);
            }
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(PumpkinRenderer::render);
        LOGGER.info("Pumpkin ESP 1.21.11 — range {} through-walls, toggle P", (int) range);
    }
}
