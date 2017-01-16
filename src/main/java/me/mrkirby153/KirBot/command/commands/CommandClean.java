package me.mrkirby153.KirBot.command.commands;

import me.mrkirby153.KirBot.command.CommandPermission;
import me.mrkirby153.KirBot.guild.BotGuild;
import me.mrkirby153.KirBot.message.MessageHandler;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandClean extends DiscordCommand {

    public CommandClean() {
        super(CommandPermission.BOT_MODERATOR, "Cleans the last messages in the channel");
    }

    @Override
    public String execute(BotGuild guild, TextChannel channel, Member sender, String[] args) {
        if (args.length == 0) {
            return MessageHandler.generateError("Please provide the number of messages to delete. (Max 100)");
        }
        try {
            int messageCount = Integer.parseInt(args[0]);
            if (messageCount > 100) {
                return MessageHandler.generateError("I can only delete 100 messages at a time");
            }
            channel.getHistory().retrievePast(messageCount).queue(m -> {
                channel.deleteMessages(m).queue(v -> {
                    channel.sendMessage(MessageHandler.generateSuccess("Deleted " + messageCount + " messages!")).queue(me -> MessageHandler.queue(me, 3000));
                });
            });
        } catch (Exception e) {
            return MessageHandler.generateError("`" + args[0] + "` is not a number!");
        }
        return null;
    }
}
