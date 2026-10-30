package tech.sadnes.client.config.cloud;

import tech.sadnes.client.network.packet.CloudConfigEntry;
import tech.sadnes.client.render.texture.Icon;

public final class CloudConfigInfo {
    private final CloudConfigEntry record;
    private final CloudConfigMeta meta;

    public CloudConfigInfo(CloudConfigEntry cloudConfigEntry) {
        this.record = cloudConfigEntry;
        this.meta = CloudConfigMeta.fromBytes(cloudConfigEntry.meta());
    }

    public String getId() {
        return this.record.id();
    }

    public String getName() {
        return this.record.name();
    }

    public boolean isShared() {
        return this.record.shared();
    }

    public String getOwnerName() {
        return this.record.ownerName();
    }

    public String getShareId() {
        return this.record.shareId();
    }

    public boolean isPublic() {
        return this.record.isPublic();
    }

    public long getModifiedAt() {
        return this.record.modifiedAtMs();
    }

    public Icon getIcon() {
        if (this.meta.getIcon() != null && !this.meta.getIcon().isEmpty()) {
            try {
                return Icon.valueOf(this.meta.getIcon());
            } catch (IllegalArgumentException illegalArgumentException) {
                return null;
            }
        } else {
            return null;
        }
    }

    public int getColor() {
        return this.meta.getColor();
    }

    public String getDescription() {
        return this.meta.getDescription();
    }

    public String getAuthorName() {
        return this.isShared() ? this.getOwnerName() : this.meta.getAuthorName();
    }

    public String getAvatarUrl() {
        return this.meta.getAvatarUrl();
    }

    public long getCreatedAt() {
        return this.meta.getCreatedAt();
    }

    public String getBuild() {
        return this.meta.getBuild();
    }
}
