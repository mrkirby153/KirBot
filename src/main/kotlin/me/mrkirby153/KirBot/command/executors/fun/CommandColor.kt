package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import java.awt.Color

@Command("color")
@RequiresClearance(Clearance.USER)
class CommandColor : BaseCommand(CommandCategory.FUN, Arguments.string("color")) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val colorString = cmdContext.get<String>("color")

        if (colorString.equals("reset", true)) {
            resetColor(context.author.getMember(context.guild))
            context.send().success("Reset your color").queue()
            return
        }

        try {
            val color = Color.decode(colorString)
            setColorRole(context, context.author.getMember(context.guild), color)
            context.send().success("Set your roleId color to `$colorString`").queue()
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
            context.guild.controller.createRole().queue { r ->
                context.guild.controller.addRolesToMember(member, r).queue()
                val m = r.managerUpdatable
                m.colorField.value = color
                m.nameField.value = "color-${member.user.id}"
                m.update().queue()
            }
        }
    }

    private fun resetColor(member: Member) {
        member.guild.getRolesByName("color-${member.user.id}", true).forEach { r ->
            r.delete().queue()
        }
    }
}