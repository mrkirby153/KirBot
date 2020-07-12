package com.mrkirby153.kirbot.services.setting

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mrkirby153.kirbot.DiscordTestUtils
import com.mrkirby153.kirbot.entity.GuildSetting
import com.mrkirby153.kirbot.entity.repo.GuildSettingRepository
import net.dv8tion.jda.api.entities.Guild
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

private val testObject = SettingManagerTest.TestJsonObject("Testing", "Testing", "example")

@DataJpaTest
internal class SettingManagerTest {

    private val expectedJson = "{\"key\": \"Testing\",\"value\": \"Testing\",\"user_name\": \"example\"}"

    @Autowired
    private lateinit var repo: GuildSettingRepository
    private lateinit var service: SettingsService

    private lateinit var guild: Guild

    private fun populateDefaults() {
        val defaults = arrayOf(Pair(TestSettings.testStringSetting, "testing"),
                Pair(TestSettings.testBooleanSetting, "false"),
                Pair(TestSettings.testNumberSetting, "10"),
                Pair(TestSettings.testArraySetting, "[$expectedJson]"),
                Pair(TestSettings.testObjectSetting, expectedJson),
                Pair(TestSettings.testPrimitiveArraySetting, "[\"example1\", \"example2\"]"))
        defaults.forEach {
            val setting = GuildSetting(it.first.key, guild.id, it.first.key, it.second)
            repo.save(setting)
        }
    }

    private fun <T> setAndVerify(setting: Setting<T>, value: T, stringRepresentation: String) {
        service.setSetting(setting, guild, value)
        assertThat(repo.getByGuildAndKey(guild.id,
                setting.key)).get().extracting { it.value }.isEqualTo(stringRepresentation)
    }

    private fun <T> getAndVerify(setting: Setting<T>, expected: T) {
        assertThat(service.getSetting(setting, guild)).isEqualTo(expected)
    }

    @BeforeEach
    fun setUp() {
        service = SettingManager(repo)
        guild = DiscordTestUtils.mockGuild

        populateDefaults()
    }

    @Test
    fun testStringSetting() {
        getAndVerify(TestSettings.testStringSetting, "testing")
        setAndVerify(TestSettings.testStringSetting, "example", "example")
    }

    @Test
    fun testBooleanSetting() {
        getAndVerify(TestSettings.testBooleanSetting, false)
        setAndVerify(TestSettings.testBooleanSetting, true, "1")
    }

    @Test
    fun testNumberSetting() {
        getAndVerify(TestSettings.testNumberSetting, 10)
        setAndVerify(TestSettings.testNumberSetting, 100, "100")
    }

    @Test
    fun testArraySetting() {
        getAndVerify(TestSettings.testArraySetting, arrayOf(testObject))
        setAndVerify(TestSettings.testArraySetting, arrayOf(TestJsonObject("a", "b", "c")), "[{\"key\":\"a\",\"value\":\"b\",\"user_name\":\"c\"}]")
    }

    @Test
    fun testObjectSetting() {
        getAndVerify(TestSettings.testObjectSetting, testObject)
        setAndVerify(TestSettings.testObjectSetting, TestJsonObject("a", "b", "c"),
                "{\"key\":\"a\",\"value\":\"b\",\"user_name\":\"c\"}")
    }

    @Test
    fun testPrimitiveArraySetting() {
        getAndVerify(TestSettings.testPrimitiveArraySetting, arrayOf("example1", "example2"))
        setAndVerify(TestSettings.testPrimitiveArraySetting, arrayOf("a"), "[\"a\"]")
    }

    @Test
    fun testResetToDefault() {
        setAndVerify(TestSettings.testStringSetting, "this is a test", "this is a test")
        service.setSetting(TestSettings.testStringSetting, guild, TestSettings.testStringSetting.default)
        assertThat(repo.getByGuildAndKey(guild.id, TestSettings.testStringSetting.key)).isNotPresent
    }

    @Test
    fun testSetToNull() {
        service.setSetting(TestSettings.testStringSetting, guild, null)
        assertThat(repo.getByGuildAndKey(guild.id, TestSettings.testStringSetting.key)).isNotPresent
    }

    @Test
    fun testCreate() {
        val setting = StringSetting("create")
        assertThat(repo.getByGuildAndKey(guild.id, setting.key)).isNotPresent
        service.setSetting(setting, guild, "testing")
        assertThat(repo.getByGuildAndKey(guild.id, setting.key)).isPresent
    }

    @Test
    fun testDefault() {
        val default = StringSetting("default", "default")
        assertThat(service.getSetting(default, guild)).isEqualTo("default")
    }


    private object TestSettings {
        val testStringSetting = StringSetting("string", "test")
        val testBooleanSetting = BooleanSetting("boolean", false)
        val testNumberSetting = NumberSetting("number", 10L)

        val testArraySetting = ArraySetting("array", TestJsonObject::class.java, arrayOf(testObject))
        val testObjectSetting = ObjectSetting("object", TestJsonObject::class.java, testObject)

        val testPrimitiveArraySetting = ArraySetting("primitive_array", String::class.java,
                arrayOf("example1", "example2"))
    }

    data class TestJsonObject(val key: String, val value: String, @JsonProperty("user_name") val username: String)

}