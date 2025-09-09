package com.example.onlinecodecompiler.execute

import com.example.onlinecodecompiler.common.ErrorDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/execute")
class ExecuteController(private val executeService: ExecuteService) {

    @PostMapping()
    fun createExecution(@Valid @RequestBody codeDto: CodeDto): Long? {
        return executeService.createExecution(codeDto)
    }

    @GetMapping("/{id}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun runExecution(@PathVariable id: Long): SseEmitter {
        return executeService.runExecution(id)
    }

    @PostMapping("/{id}")
    fun stopExecution(@PathVariable id: Long) {
        return executeService.stopExecution(id);
    }

    @GetMapping("/{id}/details")
    fun getExecutionDetails(@PathVariable id: Long): Execution {
        return executeService.findExecution(id)
    }

    @ExceptionHandler(ExecutionNotFoundException::class)
    fun handleExecutionNotFound(exception: ExecutionNotFoundException): ResponseEntity<ErrorDto> {
        return ResponseEntity(ErrorDto(exception.message), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(exception: IllegalStateException): ResponseEntity<ErrorDto> {
        return ResponseEntity(ErrorDto(exception.message), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
