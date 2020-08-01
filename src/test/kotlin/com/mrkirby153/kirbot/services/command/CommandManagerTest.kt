package com.mrkirby153.kirbot.services.command

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.createMockedMember
import com.mrkirby153.kirbot.entity.CommandAlias
import com.mrkirby153.kirbot.entity.repo.CommandAliasRepository
import com.mrkirby153.kirbot.services.PermissionService
import com.mrkirby153.kirbot.services.command.context.CommandSender
import com.mrkirby153.kirbot.services.command.context.CurrentGuild
import com.mrkirby153.kirbot.services.command.context.Optional
import com.mrkirby153.kirbot.services.command.context.Parameter
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.SelfUser
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils

@SpringBootTest
internal class CommandManagerTest {
    @MockkBean
    private lateinit var shardManager: ShardManager

    @MockkBean
    private lateinit var permissionService: PermissionService

    @SpykBean
    private lateinit var cs: CommandService
    @Autowired
    private lateinit var demoCommands: DemoCommands

    @Autowired
    private lateinit var commandAliasRepository: CommandAliasRepository

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    var commandExecuted = false
    val commandParameters = mutableMapOf<String, Any?>()

    private lateinit var tree: CommandNode

    private val user = DiscordTestUtils.mockUser1
    private val guild = DiscordTestUtils.mockGuild
    private val channel = mockk<TextChannel>()

    @BeforeEach
    fun setUp() {
        // Clean up from the last test that was ran
        commandExecuted = false
        commandParameters.clear()
        tree = ReflectionTestUtils.getField(cs, "commandTree") as CommandNode
        ReflectionTestUtils.setField(demoCommands, "test", this)

        every { shardManager.getGuildById(guild.id) } returns guild
        every { user.isBot } returns false
        val jda = mockk<JDA>()
        val selfUser = mockk<SelfUser>()
        every { jda.selfUser } returns selfUser
        every { selfUser.id } returns "888888888888888888"
        every { shardManager.shards } returns listOf(jda)
        guild.createMockedMember(user)
        every { permissionService.getClearance(any(), any()) } returns 0

        val selfMember = guild.createMockedMember(selfUser)
        every { guild.selfMember } returns selfMember
        every { selfMember.hasPermission(any<TextChannel>(), any<Permission>()) } returns true

        every { channel.type } returns ChannelType.TEXT
        every { channel.guild } returns guild
        every { channel.sendMessage(any<Message>()) } returns mockk()
    }

    @Test
    fun testExecution() {
        val node = getCommand("test")
        cs.invoke(node, listOf("test", "name"), user, guild, channel)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<CommandSender>("user")!!.id).isEqualTo(user.id)
        assertThat(getCommandParam<String>("name")).isEqualTo("test name")
    }

    @Test
    fun testSubCommand() {
        val node = getCommand("sub.command")
        cs.invoke(node, listOf(), user, guild, channel)
        assertTrue(commandExecuted)
    }

    @Test
    fun testGuild() {
        val node = getCommand("guild")
        cs.invoke(node, listOf(), user, guild, channel)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<CurrentGuild>("guild")!!.id).isEqualTo(guild.id)
    }

    @Test
    fun testOptionalPresent() {
        val node = getCommand("optional")
        cs.invoke(node, listOf("testing"), user, guild, channel)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<String>("name")).isEqualTo("testing")
    }

    @Test
    fun testOptionalAbsent() {
        val node = getCommand("optional")
        cs.invoke(node, listOf(), user, guild, channel)
        assertTrue(commandExecuted)
        assertThat(getCommandParam<String>("name")).isNull()
    }

    @Test
    fun testUsageString() {
        fun assertUsage(path: String, expected: String) {
            assertThat(cs.getUsageString(getCommand(path))).isEqualTo(expected)
        }
        assertUsage("test", "<name>")
        assertUsage("sub.command", "")
        assertUsage("guild", "")
        assertUsage("optional", "[name]")
        assertUsage("mixed", "<one> [two]")
    }

    @Test
    fun testExecute() {
        cs.executeCommand("!test test", user, channel)
        assertThat(commandExecuted).isTrue()
    }

    @Test
    fun testExecuteMessage() {
        val msg = mockk<Message>()
        val rawMsg = "testing"
        every { msg.contentRaw } returns rawMsg
        every { msg.channel } returns channel
        every { msg.guild } returns guild
        val event = mockk<GuildMessageReceivedEvent>()
        every { event.author } returns user
        every { event.message } returns msg
        every { event.isWebhookMessage } returns false
        every { event.channel } returns channel
        eventPublisher.publishEvent(event)
        verify { cs.executeCommand(eq(rawMsg), eq(user), eq(channel)) }
    }

    @Test
    fun testExecuteBot() {
        val botUser = mockk<User>()
        val event = mockk<GuildMessageReceivedEvent>()
        every { event.author } returns botUser
        every { botUser.isBot } returns true
        every { event.isWebhookMessage } returns false

        eventPublisher.publishEvent(event)
        verify(inverse = true) { cs.executeCommand(any(), any(), any()) }
    }

    @Test
    fun testAlias() {
        cs.executeCommand("!test-alias", user, channel)
        assertThat(commandExecuted).isFalse()
        val alias = CommandAlias("1", guild.id, "test-alias", "clearance", 0)
        commandAliasRepository.save(alias)
        cs.executeCommand("!test-alias", user, channel)
        assertThat(commandExecuted).isTrue()
    }

    @Test
    fun testDefaultAlias() {
        val defaultAlias = CommandAlias("1", guild.id, "*", null, 10)
        commandAliasRepository.save(defaultAlias)
        cs.executeCommand("!default", user, channel)
        assertThat(commandExecuted).isFalse()
    }

    private fun getCommand(path: String): CommandNode {
        var node = tree
        path.split(".").forEach {
            val found = node.getChild(it) ?: throw IllegalArgumentException(
                    "Command node not found")
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

    @Command(name = "mixed", clearance = 0)
    fun mixedRequiredOptional(user: CommandSender, @Parameter("one") one: String,
                              @Parameter("two") @Optional two: String?) {
        test.commandExecuted = true
        test.commandParameters["one"] = one
        test.commandParameters["two"] = two
    }

    @Command(name = "clearance", clearance = 100)
    fun clearanceCommand() {
        test.commandExecuted = true
    }

    @Command(name = "default", clearance = 0)
    fun defaultClearance() {
        test.commandExecuted = true
    }
}