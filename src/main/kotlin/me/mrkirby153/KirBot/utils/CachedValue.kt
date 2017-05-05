package me.mrkirby153.KirBot.utils

import java.lang.ref.WeakReference

class CachedValue<T>(val expiresIn: Long) {

    private var value: WeakReference<T>? = null

    private var expiresOn: Long = -1

    fun set(value: T?) {
        if (value == null)
            this.value = null
        else
            this.value = WeakReference(value)
        this.expiresOn = System.currentTimeMillis() + this.expiresIn
    }

    fun get(): T? {
        if (this.expiresOn < System.currentTimeMillis()) {
            return null
        }
        return value?.get()
    }
}