package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.createMockedMember
import com.mrkirby153.kirbot.entity.DiscordUser
import com.mrkirby153.kirbot.entity.Infraction
import com.mrkirby153.kirbot.entity.repo.DiscordUserRepository
import com.mrkirby153.kirbot.entity.repo.InfractionRepository
import com.mrkirby153.kirbot.services.InfractionService
import com.mrkirby153.kirbot.services.setting.GuildSettings
import com.mrkirby153.kirbot.services.setting.SettingsService
import com.mrkirby153.kirbot.utils.nameAndDiscrim
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.requests.restaction.MessageAction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

@DataJpaTest
internal class InfractionManagerTest {

    @Autowired
    lateinit var repo: InfractionRepository

    @Autowired
    lateinit var seenUsers: DiscordUserRepository

    lateinit var manager: InfractionManager

    @MockK
    lateinit var settingsService: SettingsService

    @MockK
    lateinit var eventPublisher: ApplicationEventPublisher

    @MockK
    lateinit var selfMember: Member

    lateinit var user: User
    lateinit var issuer: User
    lateinit var guild: Guild


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        user = DiscordTestUtils.mockUser1
        issuer = DiscordTestUtils.mockUser2
        guild = DiscordTestUtils.mockGuild

        guild.createMockedMember(user)
        guild.createMockedMember(issuer)

        seenUsers.save(DiscordUser(user.id, user.name, user.discriminator.toInt(), false))
        seenUsers.save(DiscordUser(issuer.id, issuer.name, issuer.discriminator.toInt(), false))

        manager = spyk(InfractionManager(repo, eventPublisher, settingsService))

        every { guild.selfMember } returns selfMember
        every { selfMember.hasPermission(any<Permission>()) } returns true
    }

    private fun makeCtx(reason: String) = InfractionService.InfractionContext(user, guild,
            issuer, reason)

    private fun checkInfraction(user: User, type: Infraction.InfractionType) {
        val infractions = repo.getAllByUserIdAndGuild(user.id, guild.id)
        assertThat(infractions).isNotEmpty
        assertThat(infractions[0].type).isEqualTo(type)
    }

    private fun getInfraction(user: User): Infraction {
        val infs = repo.getAllByUserIdAndGuild(user.id, guild.id)
        assertThat(infs.size).isEqualTo(1)
        return infs.first()
    }

    @Test
    fun kick() {
        val inf = makeCtx("Testing")
        val restAction = DiscordTestUtils.completedAuditableRestAction<Void>(null)
        every { guild.kick(any<String>(), any()) } returns restAction
        val result = manager.kick(inf).get(1, TimeUnit.SECONDS)
        val uid = inf.user.id
        verify { guild.kick(eq(uid), any()) }
        assertThat(result.dmResult).isEqualTo(InfractionService.DmResult.NOT_SENT)
        checkInfraction(user, Infraction.InfractionType.KICK)
    }

    @Test
    fun kickNoPermission() {
        val inf = makeCtx("Testing")
        every { selfMember.hasPermission(Permission.KICK_MEMBERS) } returns false
        assertThrows<PermissionException> {
            manager.kick(inf)
        }
    }

    @Test
    fun kickDm() {
        val inf = makeCtx("[DM] Testing")
        val restAction = DiscordTestUtils.completedAuditableRestAction<Void>(null)
        every { guild.kick(any<String>(), any()) } returns restAction
        val privateChannel = mockk<PrivateChannel>()
        val privateChannelRa = DiscordTestUtils.completedRestAction(privateChannel)

        every { user.openPrivateChannel() } returns privateChannelRa
        every {
            privateChannel.sendMessage(any<Message>())
        } returns DiscordTestUtils.makeRestAction<Message, MessageAction>(mockk())

        val result = manager.kick(inf).get(1, TimeUnit.SECONDS)
        val uid = inf.user.id
        val nad = issuer.nameAndDiscrim
        verify { guild.kick(eq(uid), any()) }
        verify {
            privateChannel.sendMessage(match<Message> { msg ->
                msg.contentRaw.contains("Testing") && msg.contentRaw.contains("by $nad")
            })
        }
        assertThat(result.dmResult).isEqualTo(InfractionService.DmResult.SENT)
        checkInfraction(user, Infraction.InfractionType.KICK)
    }

    @Test
    fun kickAdm() {
        val inf = makeCtx("[ADM] Testing")
        val restAction = DiscordTestUtils.completedAuditableRestAction<Void>(null)
        every { guild.kick(any<String>(), any()) } returns restAction
        val privateChannel = mockk<PrivateChannel>()
        val privateChannelRa = DiscordTestUtils.completedRestAction(privateChannel)

        every { user.openPrivateChannel() } returns privateChannelRa
        every {
            privateChannel.sendMessage(any<Message>())
        } returns DiscordTestUtils.makeRestAction<Message, MessageAction>(mockk())

        val result = manager.kick(inf).get(1, TimeUnit.SECONDS)
        val uid = inf.user.id
        val nad = issuer.nameAndDiscrim
        verify { guild.kick(eq(uid), any()) }
        verify {
            privateChannel.sendMessage(match<Message> { msg ->
                msg.contentRaw.contains("Testing") && !msg.contentRaw.contains("by $nad")
            })
        }
        assertThat(result.dmResult).isEqualTo(InfractionService.DmResult.SENT)
        checkInfraction(user, Infraction.InfractionType.KICK)
    }

    @Test
    fun kickDmError() {
        val inf = makeCtx("[ADM] Testing")
        val restAction = DiscordTestUtils.completedAuditableRestAction<Void>(null)
        every { guild.kick(any<String>(), any()) } returns restAction
        val ex = mockk<ErrorResponseException>()
        every { ex.errorResponse } returns ErrorResponse.CANNOT_SEND_TO_USER
        every { user.openPrivateChannel() } throws ex
        val result = manager.kick(inf).get(1, TimeUnit.SECONDS)
        val uid = inf.user.id
        verify { guild.kick(eq(uid), any()) }
        assertThat(result.dmResult).isEqualTo(InfractionService.DmResult.SEND_ERROR)
        checkInfraction(user, Infraction.InfractionType.KICK)
    }

    @Test
    fun ban() {
        val inf = makeCtx("Testing")
        every {
            guild.ban(any<String>(), eq(0), any())
        } returns DiscordTestUtils.completedAuditableRestAction<Void>(null)
        assertThat(manager.ban(inf).get(1, TimeUnit.SECONDS).infraction).isNotNull
        checkInfraction(user, Infraction.InfractionType.BAN)
    }

    @Test
    fun tempBan() {
        val inf = makeCtx("Testing")
        every {
            guild.ban(any<String>(), eq(0), any())
        } returns DiscordTestUtils.completedAuditableRestAction<Void>(null)
        manager.tempBan(inf, 1, TimeUnit.DAYS).get(1, TimeUnit.SECONDS)
        val i = getInfraction(user)
        assertThat(i.expiresAt).isAfter(Date.from(Instant.now()))
        assertThat(i.type).isEqualTo(Infraction.InfractionType.TEMP_BAN)
    }

    @Test
    fun mute() {
        val inf = makeCtx("Testing")
        val role = mockk<Role>()
        every { manager.getMutedRole(any()) } returns role
        val uid = user.id
        every {
            guild.addRoleToMember(eq(uid), eq(role))
        } returns DiscordTestUtils.makeRestAction<Void, AuditableRestAction<Void>>()
        manager.mute(inf).get(1, TimeUnit.SECONDS)
        checkInfraction(user, Infraction.InfractionType.MUTE)
    }

    @Test
    fun muteNoMutedRole() {
        every { manager.getMutedRole(any()) } returns null
        assertThatThrownBy { manager.mute(makeCtx("Testing")).get(1, TimeUnit.SECONDS) }.hasMessageContaining("Muted role not found")
    }

    @Test
    fun muteNoMember() {
        every { guild.getMember(eq(user)) } returns null
        every { manager.getMutedRole(any()) } returns mockk()
        assertThatThrownBy { manager.mute(makeCtx("Testing")).get(1, TimeUnit.SECONDS) }.hasMessageContaining("Member not found")
    }

    @Test
    fun unmute() {
        val inf = makeCtx("Testing")
        val role = mockk<Role>()
        every { manager.getMutedRole(any()) } returns role
        val uid = user.id
        every {
            guild.removeRoleFromMember(eq(uid), eq(role))
        } returns DiscordTestUtils.makeRestAction<Void, AuditableRestAction<Void>>()
        manager.unmute(inf).get(1, TimeUnit.SECONDS)
    }

    @Test
    fun unmuteNoRole() {
        every { manager.getMutedRole(any()) } returns null
        assertThatThrownBy { manager.unmute(makeCtx("Testing")).get(1, TimeUnit.SECONDS) }.hasMessageContaining("Muted role not found")
    }

    @Test
    fun unmuteNoMember() {
        every { guild.getMember(eq(user)) } returns null
        every { manager.getMutedRole(any()) } returns mockk()
        assertThatThrownBy { manager.unmute(makeCtx("Testing")).get(1, TimeUnit.SECONDS) }.hasMessageContaining("Member not found")
    }

    @Test
    fun tempMute() {
        val inf = makeCtx("Testing")
        val role = mockk<Role>()
        every { manager.getMutedRole(any()) } returns role
        val uid = user.id
        every {
            guild.addRoleToMember(eq(uid), eq(role))
        } returns DiscordTestUtils.makeRestAction<Void, AuditableRestAction<Void>>()
        manager.tempMute(inf, 1, TimeUnit.DAYS).get(1, TimeUnit.SECONDS)
        val i = getInfraction(user)
        assertThat(i.type).isEqualTo(Infraction.InfractionType.TEMP_MUTE)
        assertThat(i.expiresAt).isAfter(Date.from(Instant.now()))
    }

    @Test
    fun warn() {
        val inf = makeCtx("Testing")
        manager.warn(inf).get(1, TimeUnit.SECONDS)
        val i = getInfraction(user)
        assertThat(i.type).isEqualTo(Infraction.InfractionType.WARN)
        assertThat(i.reason).isEqualTo(inf.reason)
    }


    @Test
    fun getMutedRole() {
        val role = mockk<Role>()
        val roleId = "555555555555555555"
        every { role.id } returns roleId
        every { settingsService.getSetting(eq(GuildSettings.mutedRole), eq(guild)) } returns roleId
        every { guild.getRoleById(eq(roleId)) } returns role
        assertThat(manager.getMutedRole(guild)).isEqualTo(role)
    }
}