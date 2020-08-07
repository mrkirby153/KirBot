package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.entity.guild.Channel
import com.mrkirby153.kirbot.entity.guild.repo.ChannelRepository
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.EnumSet

@SpringBootTest
internal class GuildSyncManagerTest {

    @Autowired
    lateinit var syncManager: GuildSyncManager

    @Autowired
    lateinit var channelRepository: ChannelRepository

    lateinit var guild: Guild

    @BeforeEach
    fun setup() {
        guild = DiscordTestUtils.mockGuild

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
        every { guild.publicRole } returns publicRole

        every { existingChannelMock.getPermissionOverride(any()) } returns hiddenOverride
        every { newChannelMock.getPermissionOverride(any()) } returns publicOverride
    }


    @Test
    fun syncChannels() {
        assertThat(channelRepository.findById("3")).isNotPresent
        syncManager.syncChannels(guild).get()
        assertThat(channelRepository.findById("2")).isNotPresent
        assertThat(channelRepository.findById("1")).map { it.name }.get().isEqualToComparingFieldByField("Test")
        assertThat(channelRepository.findById("3")).isPresent
    }
}