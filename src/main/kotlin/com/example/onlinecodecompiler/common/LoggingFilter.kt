package com.example.onlinecodecompiler.common

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class LoggingFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        @NonNull response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        println("Request: " + request.requestURI)
        filterChain.doFilter(request, response)
        println("Response: " + response.status)
    }
}