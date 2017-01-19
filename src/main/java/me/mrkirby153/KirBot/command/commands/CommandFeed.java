package me.mrkirby153.KirBot.command.commands;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.command.CommandPermission;
import me.mrkirby153.KirBot.guild.BotGuild;
import me.mrkirby153.KirBot.message.MessageHandler;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandFeed extends DiscordCommand {

    private KirBot kirBot;

    public CommandFeed(KirBot kirBot) {
        super(CommandPermission.BOT_MODERATOR, "Manipulate a channel's RSS feed");
        this.kirBot = kirBot;
    }

    @Override
    public String execute(BotGuild guild, TextChannel channel, Member sender, String[] args) {
        if (args.length == 0) {
            return MessageHandler.generateError("Missing arguments!");
        }
        switch (args[0]) {
            case "set":
                if (args.length < 2) {
                    return MessageHandler.generateError("Specify a Feed URL");
                }
                String feedUrl = args[1];
                kirBot.getFeedUpdater().setChannel(channel, feedUrl, guild);
                return MessageHandler.generateSuccess("Set feed to `" + feedUrl + "`!");
            case "delete":
                kirBot.getFeedUpdater().delete(channel);
                return MessageHandler.generateSuccess("Removed feed!");
            case "update":
                kirBot.getFeedUpdater().update(channel.getId(), true);
                channel.sendMessage(MessageHandler.generateSuccess("Updated feed")).queue(m -> MessageHandler.queue(m, 1000));
                return null;
            default:
                return MessageHandler.generateError("Unrecognized option");
        }
    }
}
