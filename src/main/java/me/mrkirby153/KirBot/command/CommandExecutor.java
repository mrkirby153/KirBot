package me.mrkirby153.KirBot.command;

import me.mrkirby153.KirBot.user.Clearance;

public abstract class CommandExecutor {

    private String[] aliases;

    private String description;

    private Clearance clearance;

    public abstract void execute();

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public Clearance getClearance() {
        return clearance;
    }

    public void setClearance(Clearance clearance) {
        this.clearance = clearance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
