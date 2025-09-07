package com.example.onlinecodecompiler.execute

class ExecutionNotFoundException : RuntimeException {
    constructor() : super("Execution not found")
}