package com.occaecat.ztoeschedule.domain.model

import java.io.IOException
import retrofit2.HttpException

sealed interface AppError {
    data object Network : AppError
    data class Server(val code: Int) : AppError
    data object Parsing : AppError
    data class Unknown(val message: String?) : AppError
}

fun Throwable.toAppError(): AppError {
    return when (this) {
        is IOException -> AppError.Network
        is HttpException -> AppError.Server(this.code())
        is com.google.gson.JsonSyntaxException -> AppError.Parsing
        else -> AppError.Unknown(this.message)
    }
}

fun AppError.getUserMessage(): String {
    return when (this) {
        is AppError.Network -> "Перевірте підключення до інтернету"
        is AppError.Server -> "Помилка сервера (код ${this.code})"
        is AppError.Parsing -> "Помилка обробки даних"
        is AppError.Unknown -> "Невідома помилка: ${this.message ?: "—"}"
    }
}