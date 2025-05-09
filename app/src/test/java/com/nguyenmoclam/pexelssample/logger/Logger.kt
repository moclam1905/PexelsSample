package com.nguyenmoclam.pexelssample.logger

object Logger {
    fun d(tag: String, message: String) {
        // Do nothing in JVM test
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Do nothing
    }

    fun i(tag: String, message: String) {
        // Do nothing
    }
}
