package com.allenarcher.door_codes_manager.config

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.apache.logging.log4j.LogManager
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(private val securityProperties: SecurityProperties) {

    private val logger = LogManager.getLogger()!!

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
    fun oidcUserService(): OAuth2UserService<OidcUserRequest, OidcUser> {
        val delegate = OidcUserService()
        return OAuth2UserService { request ->
            val oidcUser = delegate.loadUser(request)
            val groups = oidcUser.claims["groups"] as? List<*> ?: emptyList<Any>()
            val authorities = oidcUser.authorities.toMutableSet()
            if (groups.contains(securityProperties.oauth2Group)) {
                authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
            }
            DefaultOidcUser(authorities, oidcUser.idToken, oidcUser.userInfo, "preferred_username")
        }
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        oidcUserService: OAuth2UserService<OidcUserRequest, OidcUser>
    ): SecurityFilterChain {
        if (!securityProperties.basicAuthEnabled && !securityProperties.oauth2Enabled) {
            logger.warn("AUTHENTICATION IS COMPLETELY DISABLED. This is not recommended as this app is open to any one who can reach it.")
            http.with(VaadinSecurityConfigurer.vaadin()) { it.anyRequest { auth -> auth.permitAll() } }
            return http.build()
        }
        // VaadinSecurityConfigurer keeps CSRF protection on, only exempting
        // Vaadin's own internal framework requests (which carry their own
        // token), instead of disabling it for the whole app.
        http.with(VaadinSecurityConfigurer.vaadin()) { it.anyRequest { auth -> auth.hasRole("ADMIN") } }
        if (securityProperties.basicAuthEnabled) {
            http.formLogin { }.httpBasic { }
        }
        if (securityProperties.oauth2Enabled) {
            http.oauth2Login { it.userInfoEndpoint { info -> info.oidcUserService(oidcUserService) } }
        }
        if (!securityProperties.rememberMeKey.isEmpty()) {
            http.rememberMe { it.key(securityProperties.rememberMeKey).tokenValiditySeconds(60 * 60 * 24 * 14) }
        }
        return http.build()
    }
}

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val rememberMeKey: String = "",
    val users: List<SecurityUsers> = emptyList(),
    val basicAuthEnabled: Boolean = true,
    val oauth2Enabled: Boolean = false,
    val oauth2Group: String = "door-codes-manager",
)

data class SecurityUsers(
    val name: String,
    val password: String,
)
