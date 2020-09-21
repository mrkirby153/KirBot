package com.mrkirby153.kirbot

import com.ninjasquad.springmockk.isMock
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

object DiscordTestUtils {

    val mockUser1
        inline get() = getMockedUser("111111111111111111", "Mock User 1", "0001")

    val mockUser2
        inline get() = getMockedUser("222222222222222222", "Mock user 2", "0001")

    val mockGuild
        inline get() = getMockedGuild("333333333333333333", "Mock Guild 1")


    /**
     * Gets a mocked user
     *
     * @param id            The id of the user
     * @param username      The username of the user
     * @param discriminator The Discriminator of the user
     * @return The mocked user
     */
    fun getMockedUser(id: String, username: String, discriminator: String): User {
        val mock = mockk<User>()
        every { mock.name } returns username
        every { mock.discriminator } returns discriminator
        every { mock.id } returns id
        every { mock.idLong } returns id.toLong()
        every { mock.isBot } returns false
        every { mock.timeCreated } returns OffsetDateTime.now()
        return mock
    }

    /**
     * Gets a mocked guild
     *
     * @param id    The id of the guild
     * @param name  The name of the guild
     */
    fun getMockedGuild(id: String, name: String): Guild {
        val mock = mockk<Guild>()
        every { mock.name } returns name
        every { mock.id } returns id
        every { mock.idLong } returns id.toLong()
        every { mock.roles } returns emptyList()
        every { mock.textChannels } returns emptyList()
        every { mock.voiceChannels } returns emptyList()
        return mock
    }

    /**
     * Gets a completed rest action. This rest action returns immediately
     *
     * @param result The result of the rest action
     * @return The completed mocked rest action
     */
    fun <T> completedRestAction(result: T? = null): RestAction<T> = makeRestAction(result)

    /**
     * Gets a completed auditable rest action. This rest action returns immediately
     *
     * @param result The result of the rest action
     * @return The completed mocked auditable rest action
     */
    fun <T> completedAuditableRestAction(
            result: T? = null): AuditableRestAction<T> = makeRestAction(result)

    /**
     * Mocks a generic completed [RestAction]
     *
     * @param result The result of the rest action
     * @return The completed rest action
     */
    inline fun <RESULT, reified CLASS : RestAction<RESULT>> makeRestAction(
            result: RESULT? = null): CLASS {
        val mock = mockk<CLASS>()
        buildRestAction(mock, result)
        return mock
    }


    /**
     * Builds a completed rest action, mocking the call functions to return
     *
     * @param mock The mock to stub out
     * @param result The result of the rest action
     */
    fun <T> buildRestAction(mock: RestAction<T>, result: T?) {
        if (!mock.isMock)
            throw IllegalArgumentException("Attempting to mock a non-mocked rest action")
        every { mock.submit() } returns CompletableFuture.completedFuture(result)
        every { mock.queue(any()) } answers {
            val func = it.invocation.args[0] ?: return@answers Unit
            func as Consumer<T?>
            func.accept(result)
        }
    }

    /**
     * Builds a mocked [GuildChannel] with the given [id] and [name] in the given [guild]
     */
    inline fun <reified T : GuildChannel> mockGuildChannel(id: String, name: String,
                                                           guild: Guild): T {
        val mock = mockk<T>()
        every { mock.id } returns id
        every { mock.idLong } returns id.toLong()
        every { mock.name } returns name
        every { mock.guild } returns guild
        // Mock out the guild calls
        if (guild.isMock) {
            every { guild.getGuildChannelById(id) } returns mock
            if (mock is TextChannel) {
                every { guild.getTextChannelById(id) } returns mock
            }
            if (mock is VoiceChannel) {
                every { guild.getVoiceChannelById(id) } returns mock
            }
        }
        return mock
    }

}

/**
 * Creates a mocked member on the guild
 *
 * @param user  The user to create the member for
 * @return The mocked member
 */
fun Guild.createMockedMember(user: User): Member {
    val mocked = mockk<Member>()
    every { mocked.id } returns user.id
    every { mocked.idLong } returns user.idLong
    every { mocked.user } returns user
    every { mocked.isOwner } returns false
    every { mocked.guild } returns this
    every { mocked.roles } returns emptyList()

    if (this.isMock) {
        every { this@createMockedMember.getMember(user) } returns mocked
        every { this@createMockedMember.isMember(user) } returns true
    }
    return mocked
}

fun Guild.createMockedRole(id: String, name: String): Role {
    val mocked = mockk<Role>()
    every { mocked.id } returns id
    every { mocked.idLong } returns id.toLong()
    every { mocked.name } returns name

    if (this.isMock) {
        every { this@createMockedRole.getRoleById(id) } returns mocked
        val roles = mutableListOf<Role>()
        roles.addAll(this.roles)
        roles.add(mocked)
        every { this@createMockedRole.roles } returns roles
    }
    return mocked
}

inline fun <reified T : GuildChannel> Guild.createMockedChannel(id: String, name: String): T {
    val mock = mockk<T>()
    every { mock.id } returns id
    every { mock.idLong } returns id.toLong()
    every { mock.name } returns name
    every { mock.guild } returns this

    if (this.isMock) {
        if (mock is TextChannel) {
            every { this@createMockedChannel.getTextChannelById(id) } returns mock
            val chans = mutableListOf<TextChannel>()
            chans.addAll(this.textChannels)
            chans.add(mock)
            every { this@createMockedChannel.textChannels } returns chans
        }
        if (mock is VoiceChannel) {
            every { this@createMockedChannel.getVoiceChannelById(id) } returns mock
            val chans = mutableListOf<VoiceChannel>()
            chans.addAll(this.voiceChannels)
            chans.add(mock)
            every { this@createMockedChannel.voiceChannels } returns chans
        }
    }
    return mock
}