package tech.sadnes.client.command;

import java.util.List;
import tech.sadnes.client.util.IMinecraft;

public abstract class Command implements IMinecraft {
    private final String name;

    public Command(String string) {
        this.name = string;
    }

    public abstract void execute(String[] strings);

    public abstract List<String> complete(String[] strings);

    public String getName() {
        return this.name;
    }

    public List<String> getAliases() {
        return List.of();
    }
}
