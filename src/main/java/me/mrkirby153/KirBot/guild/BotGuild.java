package me.mrkirby153.KirBot.guild;

import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.database.generated.Tables;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;

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
}
