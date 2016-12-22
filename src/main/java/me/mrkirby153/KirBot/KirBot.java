package me.mrkirby153.KirBot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.mrkirby153.KirBot.utils.BotConfiguration;
import net.dv8tion.jda.core.JDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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


    }

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
        try{
            FileReader reader = new FileReader(CONFIG_LOCATION);

            this.configuration = GSON.fromJson(reader, BotConfiguration.class);

            reader.close();

            logger.info("Configuration file loaded successfully");
        } catch (IOException e){
            logger.error("Encountered an error when reading the configuration file", e);
        }
    }
}
