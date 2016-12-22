package me.mrkirby153.KirBot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jline.console.ConsoleReader;
import me.mrkirby153.KirBot.utils.BotConfiguration;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Main Application class for the discord robot
 */
public class KirBot {

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
     * The robot's configuration
     */
    public BotConfiguration configuration;

    /**
     * The main JDA instance of the robot
     */
    private JDA jda;

    protected KirBot() {

    }

    /**
     * Initialize the robot
     */
    public void initialize() {
        // Load configuration
        loadConfiguration();

        // Connect to Discord
        connectToDiscord();


        logger.info("Initialization complete");
        // Initialize console
        initializeConsole();
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

        logger.info("Good bye!");
        System.exit(exitCode);
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
     * Connects the robot to discord
     */
    private void connectToDiscord() {
        logger.info("Connecting to Discord");
        if (this.configuration.discordAPIKey == null || this.configuration.discordAPIKey.isEmpty()) {
            logger.error("No API key is set, the robot will now shut down");
            this.shutdown("No API key set", -1);
        }
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(this.configuration.discordAPIKey).buildBlocking();
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
}
