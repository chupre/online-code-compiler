package com.example.onlinecodecompiler.execute

import com.example.onlinecodecompiler.docker.DockerService
import com.example.onlinecodecompiler.docker.DockerStreamCallback
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.nio.file.Files
import java.time.Instant
import java.util.*

@Service
class ExecuteService(
    private val executionRepository: ExecutionRepository,
    private val registry: ExecutionRegistry,
    private val dockerService: DockerService
) {
    fun createExecution(codeDto: CodeDto): Long? {
        val execution = Execution()
        execution.code = codeDto.code
        execution.language = Language.valueOf(codeDto.language)
        execution.status = Status.PENDING
        val saved = executionRepository.save(execution)
        return saved.id
    }

    fun runExecution(id: Long): SseEmitter {
        val execution = executionRepository.findById(id).orElseThrow { ExecutionNotFoundException() }

        // Record execution start time
        val startTime = System.currentTimeMillis()
        execution.executedAt = Instant.now()
        executionRepository.save(execution)

        val emitter = SseEmitter(0)

        // Configure SSE event handlers
        setupSseEventHandlers(id, emitter)
        registry.registerEmitter(id, emitter)

        Thread {
            try {
                // Create temp directory and code file
                val tempDir = Files.createTempDirectory("${execution.language?.extension}-exec-${UUID.randomUUID()}")
                val codeFile = tempDir.resolve("main.${execution.language?.extension}")
                Files.writeString(codeFile, execution.code ?: throw IllegalStateException("Code is null"))

                // Create and start container
                val containerImage = "sandbox-${execution.language?.extension}"
                val containerId = dockerService.createContainer(containerImage)
                registry.registerContainer(id, containerId)

                // Copy code file to container (to /home/runner, then move to /workspace)
                dockerService.copyFileToContainer(codeFile, containerId, "/home/runner")

                // Prepare execution script that moves file to workspace and executes
                val script = createExecutionScript(execution.language)

                // Create Docker callback handler
                val callback = createDockerCallback(id, emitter, execution, startTime)
                registry.registerCallback(id, callback)

                // Execute command in container
                dockerService.executeCommand(containerId, script, callback)

            } catch (ex: Exception) {
                emitter.completeWithError(ex)
                registry.cleanupExecution(id)
            }
        }.start()

        return emitter
    }

    private fun setupSseEventHandlers(id: Long, emitter: SseEmitter) {
        // Handler for client disconnection
        emitter.onCompletion {
            handleClientDisconnection(id)
        }

        // Handler for emitter errors
        emitter.onError { _ ->
            handleClientDisconnection(id)
        }
    }

    private fun handleClientDisconnection(id: Long) {
        val containerId = registry.getContainer(id)
        if (containerId != null) {
            // Stop container
            dockerService.cleanup(containerId)

            // Update execution status
            val currentExecution = executionRepository.findById(id).orElse(null)
            if (currentExecution != null && currentExecution.status != Status.OK) {
                currentExecution.status = Status.INTERRUPTED
                executionRepository.save(currentExecution)
            }

            // Clean up resources
            registry.cleanupExecution(id)
        }
    }

    private fun createExecutionScript(language: Language?): String {
        val fileName = "main.${language?.extension}"
        return if (language?.isCompiled == true) {
            """
            # Find and copy the file to workspace
            SOURCE_FILE=${'$'}(find /home/runner -name "$fileName" -type f | head -1)
            if [ -z "${'$'}SOURCE_FILE" ]; then
                echo "Error: $fileName not found in /home/runner"
                exit 1
            fi
            
            cp "${'$'}SOURCE_FILE" /workspace/$fileName
            cd /workspace
            ${language.compileCmd} 2> compile_error.txt
            if [ $? -ne 0 ]; then
              cat compile_error.txt
              exit 1
            fi
            chmod +x main
            ./main
            """.trimIndent()
        } else {
            """
            # Find and copy the file to workspace
            SOURCE_FILE=${'$'}(find /home/runner -name "$fileName" -type f | head -1)
            if [ -z "${'$'}SOURCE_FILE" ]; then
                echo "Error: $fileName not found in /home/runner"
                exit 1
            fi
            
            cp "${'$'}SOURCE_FILE" /workspace/$fileName
            cd /workspace
            ${language?.runCmd ?: ""}
            """.trimIndent()
        }
    }

    private fun createDockerCallback(
        id: Long,
        emitter: SseEmitter,
        execution: Execution,
        startTime: Long
    ): DockerStreamCallback {
        return DockerStreamCallback(
            emitter = emitter,
            onComplete = {
                // Calculate and save execution time
                val executionTime = System.currentTimeMillis() - startTime
                execution.executionTimeMs = executionTime.toInt()
                execution.status = Status.OK
                executionRepository.save(execution)

                // Clean up resources
                val containerId = registry.getContainer(id)
                if (containerId != null) {
                    dockerService.cleanup(containerId)
                }
                registry.cleanupExecution(id)
            },
            onError = { _ ->
                // Calculate and save execution time for errors too
                val executionTime = System.currentTimeMillis() - startTime
                execution.executionTimeMs = executionTime.toInt()
                execution.status = Status.INTERRUPTED
                executionRepository.save(execution)

                // Clean up resources
                val containerId = registry.getContainer(id)
                if (containerId != null) {
                    dockerService.cleanup(containerId)
                }
                registry.cleanupExecution(id)
            },
            onInterrupt = {
                // Handle execution interruption
                val containerId = registry.getContainer(id)
                if (containerId != null) {
                    dockerService.cleanup(containerId)
                }
                execution.status = Status.INTERRUPTED
                executionRepository.save(execution)
                registry.cleanupExecution(id)
            }
        )
    }

    fun stopExecution(id: Long) {
        val containerId = registry.getContainer(id) ?: return
        dockerService.cleanup(containerId)
        registry.getEmitter(id)?.complete()
        registry.cleanupExecution(id)
    }

    fun findExecution(id: Long): Execution {
        return executionRepository.findById(id).orElseThrow { ExecutionNotFoundException() }
    }
}