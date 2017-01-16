package me.mrkirby153.KirBot.command.commands.custom;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.command.CommandPermission;
import me.mrkirby153.KirBot.command.commands.DiscordCommand;
import me.mrkirby153.KirBot.database.generated.Tables;
import me.mrkirby153.KirBot.database.generated.enums.CommandsType;
import me.mrkirby153.KirBot.guild.BotGuild;
import me.mrkirby153.KirBot.message.MessageHandler;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandModifyCommand extends DiscordCommand {

    public CommandModifyCommand() {
        super(CommandPermission.BOT_ADMIN);
    }

    @Override
    public String execute(BotGuild guild, TextChannel channel, Member sender, String[] args) {
        if (args.length < 3) {
            return MessageHandler.generateError("Missing required arguments: `<name> <js|text> <command>`");
        }
        String name = args[0].toLowerCase();
        if (guild.getCommand(name) != null) {
            return MessageHandler.generateError("That command already exists! Use `!modifyCommand` to modify it!");
        }
        CommandsType type;
        try {
            type = CommandsType.valueOf(args[1].toUpperCase());
        } catch (Exception e) {
            return MessageHandler.generateError("`" + args[1] + "` is not an acceptable type. Expected `JS` or `TEXT`");
        }
        String command = "";
        for (int i = 2; i < args.length; i++) {
            command += args[i] + " ";
        }
        command = command.substring(0, command.length() - 1);
        KirBot.DATABASE.create().update(Tables.COMMANDS).set(Tables.COMMANDS.TYPE, type).set(Tables.COMMANDS.DATA, command)
                .where(Tables.COMMANDS.GUILD.eq(guild.getId()), Tables.COMMANDS.NAME.eq(name)).execute();
        return MessageHandler.generateSuccess("Modified command `" + args[1] + "`");
    }
}
