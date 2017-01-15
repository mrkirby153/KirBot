package me.mrkirby153.KirBot.command;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.guild.BotGuild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class CommandHandler extends ListenerAdapter {

    private KirBot bot;

    public CommandHandler(KirBot bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message m = event.getMessage();
        String rawMessage = m.getRawContent();
        BotGuild guild = bot.getGuild(event.getGuild());
        if (guild == null)
            return;

        // TODO: 1/15/2017 Handle built in commands before handling custom commands
        if (!rawMessage.startsWith(guild.getCommandPrefix())) {
            return;
        }
        String[] parts = rawMessage.split(" ");
        String name = parts.length > 1 ? parts[0].substring(1) : rawMessage.substring(1);

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        CustomCommand command = guild.getCommand(name);
        if(command == null)
            return;
        command.execute(event.getTextChannel(), event.getMember(), args);
    }
}
