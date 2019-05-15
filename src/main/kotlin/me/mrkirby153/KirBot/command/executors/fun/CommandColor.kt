package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import java.awt.Color


class CommandColor {

    @Command(name = "color", arguments = ["<color:string>"], category = CommandCategory.FUN)
    @CommandDescription("Sets a user's color in the member list")
    fun execute(context: Context, cmdContext: CommandContext) {
        val colorString = cmdContext.get<String>("color")

        val member = context.author.getMember(context.guild) ?: return
        if (colorString.equals("reset", true)) {
            resetColor(member)
            context.send().success("Reset your color", true).queue()
            return
        }

        try {
            val color = Color.decode(colorString)
            setColorRole(context, member, color)
            context.send().success("Set your color to `$colorString`", true).queue()
        } catch (e: NumberFormatException) {
            context.send().error("That is not a valid hexadecimal color!").queue()
        }
    }

    private fun setColorRole(context: Context, member: Member, color: Color) {
        var role: Role? = null

        context.guild.roles.forEach { r ->
            if (r.name.equals("color-${member.user.id}", true)) {
                role = r
                return@forEach
            }
        }
        if (role != null) {
            if(role !in member.roles){
                context.guild.controller.addRolesToMember(member, role).queue()
            }
            role?.manager?.setColor(color)?.queue()
        } else {
            context.guild.controller.createRole().setName("color-${member.user.id}").setColor(color).setPermissions(0).queue { r->
                context.guild.controller.addRolesToMember(member, r).queue()
            }
        }
    }

    private fun resetColor(member: Member) {
        member.guild.getRolesByName("color-${member.user.id}", true).forEach { r ->
            r.delete().queue()
        }
    }
}