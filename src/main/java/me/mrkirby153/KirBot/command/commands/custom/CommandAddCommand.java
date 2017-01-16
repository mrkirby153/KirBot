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

public class CommandAddCommand extends DiscordCommand {

    public CommandAddCommand() {
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
        KirBot.DATABASE.create().insertInto(Tables.COMMANDS, Tables.COMMANDS.GUILD, Tables.COMMANDS.TYPE, Tables.COMMANDS.NAME, Tables.COMMANDS.DATA)
                .values(guild.getId(), type, name, command).execute();
        return MessageHandler.generateSuccess("Added command `" + args[1] + "`");
    }
}
