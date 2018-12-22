package com.lockwood.laughingmanar.extensions

private const val LOG_PREFIX = "laughing_"
private const val LOG_PREFIX_LENGTH = LOG_PREFIX.length
private const val MAX_LOG_TAG_LENGTH = 23

val Any.TAG: String
    get() {
        val className = this::class.java.simpleName
        if (className.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + className.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        }
        return LOG_PREFIX + className
    }