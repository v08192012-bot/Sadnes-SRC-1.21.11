package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.feature.impl.misc.AutoBuild;
import tech.sadnes.client.feature.impl.misc.autobuild.BuildJob;
import tech.sadnes.client.feature.impl.misc.autobuild.BuildProgress;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.chat.ChatUtil;

public final class BuildCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "litematica", "list", "remove", "clear", "pause", "resume", "skip", "materials", "dir");
    private static final List<String> ROTATIONS = List.of("0", "90", "180", "270");

    public BuildCommand() {
        super("build");
    }

    @Override
    public List<String> getAliases() {
        return List.of("schem");
    }

    @Override
    public void execute(String[] strings) {
        AutoBuild autoBuild = AutoBuild.getInstance();
        if (autoBuild != null) {
            String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
            byte b = -1;
            switch (string.hashCode()) {
                case -1887933611:
                    if (string.equals("litematica")) {
                        autoBuild.addLitematicaJobs();
                        return;
                    }
                    break;
                case -934610812:
                    if (string.equals("remove")) {
                        this.remove(autoBuild, strings);
                        return;
                    }
                    break;
                case -934426579:
                    if (string.equals("resume")) {
                        autoBuild.resume();
                        return;
                    }
                    break;
                case 96417:
                    if (string.equals("add")) {
                        this.add(autoBuild, strings);
                        return;
                    }
                    break;
                case 99469:
                    if (string.equals("dir")) {
                        b = 9;
                    }
                    break;
                case 3322014:
                    if (string.equals("list")) {
                        this.list(autoBuild);
                        return;
                    }
                    break;
                case 3532159:
                    if (string.equals("skip")) {
                        autoBuild.skip();
                        return;
                    }
                    break;
                case 94746189:
                    if (string.equals("clear")) {
                        autoBuild.clearJobs();
                        return;
                    }
                    break;
                case 106440182:
                    if (string.equals("pause")) {
                        autoBuild.pause();
                        return;
                    }
                    break;
                case 681132076:
                    if (string.equals("materials")) {
                        this.printMaterials(autoBuild);
                        return;
                    }
            }

            switch (b) {
                case 0:
                    this.add(autoBuild, strings);
                    return;
                case 1:
                    autoBuild.addLitematicaJobs();
                    return;
                case 2:
                    this.list(autoBuild);
                    return;
                case 3:
                    this.remove(autoBuild, strings);
                    return;
                case 4:
                    autoBuild.clearJobs();
                    return;
                case 5:
                    autoBuild.pause();
                    return;
                case 6:
                    autoBuild.resume();
                    return;
                case 7:
                    autoBuild.skip();
                    return;
                case 8:
                    this.printMaterials(autoBuild);
                    return;
                case 9:
                    autoBuild.openFolder();
                    return;
                default:
                    ChatUtil.print(Language.translate("command.build.usage"));
            }
        }
    }

    private void add(AutoBuild autoBuild, String[] strings) {
        if (strings.length < 2) {
            ChatUtil.print(Language.translate("command.build.add.usage"));
        } else {
            List list2 = autoBuild.getFiles();
            String string = strings[strings.length - 1];
            String string2 = findSchematic(list2, joinArgs(strings, strings.length));
            boolean flag;
            if (string2 == null && strings.length > 2) {
                flag = ROTATIONS.contains(string);
            } else {
                flag = false;
            }

            if (flag) {
                string2 = findSchematic(list2, joinArgs(strings, strings.length - 1));
            }

            if (string2 == null) {
                String string3 = Language.translate("command.build.notfound");
                Object[] objects = new Object[1];
                if (flag) {
                    objects[0] = joinArgs(strings, strings.length - 1);
                    ChatUtil.print(string3, objects);
                } else {
                    objects[0] = joinArgs(strings, strings.length);
                    ChatUtil.print(string3, objects);
                }
            } else if (flag) {
                autoBuild.addFileJob(string2, AutoBuild.parseRotation(string));
            } else {
                autoBuild.addFileJob(string2, AutoBuild.parseRotation("0"));
            }
        }
    }

    private static String joinArgs(String[] strings, int i) {
        return String.join(" ", Arrays.copyOfRange(strings, 1, i)).trim();
    }

    private static String findSchematic(List<String> strings, String string) {
        String string2 = null;

        for (String string3 : strings) {
            if (string3.equals(string)) {
                return string3;
            }

            boolean flag;
            label29: {
                int i = string3.lastIndexOf(46);
                boolean flag2;
                if (!string3.equalsIgnoreCase(string)) {
                    if (i > 0) {
                        flag = string3.substring(0, i).equalsIgnoreCase(string);
                        break label29;
                    }

                    flag2 = false;
                } else {
                    flag2 = true;
                }

                flag = flag2;
            }

            if (flag && string2 == null) {
                string2 = string3;
            }
        }

        return string2;
    }

    private void remove(AutoBuild autoBuild, String[] strings) {
        if (strings.length < 2) {
            ChatUtil.print(Language.translate("command.build.remove.usage"));
        } else {
            int i;
            try {
                i = Integer.parseInt(strings[1]);
            } catch (NumberFormatException numberFormatException) {
                ChatUtil.print(Language.translate("command.build.remove.usage"));
                return;
            }

            if (autoBuild.removeJobAt(i - 1)) {
                ChatUtil.print(Language.translate("command.build.removed"), i);
            } else {
                ChatUtil.print(Language.translate("command.build.remove.notfound"), i);
            }
        }
    }

    private void list(AutoBuild autoBuild) {
        List list2 = autoBuild.getJobs();
        if (list2.isEmpty()) {
            ChatUtil.print(Language.translate("command.build.list.empty"));
        } else {
            int i = list2.size();

            for (int i2 = 0; i2 < i; i2++) {
                BuildJob buildJob = (BuildJob)list2.get(i2);
                BlockPos blockPos = buildJob.getOrigin();
                String string = buildJob.getBounds() == null
                    ? Language.translate("command.build.list.unplaced")
                    : blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
                boolean flag = buildJob.getServer().startsWith("singleplayer:");
                String string2 = flag
                    ? String.format(Language.translate("command.build.list.singleplayer"), buildJob.getServer().substring("singleplayer:".length()))
                    : buildJob.getServer();
                String string3 = Language.translate(buildJob.isStarted() ? "command.build.list.building" : "command.build.list.waiting");
                ChatUtil.print(Language.translate("command.build.list.entry"), i2 + 1, buildJob.getName(), string, string2, string3);
            }
        }
    }

    private void printMaterials(AutoBuild autoBuild) {
        List<BuildProgress.MaterialCount> materialCounts = autoBuild.getMaterials();
        if (materialCounts.isEmpty()) {
            ChatUtil.print(Language.translate("command.build.materials.empty"));
        } else {
            StringBuilder builder = new StringBuilder();

            for (BuildProgress.MaterialCount materialCount : materialCounts) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(new ItemStack(materialCount.item()).getName().getString()).append(' ').append(materialCount.have()).append('/').append(materialCount.need());
            }

            ChatUtil.print(Language.translate("command.build.materials"), builder.toString());
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        }

        AutoBuild autoBuild = AutoBuild.getInstance();
        if (autoBuild != null && strings[0].equalsIgnoreCase("add")) {
            List<String> strings2 = autoBuild.getFiles();
            String string = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
            String string2 = string.toLowerCase(Locale.ROOT);
            int i = string.lastIndexOf(32) + 1;
            ArrayList arrayList = new ArrayList();

            for (String string3 : strings2) {
                if (string3.toLowerCase(Locale.ROOT).startsWith(string2)) {
                    arrayList.add(string3.substring(i));
                }
            }

            boolean flag = strings.length > 2 && findSchematic(strings2, joinArgs(strings, strings.length - 1)) != null;
            if (flag) {
                arrayList.addAll(CommandManager.filterByPrefix(ROTATIONS, strings[strings.length - 1]));
            }

            return arrayList;
        } else {
            return List.of();
        }
    }
}
