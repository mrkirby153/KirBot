package me.mrkirby153.KirBot.command;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.command.commands.DiscordCommand;
import me.mrkirby153.KirBot.database.generated.Tables;
import me.mrkirby153.KirBot.guild.BotGuild;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.jooq.Record;

import java.util.HashMap;

public class CommandHandler extends ListenerAdapter {

    public static CommandHandler INSTANCE;

    private KirBot bot;

    private HashMap<String, DiscordCommand> commands = new HashMap<>();

    public CommandHandler(KirBot bot) {
        this.bot = bot;
        INSTANCE = this;
    }

    public CommandPermission getPermission(Member member, BotGuild guild) {
        // Check for server roles first
        CommandPermission permission = CommandPermission.DEFAULT;

        if (bot.getUserId(member.getUser()) != -1) {
            for (Record result : KirBot.DATABASE.create().select().from(Tables.GUILD_PERMISSIONS).where(Tables.GUILD_PERMISSIONS.GUILD.eq(guild.getId()), Tables.GUILD_PERMISSIONS.USER.eq(bot.getUserId(member.getUser()))).fetch()) {
                switch (result.get(Tables.GUILD_PERMISSIONS.PERMISSION)) {
                    case ADMIN:
                        permission = CommandPermission.BOT_ADMIN;
                        break;
                    case MODERATOR:
                        permission = CommandPermission.BOT_MODERATOR;
                        break;
                }
            }
        }

        if (member.getPermissions().contains(Permission.MANAGE_SERVER)) {
            permission = CommandPermission.SERVER_MANAGE_SERVER;
        }
        if (member.getPermissions().contains(Permission.ADMINISTRATOR)) {
            permission = CommandPermission.SERVER_ADMIN;
        }
        if (member.getUser().getId().equals(CommandPermission.MRKIRBY153_ID))
            permission = CommandPermission.MRKIRBY153;
        return permission;

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message m = event.getMessage();
        String rawMessage = m.getRawContent();
        BotGuild guild = bot.getGuild(event.getGuild());
        if (guild == null)
            return;

        // Ignore messages from ourself
        if (event.getMember().getUser().getId().equals(bot.getJda().getSelfUser().getId()))
            return;

        if (!rawMessage.startsWith(guild.getCommandPrefix())) {
            return;
        }
        String[] parts = rawMessage.split(" ");
        String name = parts.length > 1 ? parts[0].substring(1) : rawMessage.substring(1);

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        if (executeBuiltinCommand(guild, name, event.getTextChannel(), event.getMember(), args)) {
            return;
        }
        CustomCommand command = guild.getCommand(name);
        if (command == null)
            return;
        command.execute(event.getTextChannel(), event.getMember(), args);
    }

    public void registerCommand(DiscordCommand command, String name) {
        KirBot.logger.info("Registering command " + name);
        if (commands.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("Attempted to register a command that already is registered!");
        }
        commands.put(name.toLowerCase(), command);
    }

    private boolean executeBuiltinCommand(BotGuild guild, String name, TextChannel channel, Member member, String[] args) {
        DiscordCommand command = commands.get(name.toLowerCase());
        if (command != null) {
            if (!getPermission(member, guild).has(command.requiredPermission()))
                return false;
            String response = command.execute(guild, channel, member, args);
            if (response != null)
                channel.sendMessage(response).queue();
            return true;
        }
        return false;
    }
}
