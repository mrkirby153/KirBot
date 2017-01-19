/*
 * This file is generated by jOOQ.
*/
package me.mrkirby153.KirBot.database.generated.tables;


import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import me.mrkirby153.KirBot.database.generated.Keys;
import me.mrkirby153.KirBot.database.generated.Kirbot;
import me.mrkirby153.KirBot.database.generated.tables.records.PostedMessagesRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


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
public class PostedMessages extends TableImpl<PostedMessagesRecord> {

    private static final long serialVersionUID = 1957133633;

    /**
     * The reference instance of <code>kirbot.posted_messages</code>
     */
    public static final PostedMessages POSTED_MESSAGES = new PostedMessages();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PostedMessagesRecord> getRecordType() {
        return PostedMessagesRecord.class;
    }

    /**
     * The column <code>kirbot.posted_messages.id</code>.
     */
    public final TableField<PostedMessagesRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>kirbot.posted_messages.guid</code>.
     */
    public final TableField<PostedMessagesRecord, String> GUID = createField("guid", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

    /**
     * The column <code>kirbot.posted_messages.feed</code>.
     */
    public final TableField<PostedMessagesRecord, Integer> FEED = createField("feed", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>kirbot.posted_messages.channel</code>.
     */
    public final TableField<PostedMessagesRecord, String> CHANNEL = createField("channel", org.jooq.impl.SQLDataType.VARCHAR.length(45).nullable(false), this, "");

    /**
     * The column <code>kirbot.posted_messages.posted_on</code>.
     */
    public final TableField<PostedMessagesRecord, Timestamp> POSTED_ON = createField("posted_on", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.inline("CURRENT_TIMESTAMP", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * Create a <code>kirbot.posted_messages</code> table reference
     */
    public PostedMessages() {
        this("posted_messages", null);
    }

    /**
     * Create an aliased <code>kirbot.posted_messages</code> table reference
     */
    public PostedMessages(String alias) {
        this(alias, POSTED_MESSAGES);
    }

    private PostedMessages(String alias, Table<PostedMessagesRecord> aliased) {
        this(alias, aliased, null);
    }

    private PostedMessages(String alias, Table<PostedMessagesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Kirbot.KIRBOT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<PostedMessagesRecord, Integer> getIdentity() {
        return Keys.IDENTITY_POSTED_MESSAGES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PostedMessagesRecord> getPrimaryKey() {
        return Keys.KEY_POSTED_MESSAGES_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PostedMessagesRecord>> getKeys() {
        return Arrays.<UniqueKey<PostedMessagesRecord>>asList(Keys.KEY_POSTED_MESSAGES_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ForeignKey<PostedMessagesRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<PostedMessagesRecord, ?>>asList(Keys.FK_POSTED_MESSAGES_FEEDS1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PostedMessages as(String alias) {
        return new PostedMessages(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public PostedMessages rename(String name) {
        return new PostedMessages(name, null);
    }
}
