package tech.sadnes.client.util.media;

import com.sun.jna.win32.StdCallLibrary;
import tech.sadnes.api.optmedia.MediaInfo;

public interface OptMediaLibrary extends StdCallLibrary {
    boolean GetCurrentMediaInfo(MediaInfo mediaInfo);

    void FreeMediaInfo(MediaInfo mediaInfo);

    boolean Play();

    boolean Pause();

    boolean TogglePlayPause();

    boolean SkipNext();

    boolean SkipPrevious();

    void SeekToMs(long l);

    float GetVolume();

    boolean SetVolume(float f);

    void Deinitialize();
}
