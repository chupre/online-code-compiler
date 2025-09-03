package com.example.onlinecodecompiler.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.function.Consumer

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(): ResponseEntity<ErrorDto?> {
        return ResponseEntity.badRequest().body<ErrorDto?>(ErrorDto("Invalid request body"))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(): ResponseEntity<ErrorDto?> {
        return ResponseEntity.badRequest().body<ErrorDto?>(ErrorDto("Invalid path variable type"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<MutableMap<String?, String?>?> {
        val errors: MutableMap<String?, String?> = HashMap<String?, String?>()

        ex.bindingResult.fieldErrors
            .forEach(Consumer { error: FieldError? -> errors[error!!.field] = error.defaultMessage })

        return ResponseEntity.badRequest().body<MutableMap<String?, String?>?>(errors)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorDto?> {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body<ErrorDto?>(ErrorDto(ex.message!!))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ResponseEntity<ErrorDto?> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body<ErrorDto?>(ErrorDto(ex.message))
    }
}