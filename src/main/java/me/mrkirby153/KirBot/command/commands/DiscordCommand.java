package me.mrkirby153.KirBot.command.commands;

import me.mrkirby153.KirBot.command.CommandPermission;
import me.mrkirby153.KirBot.guild.BotGuild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public abstract class DiscordCommand {

    private CommandPermission permission;

    private String help;

    public DiscordCommand(CommandPermission permission, String help) {
        this.permission = permission;
    }

    public DiscordCommand(CommandPermission permission){
        this(permission, "");
    }

    public DiscordCommand() {
        this(CommandPermission.DEFAULT);
    }

    public abstract String execute(BotGuild guild, TextChannel channel, Member sender, String[] args);

    public CommandPermission requiredPermission() {
        return permission;
    }

    public String getHelp() {
        return help;
    }
}
