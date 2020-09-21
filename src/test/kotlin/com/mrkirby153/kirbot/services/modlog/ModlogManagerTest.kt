package com.mrkirby153.kirbot.services.modlog

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.createMockedChannel
import com.mrkirby153.kirbot.entity.LogChannel
import com.mrkirby153.kirbot.entity.guild.LoggedMessage
import com.mrkirby153.kirbot.entity.guild.repo.AttachmentRepository
import com.mrkirby153.kirbot.entity.guild.repo.LoggedMessageRepository
import com.mrkirby153.kirbot.entity.repo.LogChannelRepository
import com.mrkirby153.kirbot.services.ArchiveService
import com.mrkirby153.kirbot.services.UserService
import com.mrkirby153.kirbot.utils.mockCompletedFuture
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.UriSelector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.core.task.TaskExecutor
import org.springframework.test.util.ReflectionTestUtils
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern.matches

@DataJpaTest
internal class ModlogManagerTest {

    @Autowired
    lateinit var logChannelRepository: LogChannelRepository
    @Autowired
    lateinit var loggedMessageRepository: LoggedMessageRepository

    @MockK
    lateinit var userService: UserService

    @MockK
    lateinit var archiveService: ArchiveService

    @MockK
    lateinit var shardManager: ShardManager

    @MockK
    lateinit var taskExecutor: TaskExecutor

    lateinit var modlogManager: ModlogManager

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        modlogManager = spyk(ModlogManager(taskExecutor, logChannelRepository, shardManager,
                loggedMessageRepository, userService, archiveService))
        val uid = DiscordTestUtils.mockUser1.id
        every { userService.findUser(eq(uid)) } returns mockCompletedFuture(
                UserService.User(DiscordTestUtils.mockUser1))
    }

    @Test
    fun cache() {
        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("12345",
                "test-channel")
        val chan1 = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("5678",
                "test-channel");
        logChannelRepository.save(LogChannel("1", DiscordTestUtils.mockGuild.id, chan.id, 0, 0))
        logChannelRepository.save(LogChannel("2", DiscordTestUtils.mockGuild.id, chan1.id, 10, 0))

        modlogManager.cache(DiscordTestUtils.mockGuild)

        val loggers = ReflectionTestUtils.getField(modlogManager,
                "channelLoggers") as ConcurrentHashMap<String, MutableList<ChannelLogger>>
        assertThat(loggers.size).isEqualTo(1)

        val chanLoggers = loggers[DiscordTestUtils.mockGuild.id]
        assertThat(chanLoggers!!.size).isEqualTo(2)
    }

    @Test
    fun updateCache() {
        fun verifyLogChannel(vararg subscriptions: LogEvent) {
            val loggers = ReflectionTestUtils.getField(modlogManager,
                    "channelLoggers") as ConcurrentHashMap<String, MutableList<ChannelLogger>>
            assertThat(loggers.size).isGreaterThan(0)

            val chanLoggers = loggers[DiscordTestUtils.mockGuild.id]!!
            assertThat(chanLoggers.isEmpty()).isEqualTo(false)
            val first = chanLoggers[0]
            assertThat(ReflectionTestUtils.getField(first, "subscriptions")).isEqualTo(
                    subscriptions.toSet())
        }

        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("12345",
                "test-channel")
        val lc = logChannelRepository.save(
                LogChannel("1", DiscordTestUtils.mockGuild.id, chan.id, 10, 0))

        modlogManager.cache(DiscordTestUtils.mockGuild)
        verifyLogChannel(*LogEvent.decode(10).toTypedArray())
        lc.included = 4
        logChannelRepository.save(lc)
        modlogManager.cache(DiscordTestUtils.mockGuild)
        verifyLogChannel(*LogEvent.decode(4).toTypedArray())
    }

    @Test
    fun log() {
        val logger = mockk<ChannelLogger>()
        every { logger.hasPendingMessages() } returns true
        every { logger.submit(any(), any()) } returns true

        val loggers = ConcurrentHashMap<String, MutableList<ChannelLogger>>()
        loggers[DiscordTestUtils.mockGuild.id] = mutableListOf(logger)
        ReflectionTestUtils.setField(modlogManager, "channelLoggers", loggers)
        modlogManager.log(LogEvent.MESSAGE_DELETE, DiscordTestUtils.mockGuild, "Testing")
        verify { logger.submit("Testing", LogEvent.MESSAGE_DELETE) }
    }

    @Test
    fun logHushed() {
        val logger = mockk<ChannelLogger>()
        every { logger.hasPendingMessages() } returns false
        every { logger.submit(any(), any()) } returns true

        val loggers = ConcurrentHashMap<String, MutableList<ChannelLogger>>()
        loggers[DiscordTestUtils.mockGuild.id] = mutableListOf(logger)
        ReflectionTestUtils.setField(modlogManager, "channelLoggers", loggers)

        modlogManager.log(LogEvent.MESSAGE_DELETE, DiscordTestUtils.mockGuild, "Unhushed")
        verify { logger.submit("Unhushed", LogEvent.MESSAGE_DELETE) }

        modlogManager.hush(DiscordTestUtils.mockGuild, true)
        modlogManager.log(LogEvent.MESSAGE_DELETE, DiscordTestUtils.mockGuild, "Hushed")
        modlogManager.log(LogEvent.USER_JOIN, DiscordTestUtils.mockGuild, "Join")
        verify {
            logger.submit("Join", LogEvent.USER_JOIN)
        }
        verify(inverse = true) {
            logger.submit("Hushed", LogEvent.MESSAGE_DELETE)
        }
    }

    @Test
    fun guildLeave() {
        val logger = mockk<ChannelLogger>()
        every { logger.hasPendingMessages() } returns false
        every { logger.submit(any(), any()) } returns true

        val loggers = ConcurrentHashMap<String, MutableList<ChannelLogger>>()
        loggers[DiscordTestUtils.mockGuild.id] = mutableListOf(logger)
        ReflectionTestUtils.setField(modlogManager, "channelLoggers", loggers)

        val event = mockk<GuildLeaveEvent>()
        every { event.guild } returns DiscordTestUtils.mockGuild

        modlogManager.onGuildLeave(event)

        assertThat(ReflectionTestUtils.getField(modlogManager,
                "channelLoggers") as ConcurrentHashMap<String, *>).doesNotContainKey(
                DiscordTestUtils.mockGuild.id)
    }


    @Test
    fun messageSend() {
        val msg = mockk<Message>()
        every { msg.id } returns "1"
        every { msg.author } returns DiscordTestUtils.mockUser1
        every { msg.contentRaw } returns "Test Message"
        every { msg.guild } returns DiscordTestUtils.mockGuild
        every { msg.channel } returns DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>(
                "2", "testing")

        val attachment = mockk<Message.Attachment>()
        every { attachment.url } returns "http://example.com"

        every { msg.attachments } returns listOf(attachment)

        val evt = mockk<GuildMessageReceivedEvent>()
        every { evt.message } returns msg

        modlogManager.onMessageSend(evt)

        assertThat(loggedMessageRepository.findById("1")).hasValueSatisfying { loggedMsg ->
            assertThat(loggedMsg.id).isEqualTo("1")
            assertThat(loggedMsg.attachments).isNotNull
            assertThat(loggedMsg.attachments!!.attachments[0]).isEqualTo("http://example.com")
        }
    }

    @Test
    fun messageEdit() {
        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("3", "testing")
        every { shardManager.getGuildChannelById(any<String>()) } returns chan

        val loggedMessage = loggedMessageRepository.save(
                LoggedMessage("1", DiscordTestUtils.mockGuild.id, DiscordTestUtils.mockUser1.id,
                        chan.id, "Logged Message"))

        val msg = mockk<Message>()
        every { msg.id } returns loggedMessage.id!!
        every { msg.contentRaw } returns "Updated Message"

        val evt = mockk<GuildMessageUpdateEvent>()
        every { evt.message } returns msg
        every { evt.messageId } returns msg.id
        every { evt.guild } returns DiscordTestUtils.mockGuild

        modlogManager.onMessageEdit(evt)
        assertThat(loggedMessageRepository.findById(
                loggedMessage.id!!)).hasValueSatisfying { loggedMsg ->
            assertThat(loggedMsg.editCount).isEqualTo(1)
            assertThat(loggedMsg.message).isEqualTo("Updated Message")
        }
        verify {
            modlogManager.log(eq(LogEvent.MESSAGE_EDIT), any(), match {
                it.contains("Logged Message")   // The message before
                it.contains("Updated Message")  // The message after
            })
        }
    }

    @Test
    fun messageEditNoLog() {
        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("3", "testing")
        every { shardManager.getGuildChannelById(any<String>()) } returns chan

        val msg = mockk<Message>()
        every { msg.id } returns "5"
        every { msg.contentRaw } returns "Updated Message"

        val evt = mockk<GuildMessageUpdateEvent>()
        every { evt.message } returns msg
        every { evt.messageId } returns msg.id
        every { evt.guild } returns DiscordTestUtils.mockGuild

        modlogManager.onMessageEdit(evt)
        verify(inverse = true) {
            modlogManager.log(eq(LogEvent.MESSAGE_EDIT), any(), any())
        }
    }

    @Test
    fun messageDelete() {
        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("3", "testing")
        every { shardManager.getGuildChannelById(any<String>()) } returns chan

        var loggedMessage = LoggedMessage("1", DiscordTestUtils.mockGuild.id,
                DiscordTestUtils.mockUser1.id,
                chan.id, "Logged Message")
        loggedMessage.attachments = LoggedMessage.MessageAttachments(loggedMessage.id!!, "https://example.com")
        loggedMessage = loggedMessageRepository.save(loggedMessage)

        val msg = mockk<Message>()
        every { msg.id } returns loggedMessage.id!!

        val evt = mockk<GuildMessageDeleteEvent>()
        every { evt.messageId } returns msg.id
        every { evt.guild } returns DiscordTestUtils.mockGuild

        modlogManager.onMessageDelete(evt)
        assertThat(loggedMessageRepository.findById(
                loggedMessage.id!!)).hasValueSatisfying { loggedMsg ->
            assertThat(loggedMsg.deleted).isTrue()
        }
        verify {
            modlogManager.log(eq(LogEvent.MESSAGE_DELETE), any(), match {
                it.contains("Logged Message")       // The message
                it.contains("https://example.com")  // The message's attachments
            })
        }
    }

    @Test
    fun messageDeleteNoLog() {
        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("3", "testing")
        every { shardManager.getGuildChannelById(any<String>()) } returns chan

        val msg = mockk<Message>()
        every { msg.id } returns "4"
        every { msg.contentRaw } returns "Deleted Message"

        val evt = mockk<GuildMessageDeleteEvent>()
        every { evt.messageId } returns msg.id
        every { evt.guild } returns DiscordTestUtils.mockGuild

        modlogManager.onMessageDelete(evt)
        verify(inverse = true) {
            modlogManager.log(eq(LogEvent.MESSAGE_DELETE), any(), any())
        }
    }

    @Test
    fun messageBulkDelete() {
        val chan = DiscordTestUtils.mockGuild.createMockedChannel<TextChannel>("3", "testing")

        fun makeMessage(id: String, msg: String): LoggedMessage {
            return loggedMessageRepository.save(
                    LoggedMessage(id, DiscordTestUtils.mockGuild.id, DiscordTestUtils.mockUser1.id,
                            chan.id, msg))
        }

        every { shardManager.getGuildChannelById(any<String>()) } returns chan
        every { userService.findUsers(any()) } returns mockCompletedFuture(listOf(UserService.User(DiscordTestUtils.mockUser1)))
        every { archiveService.uploadToArchive(any()) } returns mockCompletedFuture(
                URL("https://example.com"))

        val evt = mockk<MessageBulkDeleteEvent>()
        val msg1 = makeMessage("11", "Message 1")
        val msg2 = makeMessage("12", "Message 2")
        every { evt.messageIds } returns listOf(msg1.id, msg2.id)
        every { evt.guild} returns DiscordTestUtils.mockGuild
        every { evt.channel } returns chan

        modlogManager.onMessageBulkDelete(evt)

        verify {
            modlogManager.log(eq(LogEvent.MESSAGE_BULKDELETE), any(), match {
                it.contains("https://example.com")
            })
        }
        verify {
            archiveService.uploadToArchive(match {
                it.contains("Message 1")    // The first message's content
                it.contains("Message 2")    // The 2nd message's content
            })
        }
    }

}