package com.example.onlinecodecompiler.common

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [EnumValidator::class])
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumValue(
    val message: String = "must be any of {enumClass}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val enumClass: KClass<out Enum<*>>,
    val ignoreCase: Boolean = false
)

class EnumValidator : ConstraintValidator<EnumValue, String> {
    private lateinit var enumClass: KClass<out Enum<*>>
    private var ignoreCase: Boolean = false

    override fun initialize(annotation: EnumValue) {
        enumClass = annotation.enumClass
        ignoreCase = annotation.ignoreCase
    }

    override fun isValid(p0: String?, p1: ConstraintValidatorContext?): Boolean {
        if (p0 == null) return true

        return enumClass.java.enumConstants
            .map { it.name }
            .any { if (ignoreCase) it.equals(p0, ignoreCase = true) else it == p0 }
    }
}