package com.nguyenmoclam.pexelssample.ui.model

data class UserFacingError(
    val message: String,
    val isRetryable: Boolean = true
) 