package me.mrkirby153.KirBot.command;

import me.mrkirby153.KirBot.database.generated.enums.CommandsType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.script.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class CustomCommand {

    private CommandsType type;

    private String name;
    private String data;

    public CustomCommand(CommandsType type, String name, String data) {
        this.type = type;
        this.name = name;
        this.data = data;
    }

    public void execute(TextChannel channel, Member sender, String[] args) {
        switch (type) {
            case TEXT:
                channel.sendMessage(data).queue();
                break;
            case JS:
                channel.sendMessage(executeJavascript(data, channel, sender, args)).queue();
        }
    }

    public String getName() {
        return name;
    }

    private String executeJavascript(String script, TextChannel channel, Member sender, String[] args) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

        List<EcmaVariable> var = new ArrayList<>();
        var.add(new EcmaVariable("channel", channel));
        var.add(new EcmaVariable("channelName", channel.getName()));
        var.add(new EcmaVariable("senderName", sender.getEffectiveName()));
        var.add(new EcmaVariable("senderAsMention", sender.getAsMention()));
        var.add(new EcmaVariable("sender", sender));
        var.add(new EcmaVariable("args", args));
        addJavaVariables(engine, var);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        engine.getContext().setWriter(pw);
        try {
            engine.eval(script);
            return sw.getBuffer().toString();
        } catch (ScriptException e) {
            return "An error occurred when executing the script: ```" + e.getMessage()+"```";
        }
    }

    private void addJavaVariables(ScriptEngine engine, List<EcmaVariable> variables) {
        Bindings bindings = new SimpleBindings();
        for (EcmaVariable v : variables) {
            bindings.put(v.getName(), v.getValue());
        }
        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
    }


    public static class EcmaVariable {
        private final String name;
        private final Object value;

        public EcmaVariable(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }
}
