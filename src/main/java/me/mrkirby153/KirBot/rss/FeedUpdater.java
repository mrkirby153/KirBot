package me.mrkirby153.KirBot.rss;

import com.pnikosis.html2markdown.HTML2Md;
import me.mrkirby153.KirBot.KirBot;
import me.mrkirby153.KirBot.database.generated.Tables;
import me.mrkirby153.KirBot.guild.BotGuild;
import me.mrkirby153.KirBot.message.MessageHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.jooq.Record;

import java.awt.*;
import java.sql.Timestamp;

public class FeedUpdater implements Runnable {

    private static final long UPDATE_INTERVAL = 1000 * 300; // Update every five minutes
    private long nextUpdateOn = -1;

    private Thread thread;

    private JDA jda;

    private KirBot bot;

    private boolean running = true;

    public FeedUpdater(JDA jda, KirBot kirBot) {
        this.jda = jda;
        this.nextUpdateOn = System.currentTimeMillis();
        this.bot = kirBot;

        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.setName("FeedUpdater");
        this.thread.start();
    }

    @Override
    public void run() {
        while (running) {
            if (System.currentTimeMillis() > nextUpdateOn) {
                nextUpdateOn = System.currentTimeMillis() + UPDATE_INTERVAL;
            } else {
                continue;
            }

            if (!running)
                continue;

            for (Record record : KirBot.DATABASE.create().select().from(Tables.FEEDS)) {
                update(record, true);
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setChannel(TextChannel channel, String feedUrl, BotGuild guild) {
        // TODO: 1/16/2017 If duplicate, remove the previous one. Perhapse redesign database schema
        int count = KirBot.DATABASE.create().fetchCount(Tables.FEEDS, Tables.FEEDS.CHANNEL.eq(channel.getId()));
        if(count >= 1){
            delete(channel);
        }
        KirBot.DATABASE.create().insertInto(Tables.FEEDS, Tables.FEEDS.GUILD_ID, Tables.FEEDS.CHANNEL, Tables.FEEDS.FEED_URL)
                .values(guild.getId(), channel.getId(), feedUrl).execute();
        update(channel.getId(), false);
    }

    public void delete(TextChannel channel){
        KirBot.DATABASE.create().deleteFrom(Tables.FEEDS).where(Tables.FEEDS.CHANNEL.eq(channel.getId())).execute();
    }

    public void stop() {
        this.running = false;
    }

    /**
     * Update the channel's RSS feed
     *
     * @param channel The channel
     */
    public void update(String channel, boolean post) {
        update(KirBot.DATABASE.create().select().from(Tables.FEEDS).where(Tables.FEEDS.CHANNEL.eq(channel)).fetchOne(), post);
    }

    private boolean alreadyPosted(String guid, Channel channel) {
        return KirBot.DATABASE.create().selectCount().from(Tables.POSTED_MESSAGES).where(Tables.POSTED_MESSAGES.GUID.eq(guid), Tables.POSTED_MESSAGES.CHANNEL.eq(channel.getId())).fetchOne(0, int.class) != 0;
    }

    private void update(Record record, boolean post) {
        BotGuild guild = this.bot.getGuildById(record.get(Tables.FEEDS.GUILD_ID));
        Timestamp lastError = record.get(Tables.FEEDS.RETRY_ON);
        if (lastError != null && lastError.after(new Timestamp(System.currentTimeMillis())))
            return;
        if (guild == null)
            return;
        TextChannel channel = guild.getGuild().getTextChannelById(record.get(Tables.FEEDS.CHANNEL));
        if (channel == null)
            return;
        try {
            String feed_url = record.get(Tables.FEEDS.FEED_URL);
            Feed feed = new RSSFeedParser(feed_url).readFeed();

            for (FeedMessage message : feed.getEntries()) {
                if (alreadyPosted(message.getGuid(), channel))
                    continue;
                KirBot.logger.info("Posting " + message.getGuid() + " to " + channel.getId());
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(message.getTitle());
                builder.setColor(Color.green);
                builder.addBlankField(false);
                builder.addField("Author", message.getAuthor(), true);
                builder.addBlankField(true);
                builder.addField("Summary", HTML2Md.convert(message.getDescription(), feed.getLink()).replace("#", ""), false);
                builder.addField("URL", "[" + message.getLink() + "](" + message.getLink() + ")", true);


                Message m = new MessageBuilder().setEmbed(builder.build()).build();
                if(post) {
                    channel.sendMessage("@everyone").queue();
                    channel.sendMessage(m).queue();
                }
                updatePostFeed(message.getGuid(), record.get(Tables.FEEDS.ID), channel.getId());
            }
        } catch (Exception e) {
            // Errored,
            KirBot.logger.warn("Error updating feed " + record.get(Tables.FEEDS.ID) + ", retrying in 1 hour");
            channel.sendMessage(MessageHandler.generateWarn("Could not update the feed `" + record.get(Tables.FEEDS.FEED_URL) + "`, will retry in 1 hour or when manually updated")).queue();
            channel.sendMessage("```" + e.getMessage() + "```").queue();
            KirBot.DATABASE.create().update(Tables.FEEDS).set(Tables.FEEDS.RETRY_ON, new Timestamp(System.currentTimeMillis() + 3600 * 1000)).where(Tables.FEEDS.ID.eq(record.get(Tables.FEEDS.ID))).execute();
        }
    }

    private void updatePostFeed(String guid, int feedId, String channel) {
        KirBot.DATABASE.create().insertInto(Tables.POSTED_MESSAGES, Tables.POSTED_MESSAGES.GUID, Tables.POSTED_MESSAGES.FEED, Tables.POSTED_MESSAGES.CHANNEL, Tables.POSTED_MESSAGES.POSTED_ON)
                .values(guid, feedId, channel, new Timestamp(System.currentTimeMillis())).execute();
    }
}
