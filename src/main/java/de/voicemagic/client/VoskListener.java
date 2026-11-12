package de.voicemagic.client;

import de.voicemagic.VoiceMagicClient;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoskListener {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, Long> lastCast = new ConcurrentHashMap<>();
    private static final Pattern TEXT = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");
    private static final Map<String, String> WORDS = Map.ofEntries(
            Map.entry("digga", "digga"),
            Map.entry("creeper", "creeper"), Map.entry("zisch", "creeper"),
            Map.entry("hilfe", "hilfe"), Map.entry("aua", "hilfe"),
            Map.entry("scheiße", "curse"), Map.entry("scheisse", "curse"),
            Map.entry("verdammt", "curse"), Map.entry("mist", "curse"),
            Map.entry("nein", "launch"), Map.entry("stopp", "launch"),
            Map.entry("heiß", "fire"), Map.entry("heiss", "fire"),
            Map.entry("feuer", "fire"), Map.entry("lava", "fire"),
            Map.entry("dunkel", "dark"), Map.entry("nacht", "dark"),
            Map.entry("hunger", "hunger"),
            Map.entry("bruder", "bro"), Map.entry("bro", "bro"),
            Map.entry("speed", "speed"), Map.entry("schnell", "speed"),
            Map.entry("geschwindigkeit", "speed"),
            Map.entry("heilen", "heal"), Map.entry("leben", "heal"),
            Map.entry("wasser", "water"),
            Map.entry("licht", "light"), Map.entry("sonne", "light"),
            Map.entry("unsichtbar", "invisible"), Map.entry("geist", "invisible")
    );

    public void start() {
        Thread thread = new Thread(this::run, "VoiceMagic-Microphone");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() { running.set(false); }

    private void run() {
        Model model = null;
        try {
            Path modelPath = Path.of("config", "voicemagic", "vosk-model-small-de-0.15");
            while (running.get()) {
                // Do not open the microphone at all until the player explicitly enables recognition.
                if (!MicConfig.enabled) {
                    Thread.sleep(300);
                    continue;
                }
                if (!modelPath.toFile().isDirectory()) {
                    System.err.println("[VoiceMagic] Vosk-Modell fehlt: " + modelPath.toAbsolutePath());
                    Thread.sleep(2000);
                    continue;
                }
                TargetDataLine line = null;
                try {
                    if (model == null) model = new Model(modelPath.toString());
                    AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    Mixer mixer = findMixer(MicConfig.mixerName, info);
                    line = (TargetDataLine) (mixer == null ? AudioSystem.getLine(info) : mixer.getLine(info));
                    line.open(format);
                    line.start();

                    try (Recognizer recognizer = new Recognizer(model, 16000.0f)) {
                        byte[] buffer = new byte[4096];
                        while (running.get() && MicConfig.enabled) {
                            int count = line.read(buffer, 0, buffer.length);
                            if (count <= 0) continue;
                            if (recognizer.acceptWaveForm(buffer, count)) {
                                Matcher matcher = TEXT.matcher(recognizer.getResult());
                                if (matcher.find()) {
                                    String phrase = matcher.group(1).toLowerCase(Locale.GERMAN).trim();
                                    for (String token : phrase.split("\\s+")) {
                                        String id = WORDS.get(token);
                                        if (id != null) {
                                            castIfReady(id, token);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[VoiceMagic] Mikrofon/Spracherkennung: " + e.getMessage());
                    Thread.sleep(1000);
                } finally {
                    if (line != null) { line.stop(); line.close(); }
                }
                // When recognition is disabled, the loop closes the device. Enabling it again reopens it.
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (model != null) model.close();
        }
    }

    private void castIfReady(String id, String word) {
        long now = System.currentTimeMillis();
        int cooldown = switch (id) {
            case "bro" -> MicConfig.cooldownBro;
            case "speed" -> MicConfig.cooldownSpeed;
            case "heal" -> MicConfig.cooldownHeal;
            case "water" -> MicConfig.cooldownWater;
            case "light" -> MicConfig.cooldownLight;
            case "invisible" -> MicConfig.cooldownInvisible;
            default -> 0;
        };
        long previous = lastCast.getOrDefault(id, 0L);
        if (cooldown > 0 && now - previous < cooldown * 1000L) return;
        lastCast.put(id, now);
        VoiceMagicClient.cast(id, word);
    }

    private Mixer findMixer(String wanted, DataLine.Info info) {
        if (wanted == null || wanted.isBlank()) return null;
        for (Mixer.Info candidate : AudioSystem.getMixerInfo()) {
            if (candidate.getName().equals(wanted)) {
                Mixer mixer = AudioSystem.getMixer(candidate);
                if (mixer.isLineSupported(info)) return mixer;
            }
        }
        return null;
    }
}
