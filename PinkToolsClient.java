package dev.alek.pinktools;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PinkToolsClient implements ClientModInitializer {
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pinktools.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_DOWN,
                "category.pinktools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new PinkToolsScreen());
                }
            }
            HitHighlight.tick(client);
        });

        AttackEntityCallback.EVENT.register(HitHighlight::onAttack);
        WorldRenderEvents.LAST.register(TrajectoryRenderer::render);
    }
}
