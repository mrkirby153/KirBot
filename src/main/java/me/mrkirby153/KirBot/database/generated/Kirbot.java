/*
 * This file is generated by jOOQ.
*/
package me.mrkirby153.KirBot.database.generated;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import me.mrkirby153.KirBot.database.generated.tables.Commands;
import me.mrkirby153.KirBot.database.generated.tables.Feeds;
import me.mrkirby153.KirBot.database.generated.tables.Guild;
import me.mrkirby153.KirBot.database.generated.tables.GuildPermissions;
import me.mrkirby153.KirBot.database.generated.tables.PostedMessages;
import me.mrkirby153.KirBot.database.generated.tables.Users;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.0"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Kirbot extends SchemaImpl {

    private static final long serialVersionUID = 1151521266;

    /**
     * The reference instance of <code>kirbot</code>
     */
    public static final Kirbot KIRBOT = new Kirbot();

    /**
     * The table <code>kirbot.commands</code>.
     */
    public final Commands COMMANDS = me.mrkirby153.KirBot.database.generated.tables.Commands.COMMANDS;

    /**
     * The table <code>kirbot.feeds</code>.
     */
    public final Feeds FEEDS = me.mrkirby153.KirBot.database.generated.tables.Feeds.FEEDS;

    /**
     * The table <code>kirbot.guild</code>.
     */
    public final Guild GUILD = me.mrkirby153.KirBot.database.generated.tables.Guild.GUILD;

    /**
     * The table <code>kirbot.guild_permissions</code>.
     */
    public final GuildPermissions GUILD_PERMISSIONS = me.mrkirby153.KirBot.database.generated.tables.GuildPermissions.GUILD_PERMISSIONS;

    /**
     * The table <code>kirbot.posted_messages</code>.
     */
    public final PostedMessages POSTED_MESSAGES = me.mrkirby153.KirBot.database.generated.tables.PostedMessages.POSTED_MESSAGES;

    /**
     * The table <code>kirbot.users</code>.
     */
    public final Users USERS = me.mrkirby153.KirBot.database.generated.tables.Users.USERS;

    /**
     * No further instances allowed
     */
    private Kirbot() {
        super("kirbot", null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            Commands.COMMANDS,
            Feeds.FEEDS,
            Guild.GUILD,
            GuildPermissions.GUILD_PERMISSIONS,
            PostedMessages.POSTED_MESSAGES,
            Users.USERS);
    }
}
