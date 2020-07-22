package com.mrkirby153.kirbot

import com.ninjasquad.springmockk.isMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import java.lang.AssertionError

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