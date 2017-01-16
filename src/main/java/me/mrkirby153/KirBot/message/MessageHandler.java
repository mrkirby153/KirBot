package me.mrkirby153.KirBot.message;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.*;

/**
 * Handle message-related tasks
 */
public class MessageHandler implements Runnable {

    private static HashMap<Message, Long> deleteQueue = new HashMap<>();
    private static MessageHandler INSTANCE;
    private JDA jda;
    private Thread thread;

    private MessageHandler(JDA jda) {
        this.jda = jda;
        this.thread = new Thread(this);
        this.thread.start();
    }

    /**
     * Generate an error message
     *
     * @param message The message
     * @return The formatted error message
     */
    public static String generateError(String message) {
        return String.format(":no_entry: %s :no_entry:", message);
    }

    /**
     * Generate a success message
     *
     * @param message The message
     * @return The formatted success message
     */
    public static String generateSuccess(String message) {
        return String.format(":white_check_mark: %s :white_check_mark:", message);
    }

    /**
     * Generate a warning message
     *
     * @param message The message
     * @return The formatted message
     */
    public static String generateWarn(String message) {
        return String.format(":warning: %s :warning:", message);
    }

    public static void init(JDA jda) {
        if (INSTANCE == null)
            INSTANCE = new MessageHandler(jda);
    }

    /**
     * Queues a message for deletion
     *
     * @param message   The message to queue
     * @param expiresIn The amount of time (in ms) before the message is deleted
     */
    public static void queue(Message message, long expiresIn) {
        deleteQueue.put(message, System.currentTimeMillis() + expiresIn);
    }

    @Override
    public void run() {
        while (true) {
            HashMap<Channel, List<Message>> messages = new HashMap<>();
            HashSet<Message> toDelete = new HashSet<>();
            for (Map.Entry<Message, Long> e : deleteQueue.entrySet()) {
                long l = e.getValue() - System.currentTimeMillis();
                if (l > 0)
                    continue;
                List<Message> mess = messages.get(e.getKey().getTextChannel());
                if (mess == null)
                    mess = new ArrayList<>();

                mess.add(e.getKey());
                messages.put(e.getKey().getTextChannel(), mess);
                toDelete.add(e.getKey());
            }

            for (Map.Entry<Channel, List<Message>> e : messages.entrySet()) {
                Channel channel = e.getKey();
                if (!(channel instanceof TextChannel))
                    continue;
                TextChannel textChannel = (TextChannel) channel;
                List<Message> message = e.getValue();
                // Verify permission
                if (textChannel.getGuild().getMember(jda.getSelfUser()).hasPermission(textChannel, Permission.MESSAGE_MANAGE)) {
                    if (message.size() >= 2) {
                        textChannel.deleteMessages(message).queue();
                    } else {
                        message.get(0).deleteMessage().queue();
                    }
                } else {
                    System.out.println("[ERROR] No Permission");
                }
            }
            deleteQueue.entrySet().removeIf(e -> toDelete.contains(e.getKey()));
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
