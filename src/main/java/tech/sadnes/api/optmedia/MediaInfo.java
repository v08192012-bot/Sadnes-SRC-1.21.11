package tech.sadnes.api.optmedia;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MediaInfo extends Structure {
    public byte[] title = new byte[512];
    public byte[] artist = new byte[512];
    public byte[] sourceApp = new byte[256];
    public Pointer albumArtPng;
    public int albumArtSize;
    public byte isPlaying;
    public long durationMs;
    public long positionMs;

    public MediaInfo() {
        super(1);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("title", "artist", "sourceApp", "albumArtPng", "albumArtSize", "isPlaying", "durationMs", "positionMs");
    }

    public String getTitle() {
        return this.decodeCString(this.title);
    }

    public String getArtist() {
        return this.decodeCString(this.artist);
    }

    public String getSource() {
        return this.decodeCString(this.sourceApp);
    }

    private String decodeCString(byte[] bytes) {
        int i = 0;

        while (i < bytes.length && bytes[i] != 0) {
            i++;
        }

        return new String(bytes, 0, i, StandardCharsets.UTF_8);
    }

    public byte[] getAlbumArt() {
        return this.albumArtPng != null && this.albumArtSize > 0 ? this.albumArtPng.getByteArray(0L, this.albumArtSize) : null;
    }

    public boolean isPlaying() {
        return this.isPlaying != 0;
    }
}
