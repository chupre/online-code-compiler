package com.example.onlinecodecompiler.execute

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "executions")
open class Execution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null

    @Column(name = "code", length = Integer.MAX_VALUE)
    open var code: String? = null

    @Column(name = "language", length = 20)
    @Enumerated(EnumType.STRING)
    open var language: Language? = null

    @Column(name = "output", length = Integer.MAX_VALUE)
    open var output: String? = null

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    open var status: Status? = null

    @Column(name = "execution_time_ms")
    open var executionTimeMs: Int? = null

    @Column(name = "created_at", updatable = false, insertable = false)
    open var createdAt: Instant? = null
}