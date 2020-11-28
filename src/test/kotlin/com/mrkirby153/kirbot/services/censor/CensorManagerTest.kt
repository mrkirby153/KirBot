package com.mrkirby153.kirbot.services.censor

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.services.PermissionService
import com.mrkirby153.kirbot.services.setting.GuildSettings
import com.mrkirby153.kirbot.services.setting.SettingsService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils

internal class CensorManagerTest {

    lateinit var censorManager: CensorManager

    @MockK
    lateinit var settingService: SettingsService

    @MockK
    lateinit var permissionService: PermissionService

    @MockK
    lateinit var publisher: ApplicationEventPublisher

    lateinit var failingRule: CensorRule


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        censorManager = CensorManager(settingService, publisher, permissionService)

        failingRule = mockk()
        every { failingRule.check(any(), any()) } throws ViolationException("Failed")
        ReflectionTestUtils.setField(censorManager, "censorRules", listOf(failingRule))
    }

    @Test
    fun testCheckReceive() {
        val event = mockk<GuildMessageReceivedEvent>()
        every { event.author } returns DiscordTestUtils.mockUser1
        val jda = mockk<JDA>()
        every { jda.selfUser } returns mockk()
        every { event.jda } returns jda

        val channel = mockk<MessageChannel>()

        val msg = mockk<Message>()
        every { msg.contentRaw } returns "test"
        every { msg.channel } returns channel

        val deleteRa = DiscordTestUtils.completedAuditableRestAction<Void>(null)
        every { msg.delete() } returns deleteRa

        val guild = mockk<Guild>()
        every { msg.guild } returns guild
        every { msg.author } returns DiscordTestUtils.mockUser1
        every { guild.getMember(any()) } returns mockk()
        every { event.message } returns msg

        every { permissionService.getClearance(any()) } returns 0

        val rule = mockk<CensorSetting>()
        every { rule.level } returns 0
        every {
            settingService.getSetting(eq(GuildSettings.censorSettings), any())
        } returns arrayOf(rule)

        censorManager.onMessageReceived(event)
        verify { publisher.publishEvent(ofType(MessageCensorEvent::class)) }
    }

    @Test
    fun testCheckEdit() {
        val event = mockk<GuildMessageUpdateEvent>()
        every { event.author } returns DiscordTestUtils.mockUser1
        val jda = mockk<JDA>()
        every { jda.selfUser } returns mockk()
        every { event.jda } returns jda

        val channel = mockk<MessageChannel>()

        val msg = mockk<Message>()
        every { msg.contentRaw } returns "test"
        every { msg.channel } returns channel

        val deleteRa = DiscordTestUtils.completedAuditableRestAction<Void>(null)
        every { msg.delete() } returns deleteRa

        val guild = mockk<Guild>()
        every { msg.guild } returns guild
        every { msg.author } returns DiscordTestUtils.mockUser1
        every { guild.getMember(any()) } returns mockk()
        every { event.message } returns msg

        every { permissionService.getClearance(any()) } returns 0

        val rule = mockk<CensorSetting>()
        every { rule.level } returns 0
        every {
            settingService.getSetting(eq(GuildSettings.censorSettings), any())
        } returns arrayOf(rule)

        censorManager.onMessageEdit(event)
        verify { publisher.publishEvent(ofType(MessageCensorEvent::class)) }
    }

    @Test
    fun testEffectiveRules() {
        val mockRule1 = mockk<CensorSetting>()
        every { mockRule1.level } returns 100
        val mockRule2 = mockk<CensorSetting>()
        every { mockRule2.level } returns 0
        val guild = mockk<Guild>()
        val member = mockk<Member>()
        every { guild.getMember(any()) } returns member
        every { permissionService.getClearance(any()) } returns 10
        every {
            settingService.getSetting(eq(GuildSettings.censorSettings), any())
        } returns arrayOf(mockRule1, mockRule2)

        val rules = censorManager.getEffectiveRules(guild,
                DiscordTestUtils.mockUser1)

        assertThat(rules).contains(mockRule1)
        assertThat(rules).doesNotContain(mockRule2)
    }
}