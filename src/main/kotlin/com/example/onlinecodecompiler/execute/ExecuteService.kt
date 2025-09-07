package com.example.onlinecodecompiler.execute

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*

@Service
class ExecuteService(private val executionRepository: ExecutionRepository) {
    var dockerConfig: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    var dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()

    var dockerClient: DockerClient = DockerClientImpl.getInstance(dockerConfig, dockerHttpClient)

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

        val emitter = SseEmitter(0)

        Thread {
            try {
                val tempDir = Files.createTempDirectory("${execution.language?.extension}-exec-${UUID.randomUUID()}")
                val codeFile = tempDir.resolve("main.${execution.language?.extension}")
                Files.writeString(codeFile, execution.code ?: throw IllegalStateException("Code is null"))

                val container = dockerClient.createContainerCmd("sandbox-${execution.language?.extension}")
                    .withWorkingDir("/home/runner")
                    .withCmd("sleep", "60")
                    .exec()

                dockerClient.startContainerCmd(container.id).exec()
                copyFileToContainer(codeFile, container.id, "/home/runner")

                val script = if (execution.language?.isCompiled ?: false) {
                    """
                ${execution.language?.compileCmd} 2> compile_error.txt
                if [ $? -ne 0 ]; then
                  cat compile_error.txt
                  exit 1
                fi
                chmod +x main
                ./main
                """.trimIndent()
                } else {
                    execution.language?.runCmd
                }

                val execCreateCmd = dockerClient.execCreateCmd(container.id)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withCmd("sh", "-c", script)
                    .exec()

                val buffer = StringBuilder()

                dockerClient.execStartCmd(execCreateCmd.id).exec(object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame) {
                        val chunk = String(frame.payload)
                        buffer.append(chunk)
                        var index: Int
                        while (true) {
                            index = buffer.indexOf("\n")
                            if (index == -1) break
                            val line = buffer.substring(0, index + 1)
                            buffer.delete(0, index + 1)
                            try {
                                emitter.send(line)
                            } catch (_: Exception) {
                                cleanup(container.id)
                                return
                            }
                        }
                    }

                    override fun onComplete() {
                        if (buffer.isNotEmpty()) {
                            try {
                                emitter.send(buffer.toString())
                            } catch (_: Exception) {}
                        }
                        cleanup(container.id)
                        emitter.send(SseEmitter.event().name("complete").data("done"))
                        emitter.complete()
                    }

                    override fun onError(throwable: Throwable) {
                        cleanup(container.id)
                        emitter.send(SseEmitter.event().name("complete").data("done"))
                        emitter.completeWithError(throwable)
                    }
                })

            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }.start()

        return emitter
    }

    private fun copyFileToContainer(file: Path, containerId: String, destPath: String) {
        val tarBytes = ByteArrayOutputStream()
        TarArchiveOutputStream(tarBytes).use { tarOut ->
            val entry = TarArchiveEntry(file.toFile(), file.fileName.toString())
            entry.size = Files.size(file)
            tarOut.putArchiveEntry(entry)
            Files.newInputStream(file).use { input -> input.copyTo(tarOut) }
            tarOut.closeArchiveEntry()
        }

        dockerClient.copyArchiveToContainerCmd(containerId)
            .withTarInputStream(ByteArrayInputStream(tarBytes.toByteArray()))
            .withRemotePath(destPath)
            .exec()
    }

    private fun cleanup(containerId: String) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec()
        } catch (_: Exception) {}
    }
}