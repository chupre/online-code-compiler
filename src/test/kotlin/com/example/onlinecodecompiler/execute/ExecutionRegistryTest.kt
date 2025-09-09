package com.example.onlinecodecompiler.execute

import com.example.onlinecodecompiler.docker.DockerStreamCallback
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@ExtendWith(MockitoExtension::class)
class ExecutionRegistryTest {

    @Mock
    private lateinit var emitter: SseEmitter

    @Mock
    private lateinit var callback: DockerStreamCallback

    private lateinit var registry: ExecutionRegistry

    @BeforeEach
    fun setUp() {
        registry = ExecutionRegistry()
    }

    @Test
    fun `test registerContainer and getContainer`() {
        // When
        registry.registerContainer(1L, "container123")

        // Then
        assert(registry.getContainer(1L) == "container123")
    }

    @Test
    fun `test removeContainer`() {
        // Given
        registry.registerContainer(1L, "container123")

        // When
        registry.removeContainer(1L)

        // Then
        assert(registry.getContainer(1L) == null)
    }

    @Test
    fun `test registerEmitter and getEmitter`() {
        // When
        registry.registerEmitter(1L, emitter)

        // Then
        assert(registry.getEmitter(1L) === emitter)
    }

    @Test
    fun `test removeEmitter`() {
        // Given
        registry.registerEmitter(1L, emitter)

        // When
        registry.removeEmitter(1L)

        // Then
        assert(registry.getEmitter(1L) == null)
    }

    @Test
    fun `test registerCallback and getCallback`() {
        // When
        registry.registerCallback(1L, callback)

        // Then
        assert(registry.getCallback(1L) === callback)
    }

    @Test
    fun `test removeCallback`() {
        // Given
        registry.registerCallback(1L, callback)

        // When
        registry.removeCallback(1L)

        // Then
        assert(registry.getCallback(1L) == null)
    }

    @Test
    fun `test cleanupExecution removes all resources`() {
        // Given
        registry.registerContainer(1L, "container123")
        registry.registerEmitter(1L, emitter)
        registry.registerCallback(1L, callback)

        // When
        registry.cleanupExecution(1L)

        // Then
        assert(registry.getContainer(1L) == null)
        assert(registry.getEmitter(1L) == null)
        assert(registry.getCallback(1L) == null)
    }

    @Test
    fun `test getContainer returns null for non-existent id`() {
        assert(registry.getContainer(999L) == null)
    }

    @Test
    fun `test getEmitter returns null for non-existent id`() {
        assert(registry.getEmitter(999L) == null)
    }

    @Test
    fun `test getCallback returns null for non-existent id`() {
        assert(registry.getCallback(999L) == null)
    }
}
