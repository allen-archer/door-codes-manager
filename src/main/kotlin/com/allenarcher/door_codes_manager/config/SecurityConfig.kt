package com.allenarcher.door_codes_manager.config

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(private val securityProperties: SecurityProperties) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(encoder: PasswordEncoder): UserDetailsService = InMemoryUserDetailsManager(
        securityProperties.users.map {
            User.builder()
                .username(it.name)
                .password(encoder.encode(it.password))
                .roles("ADMIN")
                .build()
        })

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // VaadinSecurityConfigurer keeps CSRF protection on, only exempting
        // Vaadin's own internal framework requests (which carry their own
        // token), instead of disabling it for the whole app.
        http.with(VaadinSecurityConfigurer.vaadin()) { it.anyRequest { auth -> auth.hasRole("ADMIN") } }
        http
            .formLogin { }
            .httpBasic { }
            .rememberMe { it.key(securityProperties.rememberMeKey).tokenValiditySeconds(60 * 60 * 24 * 14) }
        return http.build()
    }
}

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val rememberMeKey: String,
    val users: List<SecurityUsers>,
)

data class SecurityUsers(
    val name: String,
    val password: String,
)
