package me.mrkirby153.KirBot.utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;

/**
 * A guild that the robot is a part of
 */
public class DiscordGuild {

    private final String guildId;
    private String commandPrefix = "!";

    private transient Guild guild;
    private transient JDA jda;

    public DiscordGuild(String guildId) {
        this.guildId = guildId;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DiscordGuild && ((DiscordGuild) obj).guildId.equals(guildId);
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public String getId() {
        return guildId;
    }

    /**
     * Sets the {@link Guild JDA Guild} that this represents
     *
     * @param guild The guild to set
     */
    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    /**
     * Sets the {@link JDA} instance
     *
     * @param jda The JDA instance
     */
    public void setJDA(JDA jda) {
        this.jda = jda;
    }
}
