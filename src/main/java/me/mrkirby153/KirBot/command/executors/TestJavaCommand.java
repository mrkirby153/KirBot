package me.mrkirby153.KirBot.command.executors;

import me.mrkirby153.KirBot.command.Command;
import me.mrkirby153.KirBot.command.CommandExecutor;

@Command(name = "java")
public class TestJavaCommand extends CommandExecutor {
    @Override
    public void execute() {
        System.out.println("This is a java command");
    }
}
