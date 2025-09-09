package com.example.onlinecodecompiler.execute

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "executions")
class Execution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "code", length = Integer.MAX_VALUE)
    var code: String? = null

    @Column(name = "language", length = 20)
    @Enumerated(EnumType.STRING)
    var language: Language? = null

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    var status: Status? = null

    @Column(name = "execution_time_ms")
    var executionTimeMs: Int? = null

    @Column(name = "created_at", updatable = false, insertable = false)
    var createdAt: Instant? = null

    @Column(name = "executed_at")
    var executedAt: Instant? = null
}