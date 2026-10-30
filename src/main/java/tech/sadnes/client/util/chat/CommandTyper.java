package tech.sadnes.client.util.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.TimerUtil;

public final class CommandTyper implements IMinecraft {
    private final TimerUtil timer = new TimerUtil();
    private String command;
    private int typedLength;

    public void start(String string) {
        this.command = string;
        this.typedLength = 0;
        this.timer.setDelay(150 + ThreadLocalRandom.current().nextInt(200));
    }

    public boolean isTyping() {
        return this.command != null;
    }

    public void cancel() {
        this.command = null;
    }

    public void tick() {
        if (this.command != null) {
            ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
            if (clientPlayNetworkHandler == null) {
                this.command = null;
            } else if (this.timer.isReached()) {
                if (this.typedLength >= this.command.length()) {
                    clientPlayNetworkHandler.sendChatCommand(this.command);
                    this.command = null;
                } else {
                    this.typedLength++;
                    this.requestSuggestions(clientPlayNetworkHandler, this.command.substring(0, this.typedLength));
                    ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
                    TimerUtil timerUtil = this.timer;
                    int i = 65 + threadLocalRandom.nextInt(110);
                    if (threadLocalRandom.nextInt(8) == 0) {
                        timerUtil.setDelay(i + threadLocalRandom.nextInt(220));
                    } else {
                        timerUtil.setDelay(i);
                    }
                }
            }
        }
    }

    private void requestSuggestions(ClientPlayNetworkHandler clientPlayNetworkHandler, String string) {
        String string2 = "/" + string;
        StringReader stringReader = new StringReader(string2);
        stringReader.skip();
        CommandDispatcher commandDispatcher = clientPlayNetworkHandler.getCommandDispatcher();
        commandDispatcher.getCompletionSuggestions(commandDispatcher.parse(stringReader, clientPlayNetworkHandler.getCommandSource()), string2.length());
    }
}
