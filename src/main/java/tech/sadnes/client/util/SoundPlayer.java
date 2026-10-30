package tech.sadnes.client.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.FloatControl.Type;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.AssetCrypto;

public final class SoundPlayer {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "sadnes-sound");
        thread.setDaemon(true);
        return thread;
    });
    private static final int MAX_CLIPS = 8;
    private static final Map<String, SoundPlayer.SoundData> SOUND_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<Clip>> CLIP_POOLS = new ConcurrentHashMap<>();

    private SoundPlayer() {
    }

    static String getResourcePath(String string) {
        return "/assets/" + ClientInfo.MOD_ID + "/sound/" + string;
    }

    private static SoundPlayer.SoundData loadSoundData(String string) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(AssetCrypto.wrap(SoundPlayer.class.getResourceAsStream(getResourcePath(string))))
            )) {
            return new SoundPlayer.SoundData(audioInputStream.getFormat(), audioInputStream.readAllBytes());
        } catch (UnsupportedAudioFileException | IOException exception) {
            return null;
        }
    }

    private static Clip createClip(SoundPlayer.SoundData soundData) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(new AudioInputStream(new ByteArrayInputStream(soundData.data()), soundData.format(), soundData.data().length / soundData.format().getFrameSize()));
            return clip;
        } catch (LineUnavailableException | IOException exception) {
            return null;
        }
    }

    private static void warmUp(Clip clip) {
        FloatControl floatControl = (FloatControl)clip.getControl(Type.MASTER_GAIN);
        float f = floatControl.getValue();
        floatControl.setValue(floatControl.getMinimum());
        clip.start();

        try {
            Thread.sleep(50L);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }

        clip.stop();
        clip.setFramePosition(0);
        floatControl.setValue(f);
    }

    public static void preload(String... strings) {
        EXECUTOR.execute(() -> {
            for (String string : strings) {
                SoundPlayer.SoundData soundData = SOUND_CACHE.computeIfAbsent(string, SoundPlayer::loadSoundData);
                if (soundData != null) {
                    List list = CLIP_POOLS.computeIfAbsent(string, string2 -> new ArrayList<>());
                    synchronized (list) {
                        if (list.isEmpty()) {
                            Clip clip = createClip(soundData);
                            if (clip != null) {
                                warmUp(clip);
                                list.add(clip);
                            }
                        }
                    }
                }
            }
        });
    }

    public static void play(String string, double d) {
        EXECUTOR.execute(() -> {
            SoundPlayer.SoundData soundData = SOUND_CACHE.computeIfAbsent(string, SoundPlayer::loadSoundData);
            if (soundData != null) {
                List list = CLIP_POOLS.computeIfAbsent(string, string2 -> new ArrayList<>());
                synchronized (list) {
                    Clip clip = acquireClip(list, soundData);
                    if (clip != null) {
                        clip.stop();
                        clip.setFramePosition(0);
                        setVolume(clip, d);
                        clip.start();
                    }
                }
            }
        });
    }

    private static Clip acquireClip(List<Clip> clips, SoundPlayer.SoundData soundData) {
        int i = clips.size();

        for (int i2 = 0; i2 < i; i2++) {
            Clip clip = (Clip)clips.get(i2);
            if (!clip.isRunning()) {
                return clip;
            }
        }

        if (i < 8) {
            Clip clip2 = createClip(soundData);
            if (clip2 != null) {
                clips.add(clip2);
            }

            return clip2;
        } else {
            return (Clip)clips.get(0);
        }
    }

    public static void play(String string) {
        play(string, 0.5);
    }

    private static void setVolume(Clip clip, double d) {
        if (d < 0.0) {
            d = 0.0;
        }

        if (d > 1.0) {
            d = 1.0;
        }

        FloatControl floatControl = (FloatControl)clip.getControl(Type.MASTER_GAIN);
        float f = (float)(Math.log(d == 0.0 ? 1.0E-4 : d) / Math.log(10.0) * 20.0);
        floatControl.setValue(f);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();

        for (List list : CLIP_POOLS.values()) {
            synchronized (list) {
                Iterator iterator2 = list.iterator();

                while (iterator2.hasNext()) {
                    ((Clip)iterator2.next()).close();
                }
            }
        }

        CLIP_POOLS.clear();
        SOUND_CACHE.clear();
    }

    public static final class ConditionalTrack {
        private final SoundPlayer.Track track;
        private Supplier<Boolean> condition;
        private boolean pauseInstead;
        private boolean started;

        private ConditionalTrack(SoundPlayer.Track track2, Supplier<Boolean> supplier, boolean flag) {
            this.track = track2;
            this.condition = supplier;
            this.pauseInstead = flag;
        }

        public static SoundPlayer.ConditionalTrack create(SoundPlayer.Track track2, Supplier<Boolean> supplier, boolean flag) {
            return new SoundPlayer.ConditionalTrack(track2, supplier, flag);
        }

        public void setPauseInstead(boolean flag) {
            this.pauseInstead = flag;
        }

        public void update() {
            if (this.started && this.track.clip == null && this.condition.get()) {
                this.started = false;
            }

            if (!this.started && this.condition.get()) {
                this.track.play();
                this.started = true;
            }

            if (this.pauseInstead) {
                SoundPlayer.Track track2 = this.track;
                if (!this.condition.get()) {
                    track2.setPaused(true);
                } else {
                    track2.setPaused(false);
                }
            } else {
                if (this.track.isPlaying() != this.condition.get()) {
                    if (this.condition.get()) {
                        this.track.play();
                        return;
                    }

                    this.track.stop();
                }
            }
        }

        public boolean isPlaying() {
            return this.track.isPlaying();
        }

        public SoundPlayer.Track getTrack() {
            return this.track;
        }

        public Supplier<Boolean> getCondition() {
            return this.condition;
        }

        public boolean isPauseInstead() {
            return this.pauseInstead;
        }

        public boolean isStarted() {
            return this.started;
        }

        public void setCondition(Supplier<Boolean> supplier) {
            this.condition = supplier;
        }

        public void setPauseInsteadFlag(boolean flag) {
            this.pauseInstead = flag;
        }

        public void setStarted(boolean flag) {
            this.started = flag;
        }
    }

    record SoundData(AudioFormat format, byte[] data) {
    }

    public static final class Track {
        private final boolean looping;
        private boolean paused;
        private long pausePosition;
        private String sound;
        Clip clip;

        private Track(String string, boolean flag) {
            this.sound = string;
            this.looping = flag;
        }

        public static SoundPlayer.Track create(String string, boolean flag) {
            return new SoundPlayer.Track(string, flag);
        }

        public boolean isPlaying() {
            return this.clip != null && this.clip.isOpen() && this.clip.isRunning();
        }

        public void setSound(String string) {
            this.sound = string;
            this.stop();
            this.play();
        }

        public void setLoop(boolean flag) {
            if (this.clip != null) {
                if (flag) {
                    this.clip.loop(-1);
                } else {
                    this.clip.loop(0);
                }
            }
        }

        public boolean isLooping() {
            return this.looping && this.clip != null && this.clip.isOpen();
        }

        public void setPaused(boolean flag) {
            if (this.paused != flag && this.clip != null && this.clip.isOpen() && this.clip.getMicrosecondLength() != 0L) {
                if (flag) {
                    this.pausePosition = this.clip.getMicrosecondPosition();
                    this.clip.stop();
                    this.paused = flag;
                    return;
                }

                this.clip.setMicrosecondPosition(this.pausePosition);
                this.setVolume(this.getGain());
                this.setLoop(this.isLooping());
                this.clip.start();
                this.paused = flag;
            }
        }

        public boolean isPaused() {
            return this.paused && this.clip != null && !this.clip.isRunning();
        }

        public void setVolume(float f) {
            if (this.clip != null) {
                FloatControl floatControl = (FloatControl)this.clip.getControl(Type.MASTER_GAIN);
                double d = 20.0 * Math.log10(f <= 0.0F ? 1.0E-4F : f);
                double d2 = Math.max(floatControl.getMinimum(), Math.min(floatControl.getMaximum(), d));
                if (floatControl.getValue() != (float)d2) {
                    floatControl.setValue((float)d2);
                }
            }
        }

        private float getGain() {
            FloatControl floatControl = (FloatControl)this.clip.getControl(Type.MASTER_GAIN);
            return floatControl.getValue();
        }

        public void play() {
            this.stop();

            try {
                this.clip = AudioSystem.getClip();
                InputStream inputStream = AssetCrypto.wrap(SoundPlayer.class.getResourceAsStream(SoundPlayer.getResourcePath(this.sound)));
                assert inputStream != null;

                try {
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
                    this.clip.open(audioInputStream);
                    this.setVolume(this.getGain());
                    this.setLoop(this.isLooping());
                    this.clip.start();
                } catch (Exception exception) {
                    System.out.println(exception.getLocalizedMessage());
                }
            } catch (Exception exception2) {
                System.out.println(exception2.getLocalizedMessage());
            }
        }

        public void stop() {
            if (this.clip != null) {
                if (this.clip.isRunning()) {
                    this.clip.stop();
                }

                if (this.clip.isOpen()) {
                    this.clip.close();
                }

                this.clip = null;
            }
        }

        public String getSound() {
            return this.sound;
        }

    }
}
