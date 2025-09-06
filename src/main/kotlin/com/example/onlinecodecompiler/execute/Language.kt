package com.example.onlinecodecompiler.execute

enum class Language(
    val isCompiled: Boolean,
    val extension : String,
    val compileCmd: String?,
    val runCmd: String
) {
    C(
        isCompiled = true,
        extension = "c",
        compileCmd = "gcc main.c -o main",
        runCmd = "./main"
    ),
    PYTHON(
        isCompiled = false,
        extension = "py",
        compileCmd = null,
        runCmd = "python3 main.py"
    )
}