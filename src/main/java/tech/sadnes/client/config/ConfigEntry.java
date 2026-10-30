package tech.sadnes.client.config;

import tech.sadnes.client.render.texture.Icon;

public class ConfigEntry {
    private final String name;
    private final ConfigManager manager;
    private Icon icon;
    private int color;
    private String note = "";
    private long createdAt;
    private long modifiedAt;
    private String build = "";
    private String owner = "";
    private String ownerAvatar = "";

    ConfigEntry(String string, ConfigManager configManager) {
        this.name = string;
        this.manager = configManager;
    }

    void applyMetadata(Icon icon2, int i, String string, long l, long l2, String string2, String string3, String string4) {
        this.icon = icon2;
        this.color = i;
        this.note = string == null ? "" : string;
        this.createdAt = l;
        this.modifiedAt = l2;
        this.build = string2 == null ? "" : string2;
        this.owner = string3 == null ? "" : string3;
        if (string4 == null) {
            this.ownerAvatar = "";
        } else {
            this.ownerAvatar = string4;
        }
    }

    public boolean load() {
        return this.manager.save(this.name);
    }

    public boolean loadRaw() {
        return this.manager.load(this.name);
    }

    public boolean delete() {
        return this.manager.delete(this.name);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof ConfigEntry configEntry)) {
            return false;
        } else {
            if (!configEntry.canEqual(this)) {
                return false;
            }

            String string = this.getName();
            String string2 = configEntry.getName();
            if (string == null) {
                if (string2 != null) {
                    return false;
                }
            } else if (!string.equals(string2)) {
                return false;
            }

            return true;
        }
    }

    protected boolean canEqual(Object object) {
        return object instanceof ConfigEntry;
    }

    @Override
    public int hashCode() {
        String string = this.getName();
        return string == null ? 102 : 59 + string.hashCode();
    }

    public String getName() {
        return this.name;
    }

    public Icon getIcon() {
        return this.icon;
    }

    public void setIcon(Icon icon2) {
        this.icon = icon2;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String string) {
        this.note = string;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    void setCreatedAt(long l) {
        this.createdAt = l;
    }

    public long getModifiedAt() {
        return this.modifiedAt;
    }

    void setModifiedAt(long l) {
        this.modifiedAt = l;
    }

    public String getBuild() {
        return this.build;
    }

    void setBuild(String string) {
        this.build = string;
    }

    public String getOwner() {
        return this.owner;
    }

    void setOwner(String string) {
        this.owner = string;
    }

    public String getOwnerAvatar() {
        return this.ownerAvatar;
    }

    void setOwnerAvatar(String string) {
        this.ownerAvatar = string;
    }
}
