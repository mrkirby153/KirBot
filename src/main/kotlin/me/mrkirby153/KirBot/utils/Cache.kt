package me.mrkirby153.KirBot.utils

class Cache<K, V>(val expiresIn: Long) : MutableMap<K, V> {

    private val cacheRepository = mutableMapOf<K, CacheValue<V>>()

    override val size: Int
        get() = getValidValues().size

    override fun containsKey(key: K) = getValidValues().containsKey(key)

    override fun containsValue(value: V) = getValidValues().containsValue(value)

    override fun get(key: K): V? = getValidValues()[key]

    override fun isEmpty(): Boolean = getValidValues().isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = getValidValues().entries
    override val keys: MutableSet<K>
        get() = getValidValues().keys
    override val values: MutableCollection<V>
        get() = getValidValues().values

    override fun clear() {
        cacheRepository.clear()
    }

    override fun put(key: K, value: V): V? {
        val existing = cacheRepository.put(key, CacheValue(value, System.currentTimeMillis() + expiresIn)) ?: return null
        if (existing.expiresOn <= System.currentTimeMillis())
            return null
        else
            return existing.value
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach({ key, value -> put(key, value) })
    }

    override fun remove(key: K): V? {
        val existing = cacheRepository.remove(key) ?: return null
        if (existing.expiresOn <= System.currentTimeMillis())
            return null
        else
            return existing.value
    }

    private fun getValidValues(): MutableMap<K, V> {
        val validVals = mutableMapOf<K, V>()

        val iterator = this.cacheRepository.iterator()

        while(iterator.hasNext()){
            val next = iterator.next()
            val key = next.key
            val value = next.value
            if(value.expiresOn > System.currentTimeMillis()){
                validVals.put(key, value.value)
            } else {
                iterator.remove()
            }
        }
        return validVals
    }

    data class CacheValue<out V>(val value: V, val expiresOn: Long)
}
