package me.mrkirby153.KirBot.database.models

import java.sql.Statement
import kotlin.reflect.KClass

class QueryBuilder<T : Model>(private val model: Class<T>, val instance: T? = null) {


    constructor(model: KClass<T>, instance: T? = null) : this(model.java, instance)

    private val selectors = mutableListOf<QuerySelector>()


    /**
     * Adds a selector to the query, narrowing the scope
     *
     * @param col The column
     * @param test The logical test to use
     * @param value The value of the column
     *
     * @return The query builder
     */
    fun where(col: String, test: String, value: Any): QueryBuilder<T> {
        selectors.add(QuerySelector(col, test, value))
        return this
    }

    /**
     * Adds a selector to the query, narrowing the scope. This however, defaults to the "=" logical test.
     *
     * @param col The column
     * @param value The value
     *
     * @return The query builder
     */
    fun where(col: String, value: Any): QueryBuilder<T> = where(col, "=", value)

    /**
     * Executes the query and returns all models that match
     *
     * @return A list of models matching the query, or an empty array
     */
    fun get(): List<T> {
        populateSelectors()
        val query = "SELECT ${buildColumns()} FROM `${Model.getTable(
                model)}` ${buildSelectorStatement()}"

        val list = mutableListOf<T>()
        Model.factory.getConnection().use {
            it.prepareStatement(query).use { ps ->
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
        }
        return list
    }

    /**
     * Deletes a model from the database
     */
    fun delete() {
        populateSelectors()
        val query = "DELETE FROM `${Model.getTable(model)}` ${buildSelectorStatement()}"

        Model.factory.getConnection().use {
            it.prepareStatement(query).use { ps ->
                var index = 1
                selectors.forEach { s ->
                    ps.setObject(index++, s.value)
                }
                ps.executeUpdate()
            }
        }
    }

    /**
     * Creates a model in the database
     */
    fun create() {
        if (instance == null)
            throw IllegalArgumentException("Instance is null")
        val colData = instance.getColumnData()
        val placeholders = "?, ".repeat(colData.size)
        val query = "INSERT INTO ${Model.getTable(
                model)} (${colData.keys.joinToString(", ") { "`$it`" }}) VALUES(${
        placeholders.substring(
                0..placeholders.length - 3)
        })"

        Model.factory.getConnection().use {
            it.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { ps ->
                var index = 1
                colData.values.forEach { value ->
                    ps.setObject(index++, value)
                }
                println(ps)
                ps.executeUpdate()
                val rs = ps.generatedKeys
                if (rs.next()) {
                    if (Model.autoIncrementing(model)) {
                        instance.set(Model.primaryKey(model), rs.getObject(1))
                    }
                }
            }
        }
    }

    /**
     * Checks if a model exists in the database
     *
     * @return True if the model exists
     */
    fun exists(): Boolean {
        if (instance == null)
            throw IllegalArgumentException("instance is null")
        populateSelectors()
        val query = "SELECT 1 FROM `${Model.getTable(model)}` ${buildSelectorStatement()}"
        Model.factory.getConnection().use {
            it.prepareStatement(query).use { ps ->
                var index = 1
                selectors.forEach {
                    ps.setObject(index++, it.value)
                }
                ps.executeQuery().use { rs ->
                    return rs.next()
                }
            }
        }
    }

    /**
     * Gets the first model matching the query
     *
     * @return The first model or null if it doesn't exist
     */
    fun first(): T? {
        return get().firstOrNull()
    }

    /**
     * Updates the model in the database
     */
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

        Model.factory.getConnection().use {
            it.prepareStatement(query).use { ps ->
                var index = 1
                cols.values.forEach { value ->
                    ps.setObject(index++, value)
                }

                selectors.forEach { selector ->
                    ps.setObject(index++, selector.value)
                }
                ps.executeUpdate()
            }
        }
    }

    /**
     * Populates selector list with the primary key if it doesn't exist
     */
    private fun populateSelectors() {
        if (instance != null)
            if (selectors.isEmpty()) {
                selectors.add(QuerySelector(Model.primaryKey(model), "=",
                        instance.getColumnData()[Model.primaryKey(model)].toString()))
            }
    }

    /**
     * Builds a list of columns for the query
     *
     * @return The column list
     */
    private fun buildColumns() = Model.getColumns(model).joinToString(", ") { "`$it`" }

    /**
     * Builds the selector statement for scoping the query
     *
     * @return The selector statement
     */
    private fun buildSelectorStatement() = buildString {
        append("WHERE ")
        append(selectors.joinToString(", ") { "`${it.column}` ${it.test} ?" })
    }

    private data class QuerySelector(val column: String, val test: String, val value: Any)

}