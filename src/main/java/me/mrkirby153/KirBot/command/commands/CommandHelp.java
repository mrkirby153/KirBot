package me.mrkirby153.KirBot.command.commands;

import me.mrkirby153.KirBot.command.CustomCommand;
import me.mrkirby153.KirBot.guild.BotGuild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandHelp extends DiscordCommand {

    @Override
    public String execute(BotGuild guild, TextChannel channel, Member sender, String[] args) {
        String commandList = "Commands available on this server:```";

        for (CustomCommand c : guild.customCommands()) {
            commandList += "   - " + c.getName() + "\n";
        }
        commandList += "```";

        return commandList;
    }
}
