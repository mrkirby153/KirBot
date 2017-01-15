package me.mrkirby153.KirBot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jline.console.ConsoleReader;
import me.mrkirby153.KirBot.command.CommandHandler;
import me.mrkirby153.KirBot.command.commands.CommandClean;
import me.mrkirby153.KirBot.command.commands.CommandHelp;
import me.mrkirby153.KirBot.database.DatabaseHandler;
import me.mrkirby153.KirBot.database.generated.Tables;
import me.mrkirby153.KirBot.guild.BotGuild;
import me.mrkirby153.KirBot.utils.BotConfiguration;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Main Application class for the discord robot
 */
public class KirBot extends ListenerAdapter {

    /**
     * The location of the configuration file
     */
    private static final File CONFIG_LOCATION = new File("config.json");

    /**
     * The main Gson instance of the application
     */
    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * The main robot logger
     */
    public static Logger logger = LogManager.getLogger("KirBot");

    /**
     * The database
     */
    public static DatabaseHandler DATABASE;


    /**
     * The robot's configuration
     */
    public BotConfiguration configuration;

    /**
     * The main JDA instance of the robot
     */
    private JDA jda;

    /**
     * A list of guilds that the robot is a part of
     */
    private HashMap<String, BotGuild> guilds = new HashMap<>();

    /**
     * The command handler
     */
    public CommandHandler commandHandler;

    protected KirBot() {

    }

    /**
     * Gets the {@link BotGuild} from a {@link Guild}
     *
     * @param guild The guild
     * @return The {@link BotGuild}
     */
    public BotGuild getGuild(Guild guild) {
        return guilds.get(guild.getId());
    }

    public JDA getJda() {
        return jda;
    }

    /**
     * Gets the user id, or creates it if it doesn't exist
     *
     * @param user The user
     * @return The user's id
     */
    public int getOrCreateUser(User user) {
        if (getUserId(user) == -1)
            saveUser(user);
        return getUserId(user);
    }

    /**
     * Get the user's id
     *
     * @param user The user
     * @return The id, or -1 if it doesn't exist
     */
    public int getUserId(User user) {
        Record r = KirBot.DATABASE.create().select().from(Tables.USERS).where(Tables.USERS.DISCORD_ID.eq(user.getId())).fetchOne();
        if (r == null)
            return -1;
        return r.get(Tables.USERS.ID);
    }

    /**
     * Initialize the robot
     */
    public void initialize() {
        // Load configuration
        loadConfiguration();

        // Load database
        initializeDatabase();

        // Connect to Discord
        connectToDiscord();

        loadGuilds();

        registerCommands();

        logger.info("Initialization complete");
        // Initialize console
        initializeConsole();
    }

    /**
     * Load the guilds
     */
    public void loadGuilds() {
        this.guilds.clear();
        List<Record> results = DATABASE.create().select().from(Tables.GUILD).fetch();
        for (Record r : results) {
            int id = r.get(Tables.GUILD.ID);
            String guildId = r.get(Tables.GUILD.GUILD_ID);
            this.guilds.put(guildId, new BotGuild(id, guildId, jda));
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        logger.info("Joined guild " + event.getGuild().getName() + " (" + event.getGuild().getId() + ") registering");
        DATABASE.create().insertInto(Tables.GUILD, Tables.GUILD.GUILD_ID).values(event.getGuild().getId()).execute();
        loadGuilds();
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        logger.info("No longer a member of the guild " + event.getGuild().getName() + " (" + event.getGuild().getId() + ") removing from database");
        DATABASE.create().delete(Tables.GUILD).where(Tables.GUILD.GUILD_ID.eq(event.getGuild().getId())).execute();
        loadGuilds();
    }

    /**
     * Saves a user's id into the database
     *
     * @param user The user to save
     */
    public void saveUser(User user) {
        KirBot.DATABASE.create().insertInto(Tables.USERS, Tables.USERS.DISCORD_ID, Tables.USERS.USERNAME).values(user.getId(), user.getName()).execute();
    }

    /**
     * Shuts down the robot with exit code -99
     *
     * @param reason The reason why the robot is shutting down
     */
    public void shutdown(String reason) {
        shutdown(reason, -99);
    }

    /**
     * Shuts down the robot
     *
     * @param reason   The reason why the robot is shutting down
     * @param exitCode An exit code to terminate with
     */
    public void shutdown(String reason, int exitCode) {
        if (reason != null) {
            logger.info("Shutting down: " + reason);
        } else {
            logger.info("Shutting down");
        }

        if (jda != null) {
            logger.info("Disconnecting from Discord");
            jda.shutdown();
        }

        if (DATABASE != null) {
            logger.info("Disconnecting from the database");
            DATABASE.close();
        }

        logger.info("Good bye!");
        System.exit(exitCode);
    }

    /**
     * Connects the robot to discord
     */
    private void connectToDiscord() {
        logger.info("Connecting to Discord");
        if (this.configuration.discordAPIKey == null || this.configuration.discordAPIKey.isEmpty()) {
            logger.error("No API key is set, the robot will now shut down");
            this.shutdown("No API key set", -1);
        }
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(this.configuration.discordAPIKey).addListener(this).addListener(commandHandler = new CommandHandler(this)).buildBlocking();
        } catch (LoginException | InterruptedException | RateLimitedException e) {
            logger.error("Encountered an error when attempting to connect to Discord", e);
            shutdown("Error when connecting to Discord", -2);
        }
    }

    /**
     * Initializes the console for accepting commands
     */
    private void initializeConsole() {
        ConsoleReader reader;
        try {
            reader = new ConsoleReader();
            reader.setBellEnabled(false);
        } catch (IOException e) {
            logger.error("An error was encountered when initializing the console", e);
            shutdown("Error initializing console", -3);
            return;
        }

        String line;
        try {
            while ((line = reader.readLine("> ")) != null) {
                if (line.equalsIgnoreCase("shutdown")) {
                    shutdown(null, 0);
                }
            }
        } catch (IOException e) {
            logger.error("An error occurred when processing input from the console", e);
        }
    }

    private void initializeDatabase() {
        logger.info(String.format("Connecting to database `%s` at %s:%s with username %s", configuration.database, configuration.databaseHost, configuration.databasePort, configuration.databaseUsername));
        DATABASE = new DatabaseHandler(this, configuration.databaseHost, configuration.databasePort, configuration.databaseUsername, configuration.databasePassword, configuration.database);
        logger.info("Connected to database!");
    }

    /**
     * Loads the configuration from file
     */
    private void loadConfiguration() {
        logger.info("Loading configuration from " + CONFIG_LOCATION.getAbsolutePath());

        // Check if the configuration file exists
        if (!CONFIG_LOCATION.exists()) {
            logger.warn("The configuration file does not exist, creating...");

            BotConfiguration blankConfig = new BotConfiguration();
            String json = GSON.toJson(blankConfig);

            // Write the JSON
            try {
                FileWriter writer = new FileWriter(CONFIG_LOCATION);
                writer.write(json);
                writer.flush();
                writer.close();
                logger.info("Configuration file written successfully");
            } catch (IOException e) {
                logger.error("Encountered an error when creating the configuration file!", e);
            }
        }

        // Load the configuration
        try {
            FileReader reader = new FileReader(CONFIG_LOCATION);

            this.configuration = GSON.fromJson(reader, BotConfiguration.class);

            reader.close();

            logger.info("Configuration file loaded successfully");
        } catch (IOException e) {
            logger.error("Encountered an error when reading the configuration file", e);
        }
    }

    private void registerCommands() {
        logger.info("Registering commands...");
        CommandHandler.INSTANCE.registerCommand(new CommandHelp(), "help");
        CommandHandler.INSTANCE.registerCommand(new CommandClean(), "clean");
        logger.info("Commands registered!");
    }
}
