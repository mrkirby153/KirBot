package me.mrkirby153.KirBot.rss;

/**
 * Represents a message in the RSS feed
 */
public class FeedMessage {

    private String title;
    private String description;
    private String link;
    private String author;
    private String guid;

    public FeedMessage(String title, String description, String link, String author, String guid) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.author = author;
        this.guid = guid;
    }

    /**
     * Gets the author of the message
     *
     * @return The message
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Gets the description of the message
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the GUID of the message
     *
     * @return The GUID
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Gets the link for the rss message
     *
     * @return The link
     */
    public String getLink() {
        return link;
    }

    /**
     * Gets the title of the message
     *
     * @return The title
     */
    public String getTitle() {
        return title;
    }
}
