package tech.sadnes.client.render.texture;

import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;

public enum AnimatedIcon {
    MAIN("main", 62, 8, 8, 30.0F),
    TOGGLE("toggle", 22, 5, 5, 25.0F),
    BG("video", 798, 21, 38, 14.0F),
    DUCK_0("duck_0", 30, 6, 5, 10.0F),
    DUCK_1("duck_1", 30, 6, 5, 10.0F),
    DUCK_2("duck_2", 30, 6, 5, 10.0F),
    DUCK_3("duck_3", 30, 6, 5, 10.0F),
    DUCK_4("duck_4", 30, 6, 5, 10.0F),
    DUCK_5("duck_5", 30, 6, 5, 10.0F),
    DUCK_6("duck_6", 30, 6, 5, 10.0F),
    DUCK_7("duck_7", 30, 6, 5, 10.0F),
    LUNA("luna", 30, 8, 4, 10.0F),
    LUNA_2("luna_2", 30, 8, 4, 10.0F),
    LUNA_3("luna_3", 30, 8, 4, 10.0F),
    LUNA_4("luna_4", 30, 8, 4, 10.0F),
    LUNA_5("luna_5", 30, 8, 4, 10.0F);

    private static final String PATH_PREFIX = "icon/";
    private final String fileName;
    private final int frameCount;
    private final int columns;
    private final int rows;
    private final float fps;
    private final Identifier location;

    AnimatedIcon(String string, int i, int i2, int i3, float f) {
        this.fileName = string;
        this.frameCount = i;
        this.columns = i2;
        this.rows = i3;
        this.fps = f;
        this.location = Identifier.of(ClientInfo.MOD_ID, "icon/" + string + ".png");
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getFrameCount() {
        return this.frameCount;
    }

    public int getColumns() {
        return this.columns;
    }

    public int getRows() {
        return this.rows;
    }

    public float getFps() {
        return this.fps;
    }

    public Identifier getLocation() {
        return this.location;
    }
}
