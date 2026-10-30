package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.network.packet.PartyMember;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import tech.sadnes.client.util.chat.ChatUtil;

public final class PartyCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("info", "create", "invite", "accept", "join", "leave", "kick", "disband", "code");

    public PartyCommand() {
        super("party");
    }

    @Override
    public void execute(String[] strings) {
        BackendService backendService = Sadnes.getInstance().getBackend();
        if (backendService != null && backendService.isConnected()) {
            PartyManager partyManager;
            label123: {
                label124: {
                    label125: {
                        label126: {
                            label127: {
                                label106: {
                                    partyManager = backendService.getPartyManager();
                                    String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "info";
                                    byte b = -1;
                                    switch (string.hashCode()) {
                                        case -1423461112:
                                            if (string.equals("accept")) {
                                                break label127;
                                            }
                                            break;
                                        case -1352294148:
                                            if (string.equals("create")) {
                                                partyManager.createParty();
                                                ChatUtil.print(Language.translate("command.party.creating"));
                                                return;
                                            }
                                            break;
                                        case -1183699191:
                                            if (string.equals("invite")) {
                                                break label106;
                                            }
                                            break;
                                        case 3059181:
                                            if (string.equals("code")) {
                                                break label123;
                                            }
                                            break;
                                        case 3237038:
                                            if (string.equals("info")) {
                                                b = 8;
                                            }
                                            break;
                                        case 3267882:
                                            if (string.equals("join")) {
                                                break label126;
                                            }
                                            break;
                                        case 3291718:
                                            if (string.equals("kick")) {
                                                break label124;
                                            }
                                            break;
                                        case 102846135:
                                            if (string.equals("leave")) {
                                                partyManager.leave();
                                                return;
                                            }
                                            break;
                                        case 1671336899:
                                            if (string.equals("disband")) {
                                                break label125;
                                            }
                                    }

                                    switch (b) {
                                        case 0:
                                            partyManager.createParty();
                                            ChatUtil.print(Language.translate("command.party.creating"));
                                            return;
                                        case 1:
                                            break;
                                        case 2:
                                            break label127;
                                        case 3:
                                            break label126;
                                        case 4:
                                            partyManager.leave();
                                            return;
                                        case 5:
                                            break label125;
                                        case 6:
                                            break label124;
                                        case 7:
                                            break label123;
                                        case 8:
                                            this.printInfo(partyManager);
                                            return;
                                        default:
                                            ChatUtil.print(Language.translate("command.party.usage"), String.join("/", SUBCOMMANDS));
                                            return;
                                    }
                                }

                                if (strings.length < 2) {
                                    ChatUtil.print(Language.translate("command.party.invite.usage"));
                                    return;
                                }

                                partyManager.invite(strings[1]);
                                return;
                            }

                            if (!partyManager.acceptInvite()) {
                                ChatUtil.print(Language.translate("command.party.accept.none"));
                                return;
                            }

                            ChatUtil.print(Language.translate("command.party.accept.joining"));
                            return;
                        }

                        if (strings.length < 2) {
                            ChatUtil.print(Language.translate("command.party.join.usage"));
                            return;
                        }

                        partyManager.join(strings[1]);
                        ChatUtil.print(Language.translate("command.party.join.joining"), strings[1].toUpperCase(Locale.US));
                        return;
                    }

                    if (!partyManager.isLeader()) {
                        ChatUtil.printError(Language.translate("command.party.leaderonly.disband"));
                        return;
                    }

                    partyManager.disband();
                    return;
                }

                if (strings.length < 2) {
                    ChatUtil.print(Language.translate("command.party.kick.usage"));
                    return;
                }

                if (!partyManager.isLeader()) {
                    ChatUtil.printError(Language.translate("command.party.leaderonly.kick"));
                    return;
                }

                PartyMember partyMember = partyManager.findMember(strings[1]);
                if (partyMember == null) {
                    ChatUtil.printError(String.format(Language.translate("command.party.kick.notfound"), strings[1]));
                    return;
                }

                partyManager.kick(partyMember.uid());
                return;
            }

            if (!partyManager.inParty()) {
                ChatUtil.print(Language.translate("command.party.notinparty"));
            } else {
                mc.keyboard.setClipboard(partyManager.getPartyName());
                ChatUtil.print(Language.translate("command.party.code"), partyManager.getPartyName());
            }
        } else {
            ChatUtil.printError(Language.translate("command.party.offline"));
        }
    }

    private void printInfo(PartyManager partyManager) {
        if (!partyManager.inParty()) {
            ChatUtil.print(Language.translate("command.party.info.empty"));
        } else {
            ChatUtil.print(Language.translate("command.party.info.header"), partyManager.getPartyName());

            for (PartyMemberView partyMemberView : partyManager.getMemberViews()) {
                StringBuilder builder = new StringBuilder(" - ").append(partyMemberView.username());
                if (partyMemberView.uid() == partyManager.getLeaderUid()) {
                    builder.append(' ').append(Language.translate("command.party.tag.leader"));
                }

                builder.append(' ').append(Language.translate(partyMemberView.online() ? "command.party.tag.online" : "command.party.tag.offline"));
                if (partyMemberView.hasState()) {
                    builder.append(" · ").append(partyMemberView.state().getUsername()).append(" @ ").append(partyMemberView.state().getServer());
                }

                ChatUtil.print(builder.toString());
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else if (strings.length > 2) {
            return List.of();
        } else {
            BackendService backendService = Sadnes.getInstance().getBackend();
            if (backendService == null) {
                return List.of();
            } else if (strings[0].equalsIgnoreCase("invite")) {
                return backendService.getPartyManager().suggestInvitees(strings[1]);
            } else {
                return strings[0].equalsIgnoreCase("kick") ? CommandManager.filterByPrefix(this.getMemberNames(backendService), strings[1]) : List.of();
            }
        }
    }

    private List<String> getMemberNames(BackendService backendService) {
        ArrayList arrayList = new ArrayList();

        for (PartyMember partyMember : backendService.getPartyManager().getMembers()) {
            arrayList.add(partyMember.username());
        }

        return arrayList;
    }
}
