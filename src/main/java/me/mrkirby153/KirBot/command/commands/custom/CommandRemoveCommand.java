package me.mrkirby153.KirBot.command.commands.custom;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.command.CommandPermission;
import me.mrkirby153.KirBot.command.commands.DiscordCommand;
import me.mrkirby153.KirBot.database.generated.Tables;
import me.mrkirby153.KirBot.guild.BotGuild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandRemoveCommand extends DiscordCommand {

    public CommandRemoveCommand() {
        super(CommandPermission.BOT_ADMIN, "Removes a command with the given name");
    }

    @Override
    public String execute(BotGuild guild, TextChannel channel, Member sender, String[] args) {
        if (args.length < 1) {
            return ":no_entry: Please provide a command name";
        }
        String commandName = args[0].toLowerCase();
        if (guild.getCommand(commandName) != null) {
            KirBot.DATABASE.create().deleteFrom(Tables.COMMANDS).where(Tables.COMMANDS.GUILD.eq(guild.getId()), Tables.COMMANDS.NAME.eq(commandName)).execute();
            return ":white_check_mark: Deleted command `" + commandName + "`";
        } else {
            return ":no_entry: That command doesn't exist!";
        }
    }
}
