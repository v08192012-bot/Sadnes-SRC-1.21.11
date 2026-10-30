package tech.sadnes.client.feature.impl.misc.autobuild;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.Settings.Setting;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Vec3i;

public final class BaritoneBuilderControl {
    private static final List<String> EXACT_IGNORED_PROPERTIES = List.of(
        "distance",
        "persistent",
        "waterlogged",
        "power",
        "powered",
        "lit",
        "age",
        "stage",
        "snowy",
        "moisture",
        "triggered",
        "enabled",
        "occupied",
        "extended",
        "attached",
        "disarmed",
        "unstable",
        "level",
        "honey_level",
        "bites",
        "charges",
        "has_record",
        "has_book",
        "has_bottle_0",
        "has_bottle_1",
        "has_bottle_2",
        "signal_fire",
        "north",
        "east",
        "south",
        "west",
        "up",
        "down",
        "in_wall",
        "shape",
        "open",
        "hinge",
        "bottom",
        "drag",
        "tilt",
        "berries",
        "eye",
        "bloom",
        "sculk_sensor_phase",
        "can_summon",
        "shrieking",
        "cracked",
        "dusted",
        "hatch",
        "natural",
        "tip",
        "slot_0_occupied",
        "slot_1_occupied",
        "slot_2_occupied",
        "slot_3_occupied",
        "slot_4_occupied",
        "slot_5_occupied",
        "hydration",
        "side_chain",
        "copper_golem_pose",
        "creaking_heart_state",
        "crafting",
        "ominous",
        "delay",
        "locked",
        "mode",
        "note",
        "instrument",
        "inverted",
        "candles",
        "pickles",
        "eggs",
        "layers",
        "flower_amount",
        "segment_amount",
        "hanging",
        "attachment",
        "thickness",
        "leaves"
    );
    private static final List<String> DEFAULT_IGNORED_PROPERTIES = Stream.concat(EXACT_IGNORED_PROPERTIES.stream(), Stream.of("axis")).toList();
    private static final List<String> ANY_IGNORED_PROPERTIES = Stream.concat(
            EXACT_IGNORED_PROPERTIES.stream(), Stream.of("facing", "axis", "rotation", "face", "orientation", "half", "type", "part", "vertical_direction")
        )
        .toList();
    private static final Pattern MATERIAL_LINE_PATTERN = Pattern.compile("\\d+x (.+)");
    private static final Pattern BLOCK_STATE_PATTERN = Pattern.compile("Block\\{([^}]+)}(\\[.*])?");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("-?\\d+ -?\\d+ -?\\d+");
    private final BooleanSupplier activeSupplier;
    private final Map<Setting<?>, Object> savedSettings = new HashMap<>();
    private final Consumer<Text> logInterceptor = this::interceptBaritoneLog;
    private final Set<BlockState> missingStates = new HashSet<>();
    private final Set<BlockState> missingStatesView = Collections.unmodifiableSet(this.missingStates);
    private Consumer<Text> originalLogger;
    private BaritoneBuilderControl.LogSection logSection = BaritoneBuilderControl.LogSection.NONE;
    private boolean doneBuilding;
    private boolean parseFailed;

    public void saveSettings() {
        Settings settings = BaritoneAPI.getSettings();
        this.savedSettings.clear();

        for (Setting setting : List.of(
            settings.allowPlace,
            settings.allowBreak,
            settings.buildIgnoreExisting,
            settings.allowInventory,
            settings.inventoryMoveOnlyIfStationary,
            settings.buildInLayers,
            settings.layerOrder,
            settings.layerHeight,
            settings.startAtLayer,
            settings.skipFailedLayers,
            settings.buildIgnoreDirection,
            settings.buildIgnoreProperties,
            settings.buildRepeat,
            settings.buildSchematicRotation,
            settings.buildSchematicMirror,
            settings.schematicOrientationX,
            settings.schematicOrientationY,
            settings.schematicOrientationZ,
            settings.mapArtMode,
            settings.buildOnlySelection,
            settings.buildSubstitutes,
            settings.buildSkipBlocks,
            settings.breakCorrectBlockPenaltyMultiplier,
            settings.logAsToast,
            settings.rightClickSpeed,
            settings.allowSprint,
            settings.allowParkour,
            settings.allowParkourAscend,
            settings.blockReachDistance
        )) {
            this.savedSettings.put(setting, setting.value);
        }

        this.originalLogger = settings.logger.value;
        settings.logger.value = this.logInterceptor;
    }

    public void restoreSettings() {
        Settings settings = BaritoneAPI.getSettings();
        this.savedSettings.forEach((setting, object) -> ((Setting<Object>)setting).value = object);
        this.savedSettings.clear();
        if (settings.logger.value == this.logInterceptor) {
            settings.logger.value = this.originalLogger;
        }

        this.originalLogger = null;
    }

    public void applyBuildSettings(boolean flag, boolean flag2, String string) {
        Settings settings;
        Setting setting;
        List list;
        label23: {
            label22: {
                settings = BaritoneAPI.getSettings();
                settings.allowPlace.value = true;
                settings.allowBreak.value = flag;
                settings.buildIgnoreExisting.value = false;
                settings.breakCorrectBlockPenaltyMultiplier.value = 1000000.0;
                settings.allowInventory.value = flag2;
                settings.inventoryMoveOnlyIfStationary.value = true;
                settings.buildInLayers.value = false;
                settings.layerOrder.value = false;
                settings.layerHeight.value = 1;
                settings.startAtLayer.value = 0;
                settings.skipFailedLayers.value = false;
                settings.buildIgnoreDirection.value = string.equals("any");
                setting = settings.buildIgnoreProperties;
                byte b = -1;
                switch (string.hashCode()) {
                    case 96748:
                        if (string.equals("any")) {
                            break label22;
                        }
                        break;
                    case 96946943:
                        if (string.equals("exact")) {
                            b = 1;
                        }
                }

                switch (b) {
                    case 0:
                        break;
                    case 1:
                        list = EXACT_IGNORED_PROPERTIES;
                        break label23;
                    default:
                        list = DEFAULT_IGNORED_PROPERTIES;
                        break label23;
                }
            }

            list = ANY_IGNORED_PROPERTIES;
        }

        setting.value = list;
        settings.buildRepeat.value = Vec3i.ZERO;
        settings.buildSchematicRotation.value = BlockRotation.NONE;
        settings.buildSchematicMirror.value = BlockMirror.NONE;
        settings.schematicOrientationX.value = false;
        settings.schematicOrientationY.value = false;
        settings.schematicOrientationZ.value = false;
        settings.mapArtMode.value = false;
        settings.buildOnlySelection.value = false;
        settings.buildSubstitutes.value = settings.buildSubstitutes.defaultValue;
        settings.buildSkipBlocks.value = settings.buildSkipBlocks.defaultValue;
        settings.logAsToast.value = false;
    }

    public void applyMovementSettings(int i, boolean flag, float f) {
        Settings settings = BaritoneAPI.getSettings();
        settings.blockReachDistance.value = f;
        settings.rightClickSpeed.value = i;
        settings.allowSprint.value = !flag;
        settings.allowParkour.value = !flag;
        if (!flag) {
            settings.allowParkourAscend.value = true;
        } else {
            settings.allowParkourAscend.value = false;
        }
    }

    public void reset() {
        this.doneBuilding = false;
        this.resetMissing();
    }

    public void resetMissing() {
        this.logSection = BaritoneBuilderControl.LogSection.NONE;
        this.missingStates.clear();
        this.parseFailed = false;
    }

    public Set<BlockState> getMissingStates() {
        return this.missingStatesView;
    }

    private void interceptBaritoneLog(Text text) {
        if (!this.activeSupplier.getAsBoolean()) {
            this.forwardLog(text);
        } else {
            String string = text.getString();
            int i = string.startsWith("[") ? string.indexOf("] ") : -1;
            String string2 = i < 0 ? string : string.substring(i + 2);
            Matcher matcher2 = MATERIAL_LINE_PATTERN.matcher(string2);
            if (this.logSection == BaritoneBuilderControl.LogSection.MATERIALS && matcher2.matches()) {
                this.parseMissingState(matcher2.group(1));
            } else if (this.logSection != BaritoneBuilderControl.LogSection.LIQUIDS || !COORDINATES_PATTERN.matcher(string2).matches()) {
                boolean flag = string2.equals("Missing materials for at least:");
                if (flag) {
                    this.resetMissing();
                }

                this.logSection = flag
                    ? BaritoneBuilderControl.LogSection.MATERIALS
                    : (string2.equals("Unreplaceable liquids at at least:") ? BaritoneBuilderControl.LogSection.LIQUIDS : BaritoneBuilderControl.LogSection.NONE);
                boolean flag2 = string2.endsWith("Done building");
                this.doneBuilding |= flag2;
                if (!flag2
                    && this.logSection == BaritoneBuilderControl.LogSection.NONE
                    && !string2.startsWith("Unable to do it. Pausing")
                    && !string2.startsWith("Starting layer ")) {
                    if (string2.startsWith("Skipping layer that I cannot construct!")) {
                        return;
                    }

                    this.forwardLog(text);
                }
            }
        }
    }

    private void parseMissingState(String string) {
        Matcher matcher2 = BLOCK_STATE_PATTERN.matcher(string);
        if (!matcher2.matches()) {
            this.parseFailed = true;
        } else {
            try {
                String string2 = matcher2.group(2) == null ? "" : matcher2.group(2);
                this.missingStates.add(BlockArgumentParser.block(Registries.BLOCK, matcher2.group(1) + string2, false).blockState());
            } catch (CommandSyntaxException commandSyntaxException) {
                this.parseFailed = true;
            }
        }
    }

    private void forwardLog(Text text) {
        if (this.originalLogger != null) {
            this.originalLogger.accept(text);
        }
    }

    public BaritoneBuilderControl(BooleanSupplier booleanSupplier) {
        this.activeSupplier = booleanSupplier;
    }

    public boolean isDoneBuilding() {
        return this.doneBuilding;
    }

    public boolean isParseFailed() {
        return this.parseFailed;
    }

    enum LogSection {
        NONE,
        MATERIALS,
        LIQUIDS;
    }
}
