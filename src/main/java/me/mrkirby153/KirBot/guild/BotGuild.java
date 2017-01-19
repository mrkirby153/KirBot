package me.mrkirby153.KirBot.guild;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.command.CustomCommand;
import me.mrkirby153.KirBot.database.generated.Tables;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import org.jooq.Record;

import java.util.ArrayList;
import java.util.List;

/**
 * A guild on the server
 */
public class BotGuild {

    /**
     * The Discord snowflake
     */
    private String guildId;

    /**
     * The internal ID
     */
    private int id;


    /**
     * The JDA instance
     */
    private JDA jda;

    /**
     * The Discord guild
     */
    private Guild guild;


    public BotGuild(int id, String guildId, JDA jda) {
        this.id = id;
        this.guildId = guildId;
        this.guild = jda.getGuildById(guildId);
    }

    public Guild getGuild() {
        return guild;
    }

    /**
     * Gets the guild id
     *
     * @return The guild
     */
    public String getGuildId() {
        return guildId;
    }

    /**
     * Gets the id
     *
     * @return The id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the command prefix for the guild
     *
     * @return The command prefix
     */
    public String getCommandPrefix() {
        return KirBot.DATABASE.create().select().from(Tables.GUILD).where(Tables.GUILD.GUILD_ID.eq(this.guildId)).fetchOne(Tables.GUILD.COMMAND_PREFIX);
    }

    /**
     * Gets a custom command by its name
     *
     * @param name The command
     * @return The custom command
     */
    public CustomCommand getCommand(String name) {
        Record result = KirBot.DATABASE.create().select().from(Tables.COMMANDS).where(Tables.COMMANDS.NAME.eq(name), Tables.COMMANDS.GUILD.eq(id)).fetchOne();

        if (result == null)
            return null;

        return new CustomCommand(result.get(Tables.COMMANDS.TYPE), result.get(Tables.COMMANDS.NAME), result.get(Tables.COMMANDS.DATA));
    }

    /**
     * Geta a list of all the custom commands
     *
     * @return The commands
     */
    public List<CustomCommand> customCommands() {
        List<CustomCommand> commands = new ArrayList<>();
        for (Record record : KirBot.DATABASE.create().select().from(Tables.COMMANDS).where(Tables.COMMANDS.GUILD.eq(id)).fetch()) {
            commands.add(getCommand(record.get(Tables.COMMANDS.NAME)));
        }
        return commands;
    }
}
