package com.example.onlinecodecompiler.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class CorsConfig : WebMvcConfigurer {
    @field:Value("\${cors.frontend-url}")
    private val frontendUrl: String? = null

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(frontendUrl)
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}