package com.example.onlinecodecompiler.execute

import com.example.onlinecodecompiler.common.ErrorDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.*

@WebMvcTest(ExecuteController::class)
class ExecuteControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var executeService: ExecuteService

    @Test
    fun `test createExecution returns execution id`() {
        // Given
        val codeDto = CodeDto("print('Hello')", "PYTHON")
        `when`(executeService.createExecution(codeDto)).thenReturn(42L)

        // When/Then
        mockMvc.perform(
            post("/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(codeDto))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("42"))
    }

    @Test
    fun `test runExecution returns SSE emitter`() {
        // Given
        val emitter = SseEmitter()
        `when`(executeService.runExecution(1L)).thenReturn(emitter)

        // When/Then
        mockMvc.perform(
            get("/execute/1")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE)
        )
            .andExpect(status().isOk())

        verify(executeService).runExecution(1L)
    }

    @Test
    fun `test stopExecution calls service`() {
        // When/Then
        mockMvc.perform(
            post("/execute/1")
        )
            .andExpect(status().isOk())

        verify(executeService).stopExecution(1L)
    }

    @Test
    fun `test getExecutionDetails returns execution details`() {
        // Given
        val execution = Execution()
        execution.id = 1L
        execution.status = Status.OK
        execution.executedAt = Instant.now()
        execution.executionTimeMs = 123

        `when`(executeService.findExecution(1L)).thenReturn(execution)

        // When/Then
        mockMvc.perform(
            get("/execute/1/details")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.executionTimeMs").value(123))
            .andExpect(jsonPath("$.executedAt").exists())
    }

    @Test
    fun `test createExecution with invalid dto returns 400`() {
        // Given
        val invalidCodeDto = CodeDto("", "INVALID")

        // When/Then
        mockMvc.perform(
            post("/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCodeDto))
        )
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `test handleExecutionNotFound returns 404`() {
        // Given
        `when`(executeService.findExecution(999L)).thenThrow(ExecutionNotFoundException())

        // When/Then
        mockMvc.perform(
            get("/execute/999/details")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `test handleIllegalStateException returns 500`() {
        // Given
        `when`(executeService.findExecution(999L)).thenThrow(IllegalStateException("Test error"))

        // When/Then
        mockMvc.perform(
            get("/execute/999/details")
        )
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Test error"))
    }
}
