package com.example.onlinecodecompiler.docker

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * A wrapper for Docker callbacks that handles streaming output to an SSE emitter
 * with proper connection state tracking.
 */
class DockerStreamCallback(
    private val emitter: SseEmitter,
    private val onComplete: () -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onInterrupt: () -> Unit
) : ResultCallback.Adapter<Frame>() {

    private val buffer = StringBuilder()
    private val connectionActive = AtomicBoolean(true)

    override fun onNext(frame: Frame) {
        // First check if connection is still active before processing
        if (!connectionActive.get()) {
            this.close()
            return
        }

        val chunk = String(frame.payload)
        buffer.append(chunk)
        var index: Int

        while (connectionActive.get() && true) {
            index = buffer.indexOf("\n")
            if (index == -1) break

            val line = buffer.substring(0, index + 1)
            buffer.delete(0, index + 1)

            try {
                emitter.send(line)
            } catch (ex: Exception) {
                // Client disconnected, clean up and stop processing
                connectionActive.set(false)
                this.close() // Close the Docker stream immediately
                onInterrupt()
                return
            }
        }
    }

    override fun onComplete() {
        if (buffer.isNotEmpty() && connectionActive.get()) {
            try {
                emitter.send(buffer.toString())
            } catch (ex: Exception) {
                // Ignore send errors on completion
            }
        }

        try {
            // Only try to send and complete if connection is still active
            if (connectionActive.get()) {
                emitter.send(SseEmitter.event().name("complete").data("done"))
                emitter.complete()
            }
        } catch (ex: Exception) {
            // Emitter might already be completed if client disconnected
        }

        // Call the onComplete handler passed in the constructor, not this method itself
        onComplete.invoke()
    }

    override fun onError(throwable: Throwable) {
        try {
            // Only try to send and complete with error if connection is still active
            if (connectionActive.get()) {
                emitter.send(SseEmitter.event().name("complete").data("done"))
                emitter.completeWithError(throwable)
            }
        } catch (ex: Exception) {
            // Emitter might already be completed if client disconnected
        }

        // Call the onError handler passed in the constructor, not this method itself
        onError.invoke(throwable)
    }

    /**
     * Stops processing and marks the connection as inactive
     */
    fun interrupt() {
        connectionActive.set(false)
        this.close()
    }
}
