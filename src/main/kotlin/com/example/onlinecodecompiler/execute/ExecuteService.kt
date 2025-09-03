package com.example.onlinecodecompiler.execute

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Files
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

        val containerVolume = Volume("/home/runner")
        val bind = Bind(tempDir.toAbsolutePath().toString(), containerVolume)

        val script = if (language.isCompiled) {
            """
                ${language.compileCmd}
                if [ $? -ne 0 ]; then
                  cat compile_error.txt
                  exit 1
                fi
                ${language.runCmd}
            """.trimIndent()
        } else {
            """
                ${language.runCmd}
            """.trimIndent()
        }

        val cmd = arrayOf("sh", "-c", script)

        val container = dockerClient.createContainerCmd("sandbox-${language.extension}")
            .withHostConfig(HostConfig.newHostConfig().withBinds(bind))
            .withCmd(*cmd)
            .exec()

        dockerClient.startContainerCmd(container.id).exec()

        val output = ByteArrayOutputStream()
        dockerClient.logContainerCmd(container.id)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(item: Frame) {
                    output.writeBytes(item.payload)
                }
            }).awaitCompletion()

        val exitCode = dockerClient.waitContainerCmd(container.id)
            .start().awaitStatusCode()

        dockerClient.removeContainerCmd(container.id).withForce(true).exec()

        return output.toString()
    }
}