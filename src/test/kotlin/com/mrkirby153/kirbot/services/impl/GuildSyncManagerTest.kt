package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.createMockedMember
import com.mrkirby153.kirbot.entity.DiscordUser
import com.mrkirby153.kirbot.entity.guild.Channel
import com.mrkirby153.kirbot.entity.guild.repo.ChannelRepository
import com.mrkirby153.kirbot.entity.guild.repo.RoleRepository
import com.mrkirby153.kirbot.entity.repo.DiscordUserRepository
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.utils.concurrent.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@SpringBootTest
internal class GuildSyncManagerTest {

    @Autowired
    lateinit var syncManager: GuildSyncManager

    @Autowired
    lateinit var channelRepository: ChannelRepository

    @Autowired
    lateinit var discordUserRepository: DiscordUserRepository

    @Autowired
    lateinit var roleRepository: RoleRepository

    lateinit var guild: Guild

    @BeforeEach
    fun setup() {
        guild = DiscordTestUtils.mockGuild
    }


    @Test
    fun syncChannels() {
        // Existing
        channelRepository.save(Channel("1", guild.id, "Testing", Channel.Type.TEXT, false))
        // Deleted
        channelRepository.save(Channel("2", guild.id, "Deleted", Channel.Type.TEXT, false))
        val existingChannelMock = DiscordTestUtils.mockGuildChannel<TextChannel>("1", "Test", guild)
        val newChannelMock = DiscordTestUtils.mockGuildChannel<VoiceChannel>("3", "Voice", guild)

        val publicRole = mockk<Role>()
        val hiddenOverride = mockk<PermissionOverride>()
        val publicOverride = mockk<PermissionOverride>()

        every { hiddenOverride.denied } returns EnumSet.of(Permission.MESSAGE_READ)
        every { publicOverride.denied } returns EnumSet.noneOf(Permission::class.java)

        every { guild.channels } returns listOf(existingChannelMock, newChannelMock)
        every { guild.textChannels } returns listOf(existingChannelMock)
        every { guild.voiceChannels } returns listOf(newChannelMock)
        every { guild.publicRole } returns publicRole

        every { existingChannelMock.getPermissionOverride(any()) } returns hiddenOverride
        every { newChannelMock.getPermissionOverride(any()) } returns publicOverride

        assertThat(channelRepository.findById("3")).isNotPresent
        syncManager.syncChannels(guild).get(10, TimeUnit.SECONDS)
        assertThat(channelRepository.findById("2")).isNotPresent
        assertThat(channelRepository.findById(
                "1")).map { it.name }.get().isEqualToComparingFieldByField("Test")
        assertThat(channelRepository.findById("3")).isPresent
    }

    @Test
    fun syncRoles() {
        // Existing
        roleRepository.save(com.mrkirby153.kirbot.entity.guild.Role("1", guild.id, "Testing", 0, 0))
        // Deleted
        roleRepository.save(com.mrkirby153.kirbot.entity.guild.Role("2", guild.id, "Deleted", 0, 0))

        val existingRole = mockk<Role>()
        val newRole = mockk<Role>()

        every { existingRole.id } returns "1"
        every { existingRole.name } returns "Test"
        every { existingRole.permissionsRaw } returns 10
        every { existingRole.position } returns 0
        every { existingRole.guild } returns guild

        every { newRole.name } returns "new role"
        every { newRole.id } returns "3"
        every { newRole.permissionsRaw } returns 0
        every { newRole.position } returns 0
        every { newRole.guild } returns guild

        every { guild.roles } returns listOf(existingRole, newRole)
        val existingId = existingRole.id
        val newId = newRole.id
        every { guild.getRoleById(eq(existingId)) } returns existingRole
        every { guild.getRoleById(eq(newId)) } returns newRole

        syncManager.syncRoles(guild).get(10, TimeUnit.SECONDS)

        assertThat(roleRepository.findById("1")).map { it.name }.get().isEqualTo("Test")
        assertThat(roleRepository.findById("2")).isNotPresent
        assertThat(roleRepository.findById("3")).isPresent
    }

    @Test
    fun syncSeenUsers() {
        val existingUser = DiscordTestUtils.getMockedUser("12345", "Test User", "1234")
        discordUserRepository.save(DiscordUser(existingUser))

        val newUser = DiscordTestUtils.getMockedUser("6789", "New User", "12345")

        every { existingUser.name } returns "Renamed User"

        val members = mockTask(mutableListOf(guild.createMockedMember(existingUser),
                guild.createMockedMember(newUser)))
        every { guild.loadMembers() } returns members

        syncManager.syncSeenUsers(guild).get(10, TimeUnit.SECONDS)
        assertThat(discordUserRepository.findById(
                "12345")).map { it.username }.get().isEqualToComparingFieldByField("Renamed User")
        assertThat(discordUserRepository.findById("6789")).isPresent
    }

    /**
     * Creates a mocked task that runs [Task.onSuccess] with the given [result]
     */
    private fun <T> mockTask(result: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.onSuccess(any()) } answers {
            val callback = it.invocation.args[0] ?: throw IllegalStateException("No callback found")
            callback as Consumer<T>?
            callback.accept(result)
            task
        }
        every { task.onError(any()) } returns task
        return task
    }
}