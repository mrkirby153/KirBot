package me.mrkirby153.KirBot.command.commands;

import me.mrkirby153.KirBot.command.CommandPermission;
import me.mrkirby153.KirBot.guild.BotGuild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandClean extends DiscordCommand {

    public CommandClean() {
        super(CommandPermission.BOT_MODERATOR, "Cleans the last messages in the channel");
    }

    @Override
    public String execute(BotGuild guild, TextChannel channel, Member sender, String[] args) {
        if (args.length == 0) {
            return ":no_entry: Please provide a number";
        }
        try {
            int messageCount = Integer.parseInt(args[0]);
            if (messageCount > 100) {
                return ":no_entry: Can only delete at most 100 messages at a time!";
            }
            channel.getHistory().retrievePast(messageCount).queue(m -> {
                channel.deleteMessages(m).queue(v -> {
                    channel.sendMessage(":white_check_mark: Deleted " + messageCount + " messages!").queue();
                });
            });
        } catch (Exception e) {
            return ":no_entry: `" + args[0] + "` is not a number!";
        }
        return null;
    }
}
