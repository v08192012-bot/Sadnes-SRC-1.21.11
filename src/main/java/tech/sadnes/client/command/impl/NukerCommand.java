package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.feature.impl.player.Nuker;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.chat.ChatUtil;

public final class NukerCommand extends Command {
    private static final List<String> LIST_TYPES = List.of("white", "black");
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list", "clear");

    public NukerCommand() {
        super("nuker");
    }

    @Override
    public void execute(String[] strings) {
        Nuker nuker = Nuker.getInstance();
        Nuker.BlockListType blockListType = strings.length > 0 ? parseListType(strings[0]) : null;
        if (nuker != null && blockListType != null && strings.length >= 2) {
            String string;
            label40: {
                string = Language.translate("command.nuker.list." + strings[0].toLowerCase(Locale.US));
                String string2 = strings[1].toLowerCase(Locale.US);
                byte b = -1;
                switch (string2.hashCode()) {
                    case -934610812:
                        if (string2.equals("remove")) {
                            this.remove(nuker, blockListType, string, strings);
                            return;
                        }
                        break;
                    case 96417:
                        if (string2.equals("add")) {
                            this.add(nuker, blockListType, string, strings);
                            return;
                        }
                        break;
                    case 3322014:
                        if (string2.equals("list")) {
                            break label40;
                        }
                        break;
                    case 94746189:
                        if (string2.equals("clear")) {
                            b = 3;
                        }
                }

                switch (b) {
                    case 0:
                        this.add(nuker, blockListType, string, strings);
                        return;
                    case 1:
                        this.remove(nuker, blockListType, string, strings);
                        return;
                    case 2:
                        break;
                    case 3:
                        nuker.clearBlocks(blockListType);
                        ChatUtil.print(Language.translate("command.nuker.cleared", "%s очищен."), string);
                        return;
                    default:
                        ChatUtil.print(Language.translate("command.nuker.usage", "Использование: .nuker <white/black> <add/remove/list/clear> [блок]"));
                        return;
                }
            }

            List list = nuker.getBlockList(blockListType);
            if (list.isEmpty()) {
                ChatUtil.print(Language.translate("command.nuker.empty", "%s пуст."), string);
            } else {
                ChatUtil.print(Language.translate("command.nuker.show", "%s: %s"), string, String.join(", ", stripNamespaces(list)));
            }
        } else {
            ChatUtil.print(Language.translate("command.nuker.usage", "Использование: .nuker <white/black> <add/remove/list/clear> [блок]"));
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(LIST_TYPES, "") : CommandManager.filterByPrefix(LIST_TYPES, strings[0]);
        } else if (strings.length == 2) {
            return CommandManager.filterByPrefix(SUBCOMMANDS, strings[1]);
        } else if (strings.length != 3) {
            return List.of();
        } else {
            String string = strings[1].toLowerCase(Locale.US);
            Nuker nuker = Nuker.getInstance();
            Nuker.BlockListType blockListType = parseListType(strings[0]);
            if (string.equals("add")) {
                return CommandManager.filterByPrefix(this.getBlockSuggestions(), strings[2]);
            } else {
                return string.equals("remove") && nuker != null && blockListType != null
                    ? CommandManager.filterByPrefix(stripNamespaces(nuker.getBlockList(blockListType)), strings[2])
                    : List.of();
            }
        }
    }

    private void add(Nuker nuker, Nuker.BlockListType blockListType, String string, String[] strings) {
        String string2 = strings.length >= 3 ? resolveBlockId(strings[2]) : this.getTargetedBlockId();
        if (string2 == null) {
            ChatUtil.print(Language.translate("command.nuker.invalid", "Блок не найден. Укажите id блока или наведитесь на блок."));
        } else if (nuker.addBlock(blockListType, string2)) {
            ChatUtil.print(Language.translate("command.nuker.added", "%s: добавлен %s"), string, stripNamespace(string2));
        } else {
            ChatUtil.print(Language.translate("command.nuker.exists", "%s: %s уже в списке"), string, stripNamespace(string2));
        }
    }

    private void remove(Nuker nuker, Nuker.BlockListType blockListType, String string, String[] strings) {
        String string2 = strings.length >= 3 ? resolveBlockId(strings[2]) : null;
        if (string2 != null && nuker.removeBlock(blockListType, string2)) {
            ChatUtil.print(Language.translate("command.nuker.removed", "%s: удалён %s"), string, stripNamespace(string2));
        } else {
            ChatUtil.print(Language.translate("command.nuker.notfound", "%s: такого блока нет в списке"), string);
        }
    }

    private static Nuker.BlockListType parseListType(String string) {
        String string2 = string.toLowerCase(Locale.US);
        if (string2.equals("white")) {
            return Nuker.BlockListType.WHITE;
        } else {
            return string2.equals("black") ? Nuker.BlockListType.BLACK : null;
        }
    }

    private static String resolveBlockId(String string) {
        String string2 = string.trim().toLowerCase(Locale.US);
        Identifier identifier = Identifier.tryParse(string2.contains(":") ? string2 : "minecraft:" + string2);
        return identifier != null && Registries.BLOCK.containsId(identifier) ? identifier.toString() : null;
    }

    private String getTargetedBlockId() {
        if (mc.world != null && mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            Block block = mc.world.getBlockState(blockHitResult.getBlockPos()).getBlock();
            return mc.world.getBlockState(blockHitResult.getBlockPos()).isAir() ? null : Registries.BLOCK.getId(block).toString();
        } else {
            return null;
        }
    }

    private List<String> getBlockSuggestions() {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        String string = this.getTargetedBlockId();
        if (string != null) {
            linkedHashSet.add(stripNamespace(string));
        }

        for (Block block : Registries.BLOCK) {
            linkedHashSet.add(Registries.BLOCK.getId(block).getPath());
        }

        return new ArrayList<>(linkedHashSet);
    }

    private static List<String> stripNamespaces(List<String> strings) {
        ArrayList arrayList = new ArrayList();

        for (String string : strings) {
            arrayList.add(stripNamespace(string));
        }

        return arrayList;
    }

    private static String stripNamespace(String string) {
        return string.startsWith("minecraft:") ? string.substring("minecraft:".length()) : string;
    }
}
