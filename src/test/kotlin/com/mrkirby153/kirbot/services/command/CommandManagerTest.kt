package com.mrkirby153.kirbot.services.command

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.services.command.context.CommandSender
import com.mrkirby153.kirbot.services.command.context.CurrentGuild
import com.mrkirby153.kirbot.services.command.context.Optional
import com.mrkirby153.kirbot.services.command.context.Parameter
import com.mrkirby153.kirbot.services.command.context.Single
import com.ninjasquad.springmockk.MockkBean
import io.lettuce.core.dynamic.annotation.Param
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.test.util.ReflectionTestUtils
import java.lang.IllegalArgumentException

@SpringBootTest
internal class CommandManagerTest {
    @MockkBean
    private lateinit var shardManager: ShardManager

    @Autowired
    private lateinit var cs: CommandService
    @Autowired
    private lateinit var demoCommands: DemoCommands

    var commandExecuted = false
    val commandParameters = mutableMapOf<String, Any?>()

    private lateinit var tree: CommandNode

    private val user = DiscordTestUtils.mockUser1
    private val guild = DiscordTestUtils.mockGuild

    @BeforeEach
    fun setUp() {
        commandExecuted = false
        commandParameters.clear()
        tree = ReflectionTestUtils.getField(cs, "commandTree") as CommandNode
        ReflectionTestUtils.setField(demoCommands, "test", this)

        every { shardManager.getGuildById(guild.id) } returns guild
    }

    @Test
    fun testExecution() {
        val node = getCommand("test")
        cs.invoke(node, listOf("test", "name"), user, guild)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<CommandSender>("user")!!.id).isEqualTo(user.id)
        assertThat(getCommandParam<String>("name")).isEqualTo("test name")
    }

    @Test
    fun testSubCommand() {
        val node = getCommand("sub.command")
        cs.invoke(node, listOf(), user, guild)
        assertTrue(commandExecuted)
    }

    @Test
    fun testGuild() {
        val node = getCommand("guild")
        cs.invoke(node, listOf(), user, guild)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<CurrentGuild>("guild")!!.id).isEqualTo(guild.id)
    }

    @Test
    fun testOptionalPresent() {
        val node = getCommand("optional")
        cs.invoke(node, listOf("testing"), user, guild)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<String>("name")).isEqualTo("testing")
    }

    @Test
    fun testOptionalAbsent() {
        val node = getCommand("optional")
        cs.invoke(node, listOf(), user, guild)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<String>("name")).isNull()
    }

    private fun getCommand(path: String): CommandNode {
        var node = tree
        path.split(".").forEach {
            val found = node.getChild(it) ?: throw IllegalArgumentException("Command node not found")
            node = found
        }
        return node
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCommandParam(name: String) = commandParameters[name] as? T
}

@Commands
internal class DemoCommands {

    lateinit var test: CommandManagerTest

    @Command(name = "test", clearance = 0)
    fun testCommand(user: CommandSender, @Parameter("name") name: String) {
        test.commandExecuted = true
        test.commandParameters["user"] = user
        test.commandParameters["name"] = name
    }

    @Command(name = "sub command", clearance = 0)
    fun subCommand() {
        test.commandExecuted = true
    }

    @Command(name = "guild", clearance = 0)
    fun commandWithGuild(guild: CurrentGuild) {
        test.commandExecuted = true
        test.commandParameters["guild"] = guild
    }

    @Command(name = "optional", clearance = 0)
    fun optionalCommand(@Parameter("name") @Optional name: String?) {
        test.commandExecuted = true
        test.commandParameters["name"] = name
    }
}