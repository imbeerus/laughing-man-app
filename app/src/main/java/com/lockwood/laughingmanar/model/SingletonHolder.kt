package com.lockwood.laughingmanar.model

open class SingletonHolder<out T, in A, B>(creator: (A, B) -> T) {
    private var creator: ((A, B) -> T)? = creator
    @Volatile
    private var instance: T? = null

    fun getInstance(firstArg: A, secondArg: B): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(firstArg, secondArg)
                instance = created
                creator = null
                created
            }
        }
    }
}