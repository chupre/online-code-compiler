package com.example.onlinecodecompiler.docker

import com.github.dockerjava.api.model.Frame
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockedStatic
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DockerStreamCallbackTest {

    @Mock
    private lateinit var emitter: SseEmitter

    @Mock
    private lateinit var sseEventBuilder: SseEventBuilder

    @Mock
    private lateinit var frame: Frame

    private var onCompleteCalled = false
    private var onErrorCalled = false
    private var onInterruptCalled = false
    private lateinit var callback: DockerStreamCallback

    private lateinit var mockedSseEmitter: MockedStatic<SseEmitter>

    @BeforeEach
    fun setUp() {
        onCompleteCalled = false
        onErrorCalled = false
        onInterruptCalled = false

        // Mock static SseEmitter.event() method
        mockedSseEmitter = mockStatic(SseEmitter::class.java)
        mockedSseEmitter.`when`<SseEventBuilder> { SseEmitter.event() }.thenReturn(sseEventBuilder)

        // Mock instance methods
        `when`(sseEventBuilder.name(anyString())).thenReturn(sseEventBuilder)
        `when`(sseEventBuilder.data(anyString())).thenReturn(sseEventBuilder)

        // Create callback with test handlers
        callback = DockerStreamCallback(
            emitter = emitter,
            onComplete = { onCompleteCalled = true },
            onError = { onErrorCalled = true },
            onInterrupt = { onInterruptCalled = true }
        )
    }

    @AfterEach
    fun tearDown() {
        // Close the static mock to prevent memory leaks
        if (::mockedSseEmitter.isInitialized) {
            mockedSseEmitter.close()
        }
    }

    @Test
    fun `test onNext processes frame data and sends to emitter`() {
        // Given
        val payload = "Hello\nWorld\n".toByteArray()
        `when`(frame.payload).thenReturn(payload)

        // When
        callback.onNext(frame)

        // Then
        verify(emitter, times(2)).send(anyString())
    }

    @Test
    fun `test onNext handles emitter exception by interrupting`() {
        // Given
        val payload = "Hello\n".toByteArray()
        `when`(frame.payload).thenReturn(payload)
        `when`(emitter.send(anyString())).thenThrow(RuntimeException("Connection closed"))

        // When
        callback.onNext(frame)

        // Then
        assert(onInterruptCalled)
    }

    @Test
    fun `test onComplete sends remaining buffer and completion event`() {
        // Given - inject data into buffer using onNext first
        val payload = "Hello".toByteArray() // No newline, will remain in buffer
        `when`(frame.payload).thenReturn(payload)
        callback.onNext(frame)

        // When
        callback.onComplete()

        // Then
        // Verify first send for buffer content
        verify(emitter).send("Hello")
        // Verify second send for completion event
        verify(emitter).send(sseEventBuilder)
        verify(emitter).complete()
        assert(onCompleteCalled)
    }

    @Test
    fun `test onComplete handles emitter exception`() {
        // Given
        `when`(emitter.send(any<Any>())).thenThrow(RuntimeException("Connection closed"))

        // When - should not throw
        callback.onComplete()

        // Then
        assert(onCompleteCalled)
    }

    @Test
    fun `test onError sends completion event and error`() {
        // Given
        val error = RuntimeException("Test error")

        // When
        callback.onError(error)

        // Then
        // Verify that we're sending the SseEventBuilder, not a generic object
        verify(emitter).send(sseEventBuilder)
        verify(emitter).completeWithError(error)
        assert(onErrorCalled)
    }

    @Test
    fun `test onError handles emitter exception`() {
        // Given
        `when`(emitter.send(any<Any>())).thenThrow(RuntimeException("Connection closed"))

        // When - should not throw
        callback.onError(RuntimeException("Test error"))

        // Then
        assert(onErrorCalled)
    }

    @Test
    fun `test interrupt sets inactive and closes callback`() {
        // When
        callback.interrupt()

        // Then - verify it doesn't process further frames
        val payload = "Hello\n".toByteArray()
        `when`(frame.payload).thenReturn(payload)
        callback.onNext(frame)

        // No emitter sends should happen
        verify(emitter, never()).send(anyString())
    }
}
