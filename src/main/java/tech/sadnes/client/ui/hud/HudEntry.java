package tech.sadnes.client.ui.hud;

import tech.sadnes.client.animation.Animation;

public abstract class HudEntry {
    public final Animation animation = new Animation();
    public boolean active;
    public float y;
    public int index;
    public boolean removing;
    public float x;
    public float width;
    public float height;
    public float alpha;
}
