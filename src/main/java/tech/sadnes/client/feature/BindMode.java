package tech.sadnes.client.feature;

public enum BindMode {
    HOLD,
    TOGGLE;

    public String getDisplayName() {
        String string = this.name().toLowerCase();
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
}
