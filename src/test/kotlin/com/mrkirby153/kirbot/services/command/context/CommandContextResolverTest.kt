package com.mrkirby153.kirbot.services.command.context

import com.mrkirby153.kirbot.services.command.CommandNode
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import net.dv8tion.jda.api.sharding.ShardManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Method

internal class CommandContextResolverTest {

    private lateinit var ccr: CommandContextResolver
    private lateinit var contextResolvers: ContextResolvers

    @MockK
    private lateinit var shardManager: ShardManager

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        contextResolvers = ContextResolvers(shardManager)
        ccr = CommandContextResolver(contextResolvers)
    }

    @Test
    fun testHappyPath() {
        val args = listOf("one", "two", "three")
        val method = javaClass.getMethod("happyPathStub", String::class.java)
        val node = makeNode(method)
        val resolvedArgs = ccr.resolve(node, args, mockk(), null)
        assertThat(resolvedArgs[0]).isEqualTo("one two three")
    }

    @Test
    fun testOptional() {
        val method = javaClass.getMethod("optionalStub", String::class.java)
        val node = makeNode(method)
        val resolved = ccr.resolve(node, emptyList(), mockk(), null)
        assertThat(resolved[0]).isNull()
    }

    @Test
    fun testRequiredAndOptional() {
        val method = javaClass.getMethod("otherOptionalStub", String::class.java,
                String::class.java)
        val node = makeNode(method)
        val resolved = ccr.resolve(node, listOf("one", "two"), mockk(), null)
        assertThat(resolved[0]).isEqualTo("one")
        assertThat(resolved[1]).isEqualTo("two")

        val optionalResolved = ccr.resolve(node, listOf("one"), mockk(), null)
        assertThat(optionalResolved[0]).isEqualTo("one")
        assertThat(optionalResolved[1]).isNull()
    }

    @Test
    fun testRequired() {
        val method = javaClass.getMethod("happyPathStub", String::class.java)
        val node = makeNode(method)
        assertThrows<MissingArgumentException> { ccr.resolve(node, emptyList(), mockk(), null) }
    }

    @Test
    fun testNoArgs() {
        val method = javaClass.getMethod("noArgsStub")
        val node = makeNode(method)
        val resolved = ccr.resolve(node, emptyList(), mockk(), null)
        assertThat(resolved).isEmpty()
    }

    @Test
    fun testExtraArgs() {
        val method = javaClass.getMethod("noArgsStub")
        val node = makeNode(method)
        val resolved = ccr.resolve(node, listOf("one", "two", "three"), mockk(), null)
        assertThat(resolved).isEmpty()
    }

    @Test
    fun testRequiredAfterOptional() {
        val method = javaClass.getMethod("requiredAfterOptional", String::class.java,
                String::class.java)
        val node = makeNode(method)
        assertThrows<ArgumentParseException> {
            ccr.resolve(node, listOf("one", "two"), mockk(), null)
        }
    }

    private fun makeNode(method: Method) = CommandNode("test", method,
            CommandContextResolverTest::class.java, mockk())

    fun happyPathStub(name: String) {
        throw IllegalStateException(
                "This method exists as a workaround to mocking Method and should not be called manually")
    }

    fun optionalStub(@Optional name: String) {
        throw IllegalArgumentException(
                "This method exists as a workaround to mocking Method and should not be called manually")
    }

    fun noArgsStub() {
        throw IllegalStateException(
                "This method exists as a workaround to mocking Method and should not be called manually")
    }

    fun otherOptionalStub(@Single required: String, @Optional @Single optional: String) {
        throw IllegalStateException(
                "This method exists as a workaround to mocking Method and should not be called manually")
    }

    fun requiredAfterOptional(@Single @Optional required: String, @Single optional: String) {
        throw IllegalStateException(
                "This method exists as a workaround to mocking Method and should not be called manually")
    }
}