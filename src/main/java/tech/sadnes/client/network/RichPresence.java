package tech.sadnes.client.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class RichPresence {
    private static final int MAX_TEXT_LENGTH = 128;
    private static final int MAX_LABEL_LENGTH = 32;
    private static final int MAX_BUTTONS = 2;
    private final String details;
    private final String state;
    private final long startTimestamp;
    private final String largeImage;
    private final String largeText;
    private final String smallImage;
    private final String smallText;
    private final List<RichPresence.Button> buttons;

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        addIfPresent(jsonObject, "details", this.details);
        addIfPresent(jsonObject, "state", this.state);
        if (this.startTimestamp > 0L) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("start", this.startTimestamp);
            jsonObject.add("timestamps", jsonObject2);
        }

        JsonObject jsonObject3 = new JsonObject();
        if (isPresent(this.largeImage)) {
            jsonObject3.addProperty("large_image", this.largeImage);
        }

        addIfPresent(jsonObject3, "large_text", this.largeText);
        if (isPresent(this.smallImage)) {
            jsonObject3.addProperty("small_image", this.smallImage);
        }

        addIfPresent(jsonObject3, "small_text", this.smallText);
        if (!jsonObject3.keySet().isEmpty()) {
            jsonObject.add("assets", jsonObject3);
        }

        JsonArray jsonArray = this.buttonsToJson();
        if (jsonArray.size() > 0) {
            jsonObject.add("buttons", jsonArray);
        }

        return jsonObject;
    }

    private JsonArray buttonsToJson() {
        JsonArray jsonArray = new JsonArray();
        if (this.buttons == null) {
            return jsonArray;
        }

        for (RichPresence.Button button : this.buttons) {
            if (button != null && isPresent(button.label) && isPresent(button.url)) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("label", truncate(button.label, 32));
                jsonObject.addProperty("url", button.url);
                jsonArray.add(jsonObject);
                if (jsonArray.size() == 2) {
                    break;
                }
            }
        }

        return jsonArray;
    }

    private static void addIfPresent(JsonObject jsonObject, String string, String string2) {
        if (isPresent(string2)) {
            jsonObject.addProperty(string, truncate(string2, 128));
        }
    }

    private static boolean isPresent(String string) {
        return string != null && !string.isEmpty();
    }

    private static String truncate(String string, int i) {
        return string.length() <= i ? string : string.substring(0, i);
    }

    RichPresence(String string, String string2, long l, String string3, String string4, String string5, String string6, List<RichPresence.Button> buttons2) {
        this.details = string;
        this.state = string2;
        this.startTimestamp = l;
        this.largeImage = string3;
        this.largeText = string4;
        this.smallImage = string5;
        this.smallText = string6;
        this.buttons = buttons2;
    }

    public static RichPresence.RichPresenceBuilder builder() {
        return new RichPresence.RichPresenceBuilder();
    }

    public String getDetails() {
        return this.details;
    }

    public String getState() {
        return this.state;
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public String getLargeImage() {
        return this.largeImage;
    }

    public String getLargeText() {
        return this.largeText;
    }

    public String getSmallImage() {
        return this.smallImage;
    }

    public String getSmallText() {
        return this.smallText;
    }

    public List<RichPresence.Button> getButtons() {
        return this.buttons;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof RichPresence richPresence)) {
            return false;
        } else {
            if (this.getStartTimestamp() != richPresence.getStartTimestamp()) {
                return false;
            }

            String string = this.getDetails();
            String string2 = richPresence.getDetails();
            if (string == null) {
                if (string2 != null) {
                    return false;
                }
            } else if (!string.equals(string2)) {
                return false;
            }

            String string3 = this.getState();
            String string4 = richPresence.getState();
            if (string3 == null) {
                if (string4 != null) {
                    return false;
                }
            } else if (!string3.equals(string4)) {
                return false;
            }

            String string5 = this.getLargeImage();
            String string6 = richPresence.getLargeImage();
            if (string5 == null) {
                if (string6 != null) {
                    return false;
                }
            } else if (!string5.equals(string6)) {
                return false;
            }

            String string7 = this.getLargeText();
            String string8 = richPresence.getLargeText();
            if (string7 == null) {
                if (string8 != null) {
                    return false;
                }
            } else if (!string7.equals(string8)) {
                return false;
            }

            String string9 = this.getSmallImage();
            String string10 = richPresence.getSmallImage();
            if (string9 == null) {
                if (string10 != null) {
                    return false;
                }
            } else if (!string9.equals(string10)) {
                return false;
            }

            String string11 = this.getSmallText();
            String string12 = richPresence.getSmallText();
            if (string11 == null) {
                if (string12 != null) {
                    return false;
                }
            } else if (!string11.equals(string12)) {
                return false;
            }

            List list = this.getButtons();
            List list2 = richPresence.getButtons();
            if (list == null) {
                if (list2 != null) {
                    return false;
                }
            } else if (!list.equals(list2)) {
                return false;
            }

            return true;
        }
    }

    @Override
    public int hashCode() {
        long l = this.getStartTimestamp();
        int i = 59 + (int)(l >>> 32 ^ l);
        String string = this.getDetails();
        int i2 = i * 59 + (string == null ? 43 : string.hashCode());
        String string2 = this.getState();
        int i3 = i2 * 59 + (string2 == null ? 43 : string2.hashCode());
        String string3 = this.getLargeImage();
        int i4 = i3 * 59 + (string3 == null ? 43 : string3.hashCode());
        String string4 = this.getLargeText();
        int i5 = i4 * 59 + (string4 == null ? 43 : string4.hashCode());
        String string5 = this.getSmallImage();
        int i6 = i5 * 59 + (string5 == null ? 43 : string5.hashCode());
        String string6 = this.getSmallText();
        int i7 = i6 * 59 + (string6 == null ? 43 : string6.hashCode());
        List list = this.getButtons();
        return list == null ? i7 * 59 + 43 : i7 * 59 + list.hashCode();
    }

    @Override
    public String toString() {
        return "RichPresence(details="
            + this.getDetails()
            + ", state="
            + this.getState()
            + ", startTimestamp="
            + this.getStartTimestamp()
            + ", largeImage="
            + this.getLargeImage()
            + ", largeText="
            + this.getLargeText()
            + ", smallImage="
            + this.getSmallImage()
            + ", smallText="
            + this.getSmallText()
            + ", buttons="
            + this.getButtons()
            + ")";
    }

    public static final class Button {
        final String label;
        final String url;

        public Button(String string, String string2) {
            this.label = string;
            this.url = string2;
        }

        public String getLabel() {
            return this.label;
        }

        public String getUrl() {
            return this.url;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            } else if (!(object instanceof RichPresence.Button button)) {
                return false;
            } else {
                String string = this.getLabel();
                String string2 = button.getLabel();
                if (string == null) {
                    if (string2 != null) {
                        return false;
                    }
                } else if (!string.equals(string2)) {
                    return false;
                }

                String string3 = this.getUrl();
                String string4 = button.getUrl();
                if (string3 == null) {
                    if (string4 != null) {
                        return false;
                    }
                } else if (!string3.equals(string4)) {
                    return false;
                }

                return true;
            }
        }

        @Override
        public int hashCode() {
            String string = this.getLabel();
            int i = 59 + (string == null ? 43 : string.hashCode());
            String string2 = this.getUrl();
            return string2 == null ? i * 59 + 43 : i * 59 + string2.hashCode();
        }

        @Override
        public String toString() {
            return "RichPresence.Button(label=" + this.getLabel() + ", url=" + this.getUrl() + ")";
        }
    }

    public static class RichPresenceBuilder {
        private String details;
        private String state;
        private long startTimestamp;
        private String largeImage;
        private String largeText;
        private String smallImage;
        private String smallText;
        private ArrayList<RichPresence.Button> buttons;

        RichPresenceBuilder() {
        }

        public RichPresence.RichPresenceBuilder details(String string) {
            this.details = string;
            return this;
        }

        public RichPresence.RichPresenceBuilder state(String string) {
            this.state = string;
            return this;
        }

        public RichPresence.RichPresenceBuilder startTimestamp(long l) {
            this.startTimestamp = l;
            return this;
        }

        public RichPresence.RichPresenceBuilder largeImage(String string) {
            this.largeImage = string;
            return this;
        }

        public RichPresence.RichPresenceBuilder largeText(String string) {
            this.largeText = string;
            return this;
        }

        public RichPresence.RichPresenceBuilder smallImage(String string) {
            this.smallImage = string;
            return this;
        }

        public RichPresence.RichPresenceBuilder smallText(String string) {
            this.smallText = string;
            return this;
        }

        public RichPresence.RichPresenceBuilder button(RichPresence.Button button2) {
            if (this.buttons == null) {
                this.buttons = new ArrayList<>();
            }

            this.buttons.add(button2);
            return this;
        }

        public RichPresence.RichPresenceBuilder buttons(Collection<? extends RichPresence.Button> collection) {
            if (collection == null) {
                throw new NullPointerException("buttons cannot be null");
            }

            if (this.buttons == null) {
                this.buttons = new ArrayList<>();
            }

            this.buttons.addAll(collection);
            return this;
        }

        public RichPresence.RichPresenceBuilder clearButtons() {
            if (this.buttons != null) {
                this.buttons.clear();
            }

            return this;
        }

        public RichPresence build() {
            if (this.buttons != null) {
                switch (this.buttons.size()) {
                    case 0:
                        break;
                    case 1:
                        List list = Collections.singletonList(this.buttons.get(0));
                        return new RichPresence(
                            this.details, this.state, this.startTimestamp, this.largeImage, this.largeText, this.smallImage, this.smallText, list
                        );
                    default:
                        List list2 = Collections.unmodifiableList(new ArrayList<>(this.buttons));
                        return new RichPresence(
                            this.details, this.state, this.startTimestamp, this.largeImage, this.largeText, this.smallImage, this.smallText, list2
                        );
                }
            }

            List list3 = Collections.emptyList();
            return new RichPresence(this.details, this.state, this.startTimestamp, this.largeImage, this.largeText, this.smallImage, this.smallText, list3);
        }

        @Override
        public String toString() {
            return "RichPresence.RichPresenceBuilder(details="
                + this.details
                + ", state="
                + this.state
                + ", startTimestamp="
                + this.startTimestamp
                + ", largeImage="
                + this.largeImage
                + ", largeText="
                + this.largeText
                + ", smallImage="
                + this.smallImage
                + ", smallText="
                + this.smallText
                + ", buttons="
                + this.buttons
                + ")";
        }
    }
}
