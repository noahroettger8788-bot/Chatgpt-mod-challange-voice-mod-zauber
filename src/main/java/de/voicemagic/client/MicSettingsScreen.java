package de.voicemagic.client;

import de.voicemagic.VoiceMagicClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;

public class MicSettingsScreen extends Screen {
    private final Screen parent;
    private final List<String> microphones = new ArrayList<>();
    private int selected = -1;

    public MicSettingsScreen(Screen parent) {
        super(Text.literal("Voice Magic – Mikrofon"));
        this.parent = parent;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            microphones.add(info.getName());
        }
        selected = microphones.indexOf(MicConfig.mixerName);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(toggleText(), b -> {
            MicConfig.enabled = !MicConfig.enabled;
            b.setMessage(toggleText());
            MicConfig.save();
        }).dimensions(cx - 100, 55, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Mikrofon wechseln"), b -> {
            if (!microphones.isEmpty()) {
                selected = (selected + 1) % microphones.size();
                MicConfig.mixerName = microphones.get(selected);
                MicConfig.save();
            }
        }).dimensions(cx - 100, 82, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Speichern & schließen"), b -> {
            MicConfig.save();
            close();
        }).dimensions(cx - 100, 145, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> close())
                .dimensions(cx - 100, 172, 200, 20).build());
    }

    private Text toggleText() {
        return Text.literal("Mikrofon-Erkennung: " + (MicConfig.enabled ? "AN" : "AUS"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 25, 0xFFFFFF);
        String device = MicConfig.mixerName.isBlank() ? "System-Standardmikrofon" : MicConfig.mixerName;
        context.drawCenteredTextWithShadow(textRenderer, "Aktuelles Gerät: " + device, width / 2, 115, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, "Taste K öffnet diese Einstellungen", width / 2, height - 22, 0x777777);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
