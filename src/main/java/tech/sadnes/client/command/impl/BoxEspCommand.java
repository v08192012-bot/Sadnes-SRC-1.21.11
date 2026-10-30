package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.feature.impl.render.BoxEsp;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.color.ColorUtil;

public final class BoxEspCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list", "clear");
    private static final List<String> TYPES = List.of("block", "entity");

    public BoxEspCommand() {
        super("boxesp");
    }

    @Override
    public List<String> getAliases() {
        return List.of("besp", "box");
    }

    @Override
    public void execute(String[] strings) {
        String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "";
        byte b = -1;
        switch (string.hashCode()) {
            case -934610812:
                if (string.equals("remove")) {
                    this.remove(strings);
                    return;
                }
                break;
            case 96417:
                if (string.equals("add")) {
                    this.add(strings);
                    return;
                }
                break;
            case 3322014:
                if (string.equals("list")) {
                    this.list();
                    return;
                }
                break;
            case 94746189:
                if (string.equals("clear")) {
                    b = 3;
                }
        }

        switch (b) {
            case 0:
                this.add(strings);
                return;
            case 1:
                this.remove(strings);
                return;
            case 2:
                this.list();
                return;
            case 3:
                this.clear();
                return;
            default:
                this.printUsage();
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else {
            String string = strings[0].toLowerCase(Locale.US);
            if (!string.equals("add") && !string.equals("remove")) {
                return List.of();
            } else if (strings.length == 2) {
                return CommandManager.filterByPrefix(TYPES, strings[1]);
            } else if (strings.length == 3) {
                return this.completeTarget(string, strings[1].toLowerCase(Locale.US), strings[2].toLowerCase(Locale.US));
            } else {
                return strings.length == 4 && string.equals("add") ? this.completeColor(strings[3]) : List.of();
            }
        }
    }

    private void add(String[] strings) {
        if (strings.length >= 2 && this.isValidType(strings[1])) {
            String string = strings.length < 3 ? "" : strings[2].trim().toLowerCase(Locale.US);
            if (string.isEmpty()) {
                ChatUtil.print(Language.translate("command.boxesp.empty"));
            } else {
                OptionalInt optionalInt = OptionalInt.empty();
                if (strings.length >= 4) {
                    Integer integer = parseColor(strings[3]);
                    if (integer == null) {
                        ChatUtil.print(Language.translate("command.boxesp.invalidcolor"), strings[3]);
                        return;
                    }

                    optionalInt = OptionalInt.of(integer);
                }

                String string2 = strings[1].toLowerCase(Locale.US);
                BoxEsp boxEsp = BoxEsp.getInstance();
                boolean flag = string2.equals("block") ? boxEsp.addBlock(string, optionalInt) : boxEsp.addEntity(string, optionalInt);
                String string3 = optionalInt.isPresent() ? string + " (" + formatColor(optionalInt.getAsInt()) + ")" : string;
                if (flag) {
                    ChatUtil.print(Language.translate("command.boxesp.added"), this.translateType(string2), string3);
                } else {
                    ChatUtil.print(Language.translate("command.boxesp.exists"), string);
                }
            }
        } else {
            this.printUsage();
        }
    }

    private void remove(String[] strings) {
        if (strings.length >= 2 && this.isValidType(strings[1])) {
            String string = strings.length < 3 ? "" : strings[2].trim().toLowerCase(Locale.US);
            if (string.isEmpty()) {
                ChatUtil.print(Language.translate("command.boxesp.empty"));
            } else {
                String string2 = strings[1].toLowerCase(Locale.US);
                BoxEsp boxEsp = BoxEsp.getInstance();
                boolean flag = string2.equals("block") ? boxEsp.removeBlock(string) : boxEsp.removeEntity(string);
                if (flag) {
                    ChatUtil.print(Language.translate("command.boxesp.removed"), this.translateType(string2), string);
                } else {
                    ChatUtil.print(Language.translate("command.boxesp.notfound"), string);
                }
            }
        } else {
            this.printUsage();
        }
    }

    private void list() {
        BoxEsp boxEsp = BoxEsp.getInstance();
        Map map = boxEsp.getBlockFilters();
        Map map2 = boxEsp.getEntityFilters();
        if (map.isEmpty() && map2.isEmpty()) {
            ChatUtil.print(Language.translate("command.boxesp.list.empty"));
        } else {
            ChatUtil.print(Language.translate("command.boxesp.list.blocks"), map.isEmpty() ? "-" : this.formatEntries(map));
            String string = Language.translate("command.boxesp.list.entities");
            Object[] objects = new Object[1];
            if (map2.isEmpty()) {
                objects[0] = "-";
                ChatUtil.print(string, objects);
            } else {
                objects[0] = this.formatEntries(map2);
                ChatUtil.print(string, objects);
            }
        }
    }

    private void clear() {
        BoxEsp.getInstance().clearAll();
        ChatUtil.print(Language.translate("command.boxesp.cleared"));
    }

    private List<String> completeTarget(String string, String string2, String string3) {
        BoxEsp boxEsp = BoxEsp.getInstance();
        if (string.equals("remove")) {
            Set set = string2.equals("block") ? boxEsp.getBlockFilters().keySet() : (string2.equals("entity") ? boxEsp.getEntityFilters().keySet() : Set.of());
            return CommandManager.filterByPrefix(new ArrayList<>(set), string3);
        }

        if (string2.equals("block")) {
            return CommandManager.filterByPrefix(this.getBlockIds(), string3);
        }

        if (string2.equals("entity")) {
            return CommandManager.filterByPrefix(this.getEntityIds(), string3);
        }

        List list2 = List.of();
        return CommandManager.filterByPrefix(list2, string3);
    }

    private List<String> getBlockIds() {
        LinkedHashSet linkedHashSet = new LinkedHashSet();

        for (Block block : Registries.BLOCK) {
            this.addIdentifier(linkedHashSet, Registries.BLOCK.getId(block));
        }

        if (mc.world != null && mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            this.addIdentifier(linkedHashSet, Registries.BLOCK.getId(mc.world.getBlockState(blockHitResult.getBlockPos()).getBlock()));
        }

        return new ArrayList<>(linkedHashSet);
    }

    private List<String> getEntityIds() {
        LinkedHashSet linkedHashSet = new LinkedHashSet();

        for (EntityType entityType : Registries.ENTITY_TYPE) {
            this.addIdentifier(linkedHashSet, EntityType.getId(entityType));
        }

        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                this.addIdentifier(linkedHashSet, EntityType.getId(entity.getType()));
                String string = entity.getName().getString().toLowerCase(Locale.US);
                if (!string.isEmpty()) {
                    linkedHashSet.add(string);
                }
            }
        }

        return new ArrayList<>(linkedHashSet);
    }

    private void addIdentifier(Set<String> strings, Identifier identifier) {
        if (identifier != null) {
            strings.add(identifier.getPath());
            strings.add(identifier.toString());
        }
    }

    private boolean isValidType(String string) {
        return string.equalsIgnoreCase("block") || string.equalsIgnoreCase("entity");
    }

    private String translateType(String string) {
        return Language.translate("command.boxesp.type." + string);
    }

    private List<String> completeColor(String string) {
        List list2 = List.of("FFFFFF", "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF", "FFA500");
        return CommandManager.filterByPrefix(list2, string);
    }

    private String formatEntries(Map<String, OptionalInt> map) {
        StringBuilder builder = new StringBuilder();

        for (Entry entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append((String)entry.getKey());
            if (((OptionalInt)entry.getValue()).isPresent()) {
                builder.append(" (").append(formatColor(((OptionalInt)entry.getValue()).getAsInt())).append(')');
            }
        }

        return builder.toString();
    }

    private static Integer parseColor(String string) {
        if (string == null) {
            return null;
        }

        String string2 = string.trim();
        if (string2.startsWith("#")) {
            string2 = string2.substring(1);
        }

        if (string2.length() != 6 && string2.length() != 8) {
            return null;
        }

        try {
            return ColorUtil.fromHex(string2);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static String formatColor(int i) {
        int i2 = ColorUtil.alpha(i);
        return i2 == 255
            ? String.format("#%02X%02X%02X", ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i))
            : String.format("#%02X%02X%02X%02X", i2, ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i));
    }

    private void printUsage() {
        ChatUtil.print(Language.translate("command.boxesp.usage"));
    }
}
