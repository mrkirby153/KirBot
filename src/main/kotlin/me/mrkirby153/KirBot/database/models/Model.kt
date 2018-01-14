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

    fun update() {
        if (!isDirty())
            return
        updateTimestamps()
        QueryBuilder(this.javaClass, this).update()
        updateState()
    }

    fun create() {
        updateTimestamps()
        QueryBuilder(this.javaClass, this).create()
        updateState()
    }

    private fun updateTimestamps() {
        if (created_at == null) {
            created_at = Timestamp(System.currentTimeMillis())
        }
        updated_at = Timestamp(System.currentTimeMillis())
    }

    fun save() {
        val builder = QueryBuilder(this.javaClass, this)
        if (this.exists()) {
            builder.update()
        } else {
            builder.create()
        }
    }

    fun isDirty(): Boolean {
        var dirty = false
        store.forEach { field, originalValue ->
            val comparing = field.get(this)
            if (comparing != originalValue)
                dirty = true
        }
        return dirty
    }

    private fun updateState() {
        store.clear()
        getAccessibleFields(this.javaClass).forEach {
            it.isAccessible = true
            store.put(it, it.get(this))
        }
    }

    fun exists(): Boolean {
        return QueryBuilder(this.javaClass, this).exists()
    }

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

        fun getTable(clazz: Class<out Model>): String {
            return clazz.getAnnotation(Table::class.java).value
        }

        fun getColumns(clazz: Class<out Model>): List<String> {
            return getAccessibleFields(clazz).filter { !isTransient(it) }.map { getColumnName(it) }
        }

        fun getColumnName(field: Field): String {
            return field.getAnnotation(Column::class.java)?.value ?: field.name
        }

        fun isTransient(field: Field): Boolean {
            return Modifier.isTransient(field.modifiers)
        }

        fun usingTimestamps(clazz: Class<out Model>): Boolean {
            return clazz.getAnnotation(Timestamps::class.java)?.value ?: true
        }

        fun getAccessibleFields(clazz: Class<out Model>): List<Field> {
            return clazz.kotlin.memberProperties.mapNotNull { it as? KMutableProperty1<*, *> }.mapNotNull { it.javaField }.filter {
                if (isTransient(it))
                    return@filter false
                if (!usingTimestamps(clazz))
                    if (it.name == "created_at" || it.name == "updated_at")
                        return@filter false
                return@filter true
            }
        }

        fun primaryKey(clazz: Class<out Model>): String {
            clazz.fields.forEach { field ->
                if(field.isAnnotationPresent(PrimaryKey::class.java)){
                    return getColumnName(field)
                }
            }
            return "id"
        }
    }
}