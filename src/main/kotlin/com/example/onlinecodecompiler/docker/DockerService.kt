package com.example.onlinecodecompiler.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ulimit
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.time.Duration
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.nio.file.Files

@Service
class DockerService {

    companion object {
        // Resource limits for security
        private const val MEMORY_LIMIT = 128 * 1024 * 1024L // 128MB
        private const val MEMORY_SWAP_LIMIT = MEMORY_LIMIT // No additional swap
        private const val CPU_QUOTA = 50000L // 0.5 CPU (50% of 100ms period)
        private const val CPU_PERIOD = 100000L // 100ms
        private const val EXECUTION_TIMEOUT_SECONDS = 30L
        private const val MAX_PROCESSES = 64 // Limit number of processes
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB max file size
    }

    private val dockerConfig: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    private val dockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()

    val dockerClient: DockerClient = DockerClientImpl.getInstance(dockerConfig, dockerHttpClient)

    fun copyFileToContainer(file: Path, containerId: String, destPath: String) {
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

    fun cleanup(containerId: String) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec()
        } catch (_: Exception) {}
    }

    fun createContainer(imageName: String): String {
        // Create host config with strict resource limitations
        val hostConfig = HostConfig.newHostConfig()
            // Memory limitations
            .withMemory(MEMORY_LIMIT)
            .withMemorySwap(MEMORY_SWAP_LIMIT)
            .withOomKillDisable(false) // Kill if out of memory

            // CPU limitations
            .withCpuQuota(CPU_QUOTA)
            .withCpuPeriod(CPU_PERIOD)
            .withCpuShares(512) // Lower priority

            // Network isolation - no network access
            .withNetworkMode("none")

            // Security constraints
            .withPrivileged(false) // No privileged access
            .withCapDrop(Capability.ALL) // Drop all capabilities

            // Create tmpfs for writable workspace (in-memory, no persistence)
            .withTmpFs(mapOf(
                "/workspace" to "rw,exec,size=${MAX_FILE_SIZE},uid=1000,gid=1000"
                // Removed /tmp tmpfs mount as it conflicts with file copying
            ))

            // Process and file limitations
            .withPidsLimit(MAX_PROCESSES.toLong())
            .withUlimits(listOf(
                Ulimit("fsize", MAX_FILE_SIZE, MAX_FILE_SIZE), // Max file size
                Ulimit("nofile", 1024L, 1024L), // Max open files
                Ulimit("nproc", MAX_PROCESSES.toLong(), MAX_PROCESSES.toLong()) // Max processes
            ))

            // Additional security: no new privileges
            .withSecurityOpts(listOf("no-new-privileges:true"))

            // Disable swap accounting if needed
            .withMemorySwappiness(0L)

        val container = dockerClient.createContainerCmd(imageName)
            .withWorkingDir("/workspace")
            .withCmd("sleep", "${EXECUTION_TIMEOUT_SECONDS + 10}") // Sleep slightly longer than execution timeout
            .withHostConfig(hostConfig)
            // Additional container-level security
            .withUser("1000:1000") // Run as non-root user
            .withEnv("HOME=/workspace") // Set home to writable directory
            .exec()

        dockerClient.startContainerCmd(container.id).exec()
        return container.id
    }

    fun executeCommand(
        containerId: String,
        command: String,
        callback: ResultCallback<Frame>
    ) {
        val execCreateCmd = dockerClient.execCreateCmd(containerId)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withTty(true)
            .withCmd("sh", "-c", "timeout -k 5 ${EXECUTION_TIMEOUT_SECONDS} sh -c '$command'")
            .withUser("1000:1000") // Ensure non-root execution
            .exec()

        dockerClient.execStartCmd(execCreateCmd.id).exec(callback)
    }
}
