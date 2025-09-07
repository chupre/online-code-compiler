package com.example.onlinecodecompiler.execute

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExecutionRepository : JpaRepository<Execution, Long> {

}