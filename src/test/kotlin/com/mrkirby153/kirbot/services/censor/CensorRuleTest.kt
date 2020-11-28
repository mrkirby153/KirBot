package com.mrkirby153.kirbot.services.censor

import com.mrkirby153.kirbot.DiscordTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Invite
import net.dv8tion.jda.api.entities.Message
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class CensorRuleTest {

    private lateinit var setting: CensorSetting


    fun check(rule: CensorRule, setting: CensorSetting, content: String): Boolean {
        val msg = mockk<Message>()
        every { msg.contentRaw } returns content
        try {
            rule.check(msg, setting)
        } catch (e: ViolationException) {
            return true
        }
        return false
    }

    @BeforeEach
    fun setup() {
        setting = CensorSetting(0, "test", true, emptyList(), emptyList(),
                CensorSetting.Invite(true, emptyList(), emptyList()),
                CensorSetting.Domains(true, emptyList(), emptyList()))
    }


    @Test
    fun testZalgo() {
        setting.zalgo = true
        val rule = ZalgoRule()
        assertTrue(check(rule, setting, "T̴̠͋ĕ̵͔s̷͎̈t̷͈̊"))
        assertFalse(check(rule, setting, "test"))
        setting.zalgo = false
        assertFalse(check(rule, setting, "T̴̠͋ĕ̵͔s̷͎̈t̷͈̊"))
        assertFalse(check(rule, setting, "test"))
    }

    @Test
    fun testTokens() {
        setting.blockedTokens = listOf("test")
        val rule = TokenRule()
        assertTrue(check(rule, setting, "testing"))
        assertTrue(check(rule, setting, "test"))
        assertFalse(check(rule, setting, "hello, world"))

        setting.blockedTokens = listOf("r:\\d+")
        assertTrue(check(rule, setting, "t3st"))
        assertFalse(check(rule, setting, "test"))
        assertFalse(check(rule, setting, "r:\\d+"))
        assertTrue(check(rule, setting, "3"))
    }

    @Test
    fun testWords() {
        setting.blockedWords = listOf("test")
        val rule = WordRule()
        assertTrue(check(rule, setting, "test"))
        assertTrue(check(rule, setting, "this is a test"))
        assertTrue(check(rule, setting, "test 1 2 3 4"))
        assertTrue(check(rule, setting, "I like to test things"))

        assertFalse(check(rule, setting, "testing"))
        assertFalse(check(rule, setting, "testing is what I like to do"))
        assertFalse(check(rule, setting, "so much testing"))
        assertFalse(check(rule, setting, "how much testing can you do"))

        setting.blockedWords = listOf("r:\\d+")
        assertTrue(check(rule, setting, "there are 3 tests to perform"))
        assertFalse(check(rule, setting, "there are3 tests to perform"))

        setting.blockedWords = listOf("$")
        assertTrue(check(rule, setting, "There is a $ in my test case"))
    }

    @Test
    fun testInvites() {
        val g = mockk<Guild>()
        every { g.id } returns "11111"

        val msg = mockk<Message>()
        every { msg.invites } returns listOf("12345")
        every { msg.jda } returns mockk()
        every { msg.guild } returns g

        val guild = mockk<Invite.Guild>()
        every { guild.id } returns "12345"
        every { guild.name } returns "Testing"
        val invite = mockk<Invite>()
        every { invite.guild } returns guild

        val rule = InviteRule()
        mockkStatic(Invite::class)
        every { Invite.resolve(any(), eq("12345")) } returns DiscordTestUtils.completedRestAction(
                invite)

        setting.invites.enabled = true
        setting.invites.blacklist = listOf("12345")
        assertThrows<ViolationException> {
            rule.check(msg, setting)
        }

        setting.invites.enabled = false
        assertDoesNotThrow {
            rule.check(msg, setting)
        }
        setting.invites.enabled = true

        setting.invites.blacklist = listOf()
        setting.invites.whitelist = listOf("6789")
        assertThrows<ViolationException> {
            rule.check(msg, setting)
        }
        every { guild.id } returns "6789"
        assertDoesNotThrow {
            rule.check(msg, setting)
        }
    }

    @Test
    fun testDomains() {
        val rule = DomainRule()

        setting.domains.enabled = true
        setting.domains.whitelist = listOf("example.com")

        assertFalse(check(rule, setting, "Test http://example.com Test"))
        assertTrue(check(rule, setting, "Test https://test.example.com Test"))

        setting.domains.whitelist = emptyList()
        setting.domains.blacklist = listOf("example.com")
        assertTrue(check(rule, setting, "https://example.com"))
    }
}