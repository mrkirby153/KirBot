package me.mrkirby153.KirBot.database.models

import kotlin.reflect.KClass

class QueryBuilder<T : Model>(private val model: Class<T>, val instance: T? = null) {


    constructor(model: KClass<T>, instance: T? = null) : this(model.java, instance)

    private val selectors = mutableListOf<QuerySelector>()


    fun where(col: String, test: String, value: Any): QueryBuilder<T> {
        selectors.add(QuerySelector(col, test, value))
        return this
    }

    fun where(col: String, value: Any): QueryBuilder<T> = where(col, "=", value)

    fun get(): List<T> {
        populateSelectors()
        val query = "SELECT ${buildColumns()} FROM `${Model.getTable(
                model)}` ${buildSelectorStatement()}"

        val list = mutableListOf<T>()
        Model.factory.getConnection().prepareStatement(query).use { ps ->
            var index = 1
            selectors.forEach { s ->
                ps.setObject(index++, s.value)
            }

            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val instance = model.newInstance()
                    instance.populate(rs)
                    list.add(instance)
                }
            }
        }
        return list
    }

    fun create() {
        if (instance == null)
            throw IllegalArgumentException("Instance is null")
        val placeholders = "?, ".repeat(buildColumns().length)
        val colData = instance.getColumnData()
        val query = "INSERT INTO ${Model.getTable(
                model)} (${colData.keys.joinToString(", ") { "`$it`" }}) VALUES(${
        placeholders.substring(
                0..placeholders.length - 2)
        })"

        Model.factory.getConnection().prepareStatement(query).use { ps ->
            var index = 1
            colData.values.forEach { value ->
                ps.setObject(index++, value)
            }
            ps.executeUpdate()
        }
    }

    fun exists(): Boolean {
        if (instance == null)
            throw IllegalArgumentException("instance is null")
        populateSelectors()
        val query = "SELECT 1 FROM `${Model.getTable(model)}` ${buildSelectorStatement()}"
        Model.factory.getConnection().prepareStatement(query).use { ps ->
            var index = 1
            selectors.forEach {
                ps.setObject(index++, it.value)
            }
            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    fun update() {
        if (instance == null)
            throw IllegalArgumentException("Instance is null")
        populateSelectors()
        val cols = instance.getColumnData()
        val params = buildString {
            cols.keys.forEachIndexed { index, s ->
                append("`$s` = ?")
                if (index + 1 < cols.size)
                    append(", ")
            }
        }
        val query = "UPDATE `${Model.getTable(model)}` SET $params ${buildSelectorStatement()}"

        Model.factory.getConnection().prepareStatement(query).use { ps ->
            var index = 1
            cols.values.forEach { value ->
                ps.setObject(index++, value)
            }

            selectors.forEach { selector ->
                ps.setObject(index++, selector.value)
            }
            println(ps)
            ps.executeUpdate()
        }
    }

    private fun populateSelectors() {
        if (instance != null)
            if (selectors.isEmpty()) {
                selectors.add(QuerySelector(Model.primaryKey(model), "=",
                        instance.getColumnData()[Model.primaryKey(model)].toString()))
            }
    }

    private fun buildColumns() = Model.getColumns(model).joinToString(", ") { "`$it`" }

    private fun buildSelectorStatement() = buildString {
        append("WHERE ")
        append(selectors.joinToString(", ") { "`${it.column}` ${it.test} ?" })
    }

    private data class QuerySelector(val column: String, val test: String, val value: Any)

}