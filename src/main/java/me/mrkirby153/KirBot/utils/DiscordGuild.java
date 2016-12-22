package me.mrkirby153.KirBot.utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

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

    /**
     * Gets the {@link Guild JDA Guild}
     *
     * @return The guild
     */
    public Guild getGuild() {
        return this.guild;
    }

    /**
     * Sets the {@link Guild JDA Guild} that this represents
     *
     * @param guild The guild to set
     */
    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public String getId() {
        return guildId;
    }

    /**
     * Checks if the bot has the given permission on this guild
     *
     * @param p The permission to check
     * @return True if the robot has the given permission on the guild
     */
    public boolean hasPermission(Permission p) {
        Member self = guild.getMember(jda.getSelfUser());
        return self.hasPermission(p);
    }

    /**
     * Checks if the bot has all the given permissions on this guild
     *
     * @param permissions The permissions to check
     * @return True if the robot has all the given permissions on the guild
     */
    public boolean hasPermissions(Permission... permissions) {
        for (Permission p : permissions) {
            if (!hasPermission(p))
                return false;
        }
        return true;
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
