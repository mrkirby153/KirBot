package me.mrkirby153.KirBot.command;

import me.mrkirby153.KirBot.user.Clearance;
import net.dv8tion.jda.core.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {

    String[] aliases() default {};

    Clearance clearance() default Clearance.USER;

    boolean deleteCallingMessage() default true;

    String description() default "No descrption provided";

    String name();

    Permission[] requiredPermissions() default {Permission.MESSAGE_WRITE};

}
