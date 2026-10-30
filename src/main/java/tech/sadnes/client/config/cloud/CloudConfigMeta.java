package tech.sadnes.client.config.cloud;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;

public final class CloudConfigMeta {
    private static final Gson GSON = new Gson();
    private final String icon;
    private final int color;
    private final String description;
    private final String authorName;
    private final String avatarUrl;
    private final long createdAt;
    private final long modifiedAt;
    private final String build;

    public CloudConfigMeta(String string, int i, String string2, String string3, String string4, long l, long l2, String string5) {
        this.icon = string == null ? "" : string;
        this.color = i;
        this.description = string2 == null ? "" : string2;
        this.authorName = string3 == null ? "" : string3;
        this.avatarUrl = string4 == null ? "" : string4;
        this.createdAt = l;
        this.modifiedAt = l2;
        this.build = string5 == null ? "" : string5;
    }

    public byte[] toBytes() {
        return GSON.toJson(this).getBytes(StandardCharsets.UTF_8);
    }

    public static CloudConfigMeta fromBytes(byte[] bytes) {
        if (bytes != null && bytes.length != 0) {
            try {
                CloudConfigMeta cloudConfigMeta = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), CloudConfigMeta.class);
                return cloudConfigMeta == null
                    ? empty()
                    : new CloudConfigMeta(cloudConfigMeta.icon, cloudConfigMeta.color, cloudConfigMeta.description, cloudConfigMeta.authorName, cloudConfigMeta.avatarUrl, cloudConfigMeta.createdAt, cloudConfigMeta.modifiedAt, cloudConfigMeta.build);
            } catch (Exception exception) {
                return empty();
            }
        } else {
            return empty();
        }
    }

    public static CloudConfigMeta empty() {
        return new CloudConfigMeta("", 0, "", "", "", 0L, 0L, "");
    }

    public String getIcon() {
        return this.icon;
    }

    public int getColor() {
        return this.color;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public long getModifiedAt() {
        return this.modifiedAt;
    }

    public String getBuild() {
        return this.build;
    }
}
