package me.mrkirby153.KirBot.utils;

import java.io.File;

public class BotConfiguration {

    /**
     * The Discord id of the bot creator
     */
    public static final String MRKIRBY_ID = "117791909786812423";

    /**
     * The API key when interfacing with the
     */
    public String discordAPIKey = "";

    /**
     * The location where data is stored
     */
    public String dataLocation = "data";

    public File getDataLocation(){
        File file = new File(dataLocation);
        if(!file.exists())
            file.mkdirs();
        return file;
    }

    public File dataStore(String file){
        return new File(getDataLocation(), file);
    }

}
