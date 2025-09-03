package com.example.onlinecodecompiler.execute

import com.example.onlinecodecompiler.common.EnumValue
import jakarta.validation.constraints.NotBlank

data class CodeDto(
    @field:NotBlank
    val code: String = "",

    @field:NotBlank
    @field:EnumValue(enumClass = Language::class, message = "must be any of C, PYTHON")
    val language: String = ""
)
