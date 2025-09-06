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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*

@Service
class ExecuteService {
    var dockerConfig: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    var dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()

    var dockerClient: DockerClient = DockerClientImpl.getInstance(dockerConfig, dockerHttpClient)

    fun execute(codeDto: CodeDto): String {
        val language = Language.valueOf(codeDto.language)

        val tempDir = Files.createTempDirectory("${language.extension}-exec-${UUID.randomUUID()}")
        val codeFile = tempDir.resolve("main.${language.extension}")
        Files.writeString(codeFile, codeDto.code)

        val container = dockerClient.createContainerCmd("sandbox-${language.extension}")
            .withWorkingDir("/home/runner")
            .withCmd("sleep", "60") // keep container alive temporarily
            .exec()

        dockerClient.startContainerCmd(container.id).exec()

        copyFileToContainer(codeFile, container.id, "/home/runner")

        val script = if (language.isCompiled) {
            """
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
            ${language.runCmd}
            """.trimIndent()
        }

        val execCreateCmd = dockerClient.execCreateCmd(container.id)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("sh", "-c", script)
            .exec()

        val output = ByteArrayOutputStream()
        dockerClient.execStartCmd(execCreateCmd.id)
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(item: Frame) {
                    output.writeBytes(item.payload)
                }
            }).awaitCompletion()

        dockerClient.removeContainerCmd(container.id).withForce(true).exec()

        return output.toString()
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
}