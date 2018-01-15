package me.mrkirby153.KirBot.database.models

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.sql.ResultSet
import java.sql.Timestamp
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.kotlinProperty

open class Model {

    private val store = mutableMapOf<Field, Any?>()

    @Transient
    var timestamps = true

    var created_at: Timestamp? = null
    var updated_at: Timestamp? = null

    init {
        updateState()
    }

    /**
     * Updates the model in the database
     */
    fun update() {
        if (!isDirty())
            return
        updateTimestamps()
        QueryBuilder(this.javaClass, this).update()
        updateState()
    }

    /**
     * Creates the model in the database
     */
    fun create() {
        updateTimestamps()
        QueryBuilder(this.javaClass, this).create()
        updateState()
    }

    /**
     * Updates the created_at and updated_at fields in the database
     */
    private fun updateTimestamps() {
        if (created_at == null) {
            created_at = Timestamp(System.currentTimeMillis())
        }
        updated_at = Timestamp(System.currentTimeMillis())
    }

    /**
     * Saves the model in the database. If the model doesn't exist, it is created. If the model exists,
     * then it will be created
     */
    fun save() {
        val builder = QueryBuilder(this.javaClass, this)
        if (this.exists()) {
            if (isDirty()) {
                updateTimestamps()
                builder.update()
            }
        } else {
            updateTimestamps()
            builder.create()
        }
    }

    /**
     * Determines if the model is dirty (pending changes that need to be committed to the database
     *
     * @return True if the model has unsaved changes
     */
    fun isDirty(): Boolean {
        var dirty = false
        store.forEach { field, originalValue ->
            val comparing = field.get(this)
            if (comparing != originalValue)
                dirty = true
        }
        return dirty
    }

    /**
     * Commits the current state of the model to the internal storage. Used fot determining if the
     * model is dirty
     */
    private fun updateState() {
        store.clear()
        getAccessibleFields(this.javaClass).forEach {
            it.isAccessible = true
            store.put(it, it.get(this))
        }
    }

    /**
     * Checks if the model exists in the database
     *
     * @return True if the model exists
     */
    fun exists(): Boolean {
        return QueryBuilder(this.javaClass, this).exists()
    }

    /**
     * Takes a ResultSet and populates all the fields in the current model
     *
     * @param rs The resultset to populate the model with
     */
    fun populate(rs: ResultSet) {
        getAccessibleFields(this.javaClass).forEach { field ->
            field.isAccessible = true
            val columnName = getColumnName(field)
            if (field.kotlinProperty?.isFinal == true) {
                (field.kotlinProperty as? KMutableProperty)?.setter?.call(this,
                        rs.getObject(columnName))
            }
        }
        updateState()
    }

    /**
     * Gets a key/value pair of the column data and its values
     *
     * @return A map of the keys and values for the column
     */
    fun getColumnData(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        getAccessibleFields(this.javaClass).forEach { field ->
            if (!isTransient(field))
                map[getColumnName(field)] = field.get(this)
        }

        return map
    }


    companion object {
        lateinit var factory: ConnectionFactory

        /**
         * Gets the table for a model
         *
         * @param clazz The model class
         *
         * @return The table name
         */
        fun getTable(clazz: Class<out Model>): String {
            return clazz.getAnnotation(Table::class.java).value
        }

        /**
         * Get a list of all the columns that this model should save in the database. Transient fields
         * and final fields are excluded from this list
         *
         * @param clazz The model class
         *
         * @return A list of column names
         */
        fun getColumns(clazz: Class<out Model>): List<String> {
            return getAccessibleFields(clazz).filter { !isTransient(it) }.map { getColumnName(it) }
        }

        /**
         * Converts a field's name into a column name. If the field is annotated with a [Column] annotation
         * it will use this value. If it isn't set, the field's name will be used
         *
         * @param field The field
         *
         * @return The columns name
         */
        fun getColumnName(field: Field): String {
            return field.getAnnotation(Column::class.java)?.value ?: field.name
        }

        /**
         * Checks if a field is transient (Not serializable)
         *
         * @param field The field to check
         *
         * @return True if the field is transient, and should be excluded from the serialization
         */
        fun isTransient(field: Field): Boolean {
            return Modifier.isTransient(field.modifiers)
        }

        /**
         * Checks if the model has `created_at` and `updated_at` columns and if they should be updated.
         * This checks for the [Timestamps] annotation on a class, and returns its value. Defaults
         * to "true" if the annotation doesn't exist
         *
         * @param clazz The class to check
         *
         * @return True if the model should update timestamp.
         */
        private fun usingTimestamps(clazz: Class<out Model>): Boolean {
            return clazz.getAnnotation(Timestamps::class.java)?.value ?: true
        }

        /**
         * Gets all the fields that should be serialized in the database
         *
         * @param clazz The model
         *
         * @return A list of fields of the model which should serialized
         */
        fun getAccessibleFields(clazz: Class<out Model>): List<Field> {
            return clazz.kotlin.memberProperties.mapNotNull { it as? KMutableProperty1<*, *> }.mapNotNull { it.javaField }.filter {
                if (isTransient(it))
                    return@filter false
                if (Modifier.isFinal(it.modifiers))
                    return@filter false
                if (!usingTimestamps(clazz))
                    if (it.name == "created_at" || it.name == "updated_at")
                        return@filter false
                return@filter true
            }
        }

        /**
         * Determines the primary key of the model. Checks for a field with [PrimaryKey] and return's the
         * model's name
         *
         * @param clazz The model
         *
         * @return The primary key
         */
        fun primaryKey(clazz: Class<out Model>): String {
            clazz.fields.forEach { field ->
                if (field.isAnnotationPresent(PrimaryKey::class.java)) {
                    return getColumnName(field)
                }
            }
            return "id"
        }

        /**
         * Gets a model by its primary key
         *
         * @param clazz The class of the model
         * @param value The value of the primary key
         *
         * @return A list of models
         */
        @JvmStatic
        @JvmOverloads
        fun <T : Model> get(clazz: Class<T>, value: Any, column: String? = null): List<T> {
            return QueryBuilder(clazz).where(column ?: Companion.primaryKey(clazz),
                    value).get()
        }

        /**
         * Gets the first instnace of a module matching its primary key
         *
         * @param clazz The class
         * @param value The value of the key
         *
         * @return The model
         */
        @JvmStatic
        @JvmOverloads
        fun <T : Model> first(clazz: Class<T>, value: Any, column: String? = null): T? {
            return QueryBuilder(clazz).where(column ?: Companion.primaryKey(clazz), value).first()
        }

        /**
         * Gets the first element matching the columns
         *
         * @param clazz The model class
         * @param columns The model's columns
         *
         * @return A list of models
         */
        @JvmStatic
        fun <T : Model> get(clazz: Class<T>, vararg columns: Pair<String, Any>): List<T> {
            return QueryBuilder(clazz).apply {
                columns.forEach {
                    where(it.first, it.second)
                }
            }.get()
        }

        /**
         * Gets the first element matching the columns
         *
         * @param clazz The model class
         * @param columns The model's columns
         *
         * @return The model, or null if it doesn't exist
         */
        @JvmStatic
        fun <T : Model> first(clazz: Class<T>, vararg columns: Pair<String, Any>): T? {
            return QueryBuilder(clazz).apply {
                columns.forEach {
                    where(it.first, it.second)
                }
            }.first()
        }
    }
}