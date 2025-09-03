package com.example.onlinecodecompiler.execute

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/execute")
class ExecuteController(private val executeService: ExecuteService) {

    @PostMapping
    fun execute(@Valid @RequestBody codeDto: CodeDto): String {
        return executeService.execute(codeDto)
    }
}