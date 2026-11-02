package com.dcs.charging.common

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ErrorResponse(val message: String, val errors: Map<String, String> = emptyMap())

class NotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(ex: NotFoundException) = ErrorResponse(ex.message ?: "Not found")

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflict(ex: ConflictException) = ErrorResponse(ex.message ?: "Conflict")

    @ExceptionHandler(OptimisticLockingFailureException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun concurrentUpdate(ex: OptimisticLockingFailureException) =
        ErrorResponse("The resource was changed by another request")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun validation(ex: MethodArgumentNotValidException): ErrorResponse {
        val errors = ex.bindingResult.fieldErrors
            .groupBy { it.field }
            .mapValues { (_, fieldErrors) -> fieldErrors.first().defaultMessage ?: "invalid" }
        return ErrorResponse("Validation failed", errors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun unreadable(ex: HttpMessageNotReadableException) = ErrorResponse("Malformed request body")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun typeMismatch(ex: MethodArgumentTypeMismatchException) =
        ErrorResponse("Invalid value for '${ex.name}': ${ex.value}")
}
