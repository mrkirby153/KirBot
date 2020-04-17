package me.mrkirby153.KirBot.inject

import me.mrkirby153.KirBot.Bot
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinProperty


/**
 * Class responsible for handling dependency injection and instantiation
 */
class ApplicationContext {


    /**
     * Map of currently registered contexts in the application context
     */
    private val registeredObjects: ConcurrentHashMap<Class<*>, MutableList<RegisteredObject<*>>> = ConcurrentHashMap()

    private val constructorCache: ConcurrentHashMap<Class<*>, Constructor<*>> = ConcurrentHashMap()

    private val injectableFieldCache: ConcurrentHashMap<Class<*>, List<Field>> = ConcurrentHashMap()

    private val applicationContextLogger = LoggerFactory.getLogger("Application Context")

    /**
     * Scans the provided packages for classes with the [Injectable] annotation and automatically
     * registers them into the context
     */
    fun scanForInjectables(vararg packages: String) {
        val reflections = Reflections(packages)

        val injectableClasses = reflections.getTypesAnnotatedWith(Injectable::class.java)
        Bot.LOG.debug("Discovered ${injectableClasses.size} injectable classes")
        injectableClasses.forEach { clazz ->
            val name = clazz.getAnnotation(Injectable::class.java).name
            if(name == "") {
                register(clazz, null)
            } else {
                register(clazz, name)
            }
        }
    }


    /**
     * Registers a class in the application context. A new instance of this class will be created
     * when required
     *
     * @param clazz The class to register into the application context
     * @param name The name of the injectable
     */
    fun register(clazz: Class<*>, name: String? = null) {
        val registeredObjects = this.registeredObjects[clazz]
        if (registeredObjects != null) {
            val found = registeredObjects.firstOrNull { it.objectClass == clazz && it.name == name }
            if (found != null) {
                throw java.lang.IllegalArgumentException(
                        "Provided object Class ($clazz) is already registered in the context")
            }
        }
        val type = if (clazz.isAnnotationPresent(
                        Singleton::class.java)) InjectionType.SINGLETON else InjectionType.CLASS
        applicationContextLogger.debug("Registering $clazz into the context as $type")
        val className = clazz.getAnnotation(Named::class.java)?.value ?: name
        this.registeredObjects.computeIfAbsent(clazz) { mutableListOf() }.add(
                RegisteredObject(type, clazz, className))
    }


    /**
     * Registers a singleton object into the application context. This instance will be reused
     *
     * @param obj The object singleton to register into the application context
     */
    fun register(obj: Any, name: String? = null) {
        val registeredObjects = this.registeredObjects[obj.javaClass]
        if (registeredObjects != null) {
            val found = registeredObjects.firstOrNull { it.objectClass == obj.javaClass && it.name == name }
            if (found != null) {
                throw java.lang.IllegalArgumentException(
                        "Provided object $obj (${obj.javaClass}) is already registered in the context")
            }
        }
        applicationContextLogger.debug(
                "Registering singleton $obj (${obj.javaClass}) into the context")

        this.registeredObjects.computeIfAbsent(obj.javaClass) { mutableListOf() }.add(
                RegisteredObject(InjectionType.SINGLETON, obj.javaClass, name, obj))
    }

    fun <T> registerLazySingleton(clazz: Class<T>, name: String? = null) {
        val registeredObjects = this.registeredObjects[clazz]
        if (registeredObjects != null) {
            val found = registeredObjects.firstOrNull { it.objectClass == clazz && it.name == name }
            if (found != null) {
                throw java.lang.IllegalArgumentException(
                        "Provided $clazz is already registered in the context")
            }
        }
        applicationContextLogger.debug(
                "Registering lazy singleton $clazz into the context")
        this.registeredObjects.computeIfAbsent(clazz) { mutableListOf() }.add(
                RegisteredObject(InjectionType.SINGLETON, clazz, name))
    }

    /**
     * Gets the object from the application context
     *
     * @param clazz The class to get from the context
     * @param name The name of the class, or null
     *
     * @return An instance of the class from the application context
     *
     * @throws IllegalArgumentException If the provided class is not valid for instantiation
     */
    fun <T> get(clazz: Class<T>, name: String? = null): T {
        return get(clazz, name, emptyArray())
    }

    /**
     * Constructs a new instance of an arbitrary class using values from the application context
     *
     * @param clazz The class
     *
     * @return A new instance of the class
     */
    fun <T> newInstance(clazz: Class<T>): T {
        return doInjection(clazz, arrayOf(clazz as Class<*>))
    }

    private fun <T> get(clazz: Class<T>, name: String?, callChain: Array<Class<*>>): T {
        applicationContextLogger.trace("Retrieving $clazz from the application context")

        if (clazz in callChain) {
            val chain = callChain.joinToString(" -> ") { it.canonicalName }
            throw IllegalArgumentException(
                    "Circular dependency chain detected! Call chain is [$chain]")
        }

        val registeredObjects = registeredObjects.filterKeys { clazz.isAssignableFrom(it)  }.flatMap { it.value }
        val registeredObject = registeredObjects.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException(
                        "$clazz ($name) was not found in the application context")

        if (registeredObject.injectionType == InjectionType.SINGLETON) {
            if (registeredObject.objectValue != null) {
                applicationContextLogger.trace("$clazz is a singleton, returning")
                return registeredObject.objectValue as T
            }
            applicationContextLogger.trace("$clazz has not been constructed, constructing")
        }
        val instance = clazz.cast(doInjection(registeredObject.objectClass, callChain))


        applicationContextLogger.trace("Constructed $clazz as $instance")
        if (registeredObject.injectionType == InjectionType.SINGLETON) {
            registeredObject.objectValue = instance
        }
        return instance
    }

    private fun <T> doInjection(clazz: Class<T>, callChain: Array<Class<*>>): T {
        // Do some reflection to create the class
        val constructor = findConstructor(clazz)

        val constructorArguments = mutableListOf<Any>()
        constructor.parameters.forEach { param ->
            val paramName = param.getAnnotation(Named::class.java)?.value
            constructorArguments.add(get(param.type, paramName, arrayOf(*callChain, clazz)))
        }

        val instance = constructor.newInstance(*constructorArguments.toTypedArray())

        val injectableFields = getInjectableFields(clazz, instance as Any)
        injectableFields.forEach { field ->
            val kp = field.kotlinProperty
            val fieldName = if (kp != null) {
                kp.findAnnotation<Named>()?.value
            } else {
                field.getAnnotation(Named::class.java)?.value
            }
            val toInject = get(field.type, fieldName, arrayOf(*callChain, clazz))
            field.set(instance, toInject)
        }
        return instance
    }


    /**
     * Finds a suitable constructor for injection
     */
    private fun <T> findConstructor(clazz: Class<T>): Constructor<T> {
        if (constructorCache.contains(clazz)) {
            return constructorCache[clazz] as Constructor<T>
        }
        val suitableConstructors = clazz.constructors.filter { constructor ->
            constructor.isAnnotationPresent(
                    Inject::class.java) || constructor.parameters.isEmpty()
        }
        applicationContextLogger.trace(
                "Found ${suitableConstructors.size} potential constructors on $clazz")
        if (suitableConstructors.isEmpty())
            throw IllegalArgumentException("$clazz has no suitable constructors")

        // Prefer injectable constructor over the default constructor
        val injectableConstructors = suitableConstructors.filter {
            it.isAnnotationPresent(Inject::class.java)
        }

        applicationContextLogger.trace(
                "Found ${injectableConstructors.size} injectable constructors on $clazz")

        if (injectableConstructors.size > 1)
            throw IllegalArgumentException(
                    "$clazz has more than one constructor annotated with @Inject")

        val constructor: Constructor<*>
        constructor = if (injectableConstructors.isEmpty()) {
            // Default constructor
            applicationContextLogger.trace("Using default constructor on $clazz")
            suitableConstructors.first { it.parameters.isEmpty() }
        } else {
            // Argument constructor
            applicationContextLogger.trace("Using argument constructor on $clazz")
            injectableConstructors.first()
        }
        constructorCache[clazz] = constructor
        return constructor as Constructor<T>
    }

    /**
     * Gets a list of all fields annotated with @Inject that can have types injected into
     *
     * @param clazz The class to get injectable fields for
     * @param instance The instance of the class
     */
    private fun getInjectableFields(clazz: Class<*>, instance: Any): List<Field> {
        if (injectableFieldCache.contains(clazz)) {
            return injectableFieldCache[clazz]!!
        }

        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz
        do {
            if (currentClass != null) {

                fields.addAll(currentClass.declaredFields.filter { field ->
                    if(Modifier.isStatic(field.modifiers))
                        return@filter false
                    if (!field.canAccess(instance))
                        field.isAccessible = true
                    field.isAnnotationPresent(
                            Inject::class.java) || field.kotlinProperty?.findAnnotation<Inject>() != null
                })

                currentClass = currentClass.superclass
            } else {
                break // This should never happen but it makes Kotlin happy
            }
        } while (currentClass != null)

        applicationContextLogger.trace("Found ${fields.size} injectable fields on $clazz")
        injectableFieldCache[clazz] = fields
        return fields
    }

    /**
     * Data class representing an object registered in the application context
     */
    private data class RegisteredObject<T>(val injectionType: InjectionType,
                                           val objectClass: Class<T>,
                                           val name: String? = null, var objectValue: Any? = null)

    private enum class InjectionType {
        SINGLETON,
        CLASS
    }
}