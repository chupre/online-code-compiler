package com.example.onlinecodecompiler.execute

import com.example.onlinecodecompiler.docker.DockerStreamCallback
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Service
class ExecutionRegistry {
    private val containers = ConcurrentHashMap<Long, String>()
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()
    private val callbacks = ConcurrentHashMap<Long, DockerStreamCallback>()

    fun registerContainer(executionId: Long, containerId: String) {
        containers[executionId] = containerId
    }

    fun getContainer(executionId: Long): String? = containers[executionId]

    fun removeContainer(executionId: Long) {
        containers.remove(executionId)
    }

    fun registerEmitter(executionId: Long, emitter: SseEmitter) {
        emitters[executionId] = emitter
    }

    fun removeEmitter(executionId: Long) {
        emitters.remove(executionId)
    }

    fun getEmitter(executionId: Long): SseEmitter? = emitters[executionId]

    fun registerCallback(executionId: Long, callback: DockerStreamCallback) {
        callbacks[executionId] = callback
    }

    fun getCallback(executionId: Long): DockerStreamCallback? = callbacks[executionId]

    fun removeCallback(executionId: Long) {
        callbacks.remove(executionId)
    }

    fun cleanupExecution(executionId: Long) {
        // Clean up all resources for an execution
        removeContainer(executionId)
        removeEmitter(executionId)
        removeCallback(executionId)
    }
}
