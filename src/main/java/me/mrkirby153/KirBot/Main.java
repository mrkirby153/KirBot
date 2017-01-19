package me.mrkirby153.KirBot;

public class Main {

    public static void main(String[] args) {
        Bot.INSTANCE.start(Bot.INSTANCE.getProperties().getProperty("auth-token"));
    }
}
