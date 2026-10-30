package tech.sadnes.client.cosmetic;


public enum CosmeticType {
    HAT("hat"),
    BODYWEAR("bodywear"),
    WINGS("wings"),
    PET("pet"),
    SWORD("sword");

    private final String id;

    public String getTranslationKey() {
        return "cosmetics.type." + this.id;
    }

    public static CosmeticType fromId(String string) {
        for (CosmeticType cosmeticType : values()) {
            if (cosmeticType.id.equals(string)) {
                return cosmeticType;
            }
        }

        return null;
    }

    public String getId() {
        return this.id;
    }

    CosmeticType(String string) {
        this.id = string;
    }
}
