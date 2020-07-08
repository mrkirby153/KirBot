package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.createMockedMember
import com.mrkirby153.kirbot.createMockedRole
import com.mrkirby153.kirbot.entity.RoleClearance
import com.mrkirby153.kirbot.entity.repo.PanelUserRepository
import com.mrkirby153.kirbot.entity.repo.RoleClearanceRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.TimeUnit

@DataJpaTest
internal class PermissionManagerTest {

    private lateinit var manager: PermissionManager

    @Autowired
    private lateinit var panelUserRepository: PanelUserRepository

    @Autowired
    private lateinit var roleClearanceRepository: RoleClearanceRepository

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(javaClass)
        manager = PermissionManager(panelUserRepository, roleClearanceRepository)
    }

    @Test
    fun getClearance() {
        val mockUser = DiscordTestUtils.mockUser1
        val mockGuild = DiscordTestUtils.mockGuild
        mockGuild.createMockedMember(mockUser)

        assertThat(manager.getClearance(mockUser, mockGuild)).isEqualTo(0)
    }

    @Test
    fun getClearanceNotInGuild() {
        val user = DiscordTestUtils.mockUser1
        val guild = DiscordTestUtils.mockGuild
        every { guild.getMember(any()) } returns null

        assertThat(manager.getClearance(user, guild)).isEqualTo(0)
    }

    @Test
    fun getClearanceOwner() {
        val user = DiscordTestUtils.mockUser1
        val guild = DiscordTestUtils.mockGuild
        val member = guild.createMockedMember(user)
        every { member.isOwner } returns true

        assertThat(manager.getClearance(user, guild)).isEqualTo(DEFAULT_CLEARANCE_ADMIN)
    }

    @Test
    fun getClearanceRoles() {
        val user = DiscordTestUtils.mockUser1
        val guild = DiscordTestUtils.mockGuild
        val member = guild.createMockedMember(user)

        val role1 = guild.createMockedRole("12345", "Role 1")
        val role2 = guild.createMockedRole("12345", "Role 2")

        val role1Clearance = RoleClearance("1", guild.id, role1.id, 1L)
        val role2Clearance = RoleClearance("2", guild.id, role2.id, 2L)
        roleClearanceRepository.saveAll(listOf(role1Clearance, role2Clearance))

        every { member.roles } returns listOf(role1, role2)

        assertThat(manager.getClearance(member)).isEqualTo(2L)
    }

    @Test
    fun overrideClearance() {
        val user = DiscordTestUtils.mockUser1
        val guild = DiscordTestUtils.mockGuild
        val member = guild.createMockedMember(user)

        assertThat(manager.getClearance(user, guild)).isEqualTo(0)
        manager.overrideClearance(user, 2, TimeUnit.SECONDS)
        assertThat(manager.getClearance(user, guild)).isEqualTo(Long.MAX_VALUE)
        assertThat(manager.getClearance(member)).isEqualTo(Long.MAX_VALUE)
        await().atMost(5, TimeUnit.SECONDS).until { manager.getClearance(user, guild) == 0L }
    }

    @Test
    fun clearOverriddenClearance() {
        val user = DiscordTestUtils.mockUser1
        val guild = DiscordTestUtils.mockGuild
        guild.createMockedMember(user)

        assertThat(manager.getClearance(user, guild)).isEqualTo(0)
        manager.overrideClearance(user, 2, TimeUnit.MINUTES)
        assertThat(manager.getClearance(user, guild)).isEqualTo(Long.MAX_VALUE)
        manager.clearOverriddenClearance(user)
        assertThat(manager.getClearance(user, guild)).isEqualTo(0)
    }

    @Test
    fun isGlobalAdmin() {
        val user = DiscordTestUtils.mockUser1
        ReflectionTestUtils.setField(manager, "globalAdmins", listOf(user.id))

        assertThat(manager.isGlobalAdmin(user)).isTrue()
    }
}