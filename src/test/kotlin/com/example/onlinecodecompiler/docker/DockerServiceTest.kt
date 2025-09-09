package com.example.onlinecodecompiler.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.ExecCreateCmd
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.command.ExecStartCmd
import com.github.dockerjava.api.command.RemoveContainerCmd
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.Frame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
class DockerServiceTest {

    @Mock
    private lateinit var dockerClient: DockerClient

    @Mock
    private lateinit var createContainerCmd: CreateContainerCmd

    @Mock
    private lateinit var startContainerCmd: StartContainerCmd

    @Mock
    private lateinit var removeContainerCmd: RemoveContainerCmd

    @Mock
    private lateinit var execCreateCmd: ExecCreateCmd

    @Mock
    private lateinit var execStartCmd: ExecStartCmd

    @Mock
    private lateinit var containerResponse: CreateContainerResponse

    @Mock
    private lateinit var execCreateResponse: ExecCreateCmdResponse

    @Mock
    private lateinit var callback: ResultCallback<Frame>

    private lateinit var dockerService: DockerService
    private lateinit var tempFile: Path

    @BeforeEach
    fun setUp() {
        // Create a real temp file for testing
        tempFile = Files.createTempFile("test", ".txt")
        Files.writeString(tempFile, "test content")

        // Create service with mocked Docker client
        dockerService = DockerService()
        // Use reflection to set the mocked Docker client
        val field = DockerService::class.java.getDeclaredField("dockerClient")
        field.isAccessible = true
        field.set(dockerService, dockerClient)
    }

    @Test
    fun `test createContainer creates and starts container`() {
        // Given
        `when`(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd)
        `when`(createContainerCmd.withWorkingDir(anyString())).thenReturn(createContainerCmd)
        `when`(createContainerCmd.withCmd(anyString(), anyString())).thenReturn(createContainerCmd)
        `when`(createContainerCmd.exec()).thenReturn(containerResponse)
        `when`(containerResponse.id).thenReturn("container123")
        `when`(dockerClient.startContainerCmd("container123")).thenReturn(startContainerCmd)

        // When
        val containerId = dockerService.createContainer("test-image")

        // Then
        verify(dockerClient).createContainerCmd("test-image")
        verify(createContainerCmd).withWorkingDir("/home/runner")
        verify(createContainerCmd).withCmd("sleep", "60")
        verify(createContainerCmd).exec()
        verify(dockerClient).startContainerCmd("container123")
        verify(startContainerCmd).exec()
        assert(containerId == "container123")
    }

    @Test
    fun `test cleanup removes container`() {
        // Given
        `when`(dockerClient.removeContainerCmd("container123")).thenReturn(removeContainerCmd)
        `when`(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd)

        // When
        dockerService.cleanup("container123")

        // Then
        verify(dockerClient).removeContainerCmd("container123")
        verify(removeContainerCmd).withForce(true)
        verify(removeContainerCmd).exec()
    }

    @Test
    fun `test executeCommand executes command in container`() {
        // Given
        `when`(dockerClient.execCreateCmd("container123")).thenReturn(execCreateCmd)
        `when`(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd)
        `when`(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd)
        `when`(execCreateCmd.withTty(true)).thenReturn(execCreateCmd)
        `when`(execCreateCmd.withCmd(anyString(), anyString(), anyString())).thenReturn(execCreateCmd)
        `when`(execCreateCmd.exec()).thenReturn(execCreateResponse)
        `when`(execCreateResponse.id).thenReturn("exec123")
        `when`(dockerClient.execStartCmd("exec123")).thenReturn(execStartCmd)

        // When
        dockerService.executeCommand("container123", "echo hello", callback)

        // Then
        verify(dockerClient).execCreateCmd("container123")
        verify(execCreateCmd).withAttachStdout(true)
        verify(execCreateCmd).withAttachStderr(true)
        verify(execCreateCmd).withTty(true)
        verify(execCreateCmd).withCmd("sh", "-c", "echo hello")
        verify(execCreateCmd).exec()
        verify(dockerClient).execStartCmd("exec123")
        verify(execStartCmd).exec(callback)
    }

    @Test
    fun `test cleanup handles exception gracefully`() {
        // Given
        `when`(dockerClient.removeContainerCmd("container123")).thenReturn(removeContainerCmd)
        `when`(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd)
        `when`(removeContainerCmd.exec()).thenThrow(RuntimeException("Container not found"))

        // When - this should not throw
        dockerService.cleanup("container123")

        // Then
        verify(dockerClient).removeContainerCmd("container123")
        verify(removeContainerCmd).withForce(true)
    }

    @Test
    fun `test cleanup with invalid container id`() {
        // Given
        `when`(dockerClient.removeContainerCmd("invalid-container")).thenReturn(removeContainerCmd)
        `when`(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd)

        // When
        dockerService.cleanup("invalid-container")

        // Then - should not throw
        verify(dockerClient).removeContainerCmd("invalid-container")
    }
}
