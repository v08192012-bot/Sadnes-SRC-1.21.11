package tech.sadnes.client.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import tech.sadnes.client.util.IMinecraft;

public enum SpecialPotion implements IMinecraft {
    HOLY(
        16777215,
        Text.literal("")
            .append(coloredChar("[★]", 16777163))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("С", 16777163))
            .append(coloredChar("в", 16777148))
            .append(coloredChar("я", 16777132))
            .append(coloredChar("т", 16777117))
            .append(coloredChar("а", 16777102))
            .append(coloredChar("я", 16777087))
            .append(coloredChar(" ", 16776815))
            .append(coloredChar("в", 16776800))
            .append(coloredChar("о", 16776785))
            .append(coloredChar("д", 16776769))
            .append(coloredChar("а", 16776754)),
        Arrays.asList(
            new SpecialPotion.Effect(StatusEffects.REGENERATION, 45, 1),
            new SpecialPotion.Effect(StatusEffects.INVISIBILITY, 600, 1),
            new SpecialPotion.Effect(StatusEffects.INSTANT_HEALTH, 0, 1)
        ),
        Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    RAGE(
        10040115,
        Text.literal("")
            .append(coloredChar("[★]", 9109504))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("З", 9109504))
            .append(coloredChar("е", 10620416))
            .append(coloredChar("л", 12131328))
            .append(coloredChar("ь", 13707520))
            .append(coloredChar("е", 15218432))
            .append(coloredChar(" ", 16729344))
            .append(coloredChar("Г", 16732928))
            .append(coloredChar("н", 16736512))
            .append(coloredChar("е", 16740352))
            .append(coloredChar("в", 16743936))
            .append(coloredChar("а", 16747520)),
        Arrays.asList(new SpecialPotion.Effect(StatusEffects.STRENGTH, 30, 4), new SpecialPotion.Effect(StatusEffects.SLOWNESS, 30, 3)),
        Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    PALADIN(
        65535,
        Text.literal("")
            .append(coloredChar("[★]", 13762395))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("З", 13762395))
            .append(coloredChar("е", 14090092))
            .append(coloredChar("л", 14417789))
            .append(coloredChar("ь", 14745486))
            .append(coloredChar("е", 15007648))
            .append(coloredChar(" ", 15335345))
            .append(coloredChar("П", 15663042))
            .append(coloredChar("а", 15990739))
            .append(coloredChar("л", 15663042))
            .append(coloredChar("л", 15335345))
            .append(coloredChar("а", 15007648))
            .append(coloredChar("д", 14745486))
            .append(coloredChar("и", 14417789))
            .append(coloredChar("н", 14090092))
            .append(coloredChar("а", 13762395)),
        Arrays.asList(
            new SpecialPotion.Effect(StatusEffects.RESISTANCE, 600, 0),
            new SpecialPotion.Effect(StatusEffects.FIRE_RESISTANCE, 600, 0),
            new SpecialPotion.Effect(StatusEffects.HEALTH_BOOST, 60, 2),
            new SpecialPotion.Effect(StatusEffects.INVISIBILITY, 900, 0)
        ),
        Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    ASSASSIN(
        3355443,
        Text.literal("")
            .append(coloredChar("[★]", 4277061))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("З", 4277061))
            .append(coloredChar("е", 4603456))
            .append(coloredChar("л", 4929850))
            .append(coloredChar("ь", 5256245))
            .append(coloredChar("е", 5516848))
            .append(coloredChar(" ", 5843242))
            .append(coloredChar("А", 6169637))
            .append(coloredChar("с", 6496032))
            .append(coloredChar("с", 6822427))
            .append(coloredChar("а", 7148821))
            .append(coloredChar("с", 7409424))
            .append(coloredChar("и", 7735819))
            .append(coloredChar("н", 8062213))
            .append(coloredChar("а", 8388608)),
        Arrays.asList(
            new SpecialPotion.Effect(StatusEffects.STRENGTH, 60, 3),
            new SpecialPotion.Effect(StatusEffects.SPEED, 300, 2),
            new SpecialPotion.Effect(StatusEffects.HASTE, 60, 0),
            new SpecialPotion.Effect(StatusEffects.INSTANT_DAMAGE, 0, 1)
        ),
        Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    RADIATION(
        3329330,
        Text.literal("")
            .append(coloredChar("[★]", 16774970))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("З", 16774970))
            .append(coloredChar("е", 16250192))
            .append(coloredChar("л", 15659878))
            .append(coloredChar("ь", 15135100))
            .append(coloredChar("е", 14545043))
            .append(coloredChar(" ", 14020265))
            .append(coloredChar("Р", 13429951))
            .append(coloredChar("а", 12905919))
            .append(coloredChar("д", 12382378))
            .append(coloredChar("и", 11858836))
            .append(coloredChar("а", 11269759))
            .append(coloredChar("ц", 10746217))
            .append(coloredChar("и", 10222676))
            .append(coloredChar("и", 9699134)),
        Arrays.asList(
            new SpecialPotion.Effect(StatusEffects.POISON, 60, 1),
            new SpecialPotion.Effect(StatusEffects.WITHER, 60, 1),
            new SpecialPotion.Effect(StatusEffects.SLOWNESS, 90, 2),
            new SpecialPotion.Effect(StatusEffects.HUNGER, 60, 4),
            new SpecialPotion.Effect(StatusEffects.GLOWING, 120, 0)
        ),
        Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    SLEEPS(
        255,
        Text.literal("")
            .append(coloredChar("[★]", 4132250))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("С", 4132250))
            .append(coloredChar("н", 3219615))
            .append(coloredChar("о", 2306725))
            .append(coloredChar("т", 1394090))
            .append(coloredChar("в", 481455))
            .append(coloredChar("о", 812728))
            .append(coloredChar("р", 2322884))
            .append(coloredChar("н", 3833041))
            .append(coloredChar("о", 5408733))
            .append(coloredChar("е", 6918889)),
        Arrays.asList(
            new SpecialPotion.Effect(StatusEffects.WEAKNESS, 90, 1),
            new SpecialPotion.Effect(StatusEffects.MINING_FATIGUE, 10, 1),
            new SpecialPotion.Effect(StatusEffects.WITHER, 90, 2),
            new SpecialPotion.Effect(StatusEffects.BLINDNESS, 10, 0)
        ),
        Arrays.asList(4737096, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    BOOMKA(
        16738740,
        Text.literal("")
            .append(coloredChar("[★]", 16716947))
            .append(coloredChar(" ", 16777215))
            .append(coloredChar("Х", 16711765))
            .append(coloredChar("л", 16727869))
            .append(coloredChar("л", 16727869))
            .append(coloredChar("о", 16743972))
            .append(coloredChar("п", 16760076))
            .append(coloredChar("у", 14410269))
            .append(coloredChar("ш", 9628759))
            .append(coloredChar("к", 4846994))
            .append(coloredChar("а", 65484)),
        Arrays.asList(
            new SpecialPotion.Effect(StatusEffects.SLOWNESS, 10, 9),
            new SpecialPotion.Effect(StatusEffects.SPEED, 20, 4),
            new SpecialPotion.Effect(StatusEffects.BLINDNESS, 5, 9),
            new SpecialPotion.Effect(StatusEffects.GLOWING, 180, 0)
        ),
        Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
    ),
    NULL(-1, Text.literal(""), new ArrayList<>(), List.of(-1));

    private final Integer color;
    private final Text displayName;
    private final List<SpecialPotion.Effect> effects;
    private final List<Integer> particleColors;

    private static MutableText coloredChar(String string, int i) {
        return Text.literal(string).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(i)).withBold(true));
    }

    public static Slot findSlot(SpecialPotion specialPotion) {
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            ArrayList<Slot> slots2 = new ArrayList<>(mc.player.currentScreenHandler.slots);
            Collections.reverse(slots2);
            return slots2.stream().filter(slot2 -> {
                SpecialPotion specialPotion2 = fromStack(slot2.getStack());
                return specialPotion2 != null && specialPotion2 == specialPotion;
            }).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public static SpecialPotion fromStack(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionContentsComponent == null) {
                return null;
            }

            ArrayList arrayList = new ArrayList();

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                arrayList.add(statusEffectInstance);
            }

            for (SpecialPotion specialPotion : values()) {
                if (specialPotion != NULL && matchesEffects(arrayList, specialPotion.getEffects())) {
                    return specialPotion;
                }
            }
        }

        return null;
    }

    public static boolean matchesEffects(List<StatusEffectInstance> statusEffectInstances, List<SpecialPotion.Effect> effects2) {
        if (statusEffectInstances.size() != effects2.size()) {
            return false;
        }

        label27:
        for (SpecialPotion.Effect effect2 : effects2) {
            for (StatusEffectInstance statusEffectInstance : statusEffectInstances) {
                if (statusEffectInstance.getEffectType().equals(effect2.effect()) && statusEffectInstance.getAmplifier() == effect2.amplifier()) {
                    continue label27;
                }
            }

            return false;
        }

        return true;
    }

    public Integer getColor() {
        return this.color;
    }

    public Text getDisplayName() {
        return this.displayName;
    }

    public List<SpecialPotion.Effect> getEffects() {
        return this.effects;
    }

    public List<Integer> getParticleColors() {
        return this.particleColors;
    }

    SpecialPotion(Integer integer, Text text, List<SpecialPotion.Effect> effects2, List<Integer> integers) {
        this.color = integer;
        this.displayName = text;
        this.effects = effects2;
        this.particleColors = integers;
    }

    public record Effect(RegistryEntry<StatusEffect> effect, Integer durationSeconds, Integer amplifier) {
        public Integer getDurationTicks() {
            return this.durationSeconds * 20;
        }
    }
}
