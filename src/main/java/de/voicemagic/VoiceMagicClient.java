package de.voicemagic;

import de.voicemagic.client.MicConfig;
import de.voicemagic.client.MicSettingsScreen;
import de.voicemagic.client.VoskListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VoiceMagicClient implements ClientModInitializer {
    public static VoskListener listener;
    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        MicConfig.load();
        listener = new VoskListener();
        listener.start();

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voicemagic.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K,
                "category.voicemagic"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.wasPressed()) {
                client.setScreen(new MicSettingsScreen(client.currentScreen));
            }
        });
    }

    public static void cast(String spellId, String recognized) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        ClientPlayNetworking.send(VoiceMagic.CAST_PACKET,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create().writeString(spellId));
        client.execute(() -> {
            if (client.player != null) client.player.sendMessage(Text.literal("Erkannt: " + recognized), true);
        });
    }
}
