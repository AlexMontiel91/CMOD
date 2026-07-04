package mx.infotec.imss.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuracion de Spring Security. La autenticacion la orquesta LoginController
 * con el AuthenticationManager (para validacion backend + bloqueo de intentos), no
 * el filtro de formLogin. Aqui se define autorizacion, entry point, logout,
 * cabeceras de seguridad (OWASP) y el repositorio de contexto.
 *
 * El timeout y las cookies de sesion los gobierna Liberty en server.xml, no aqui.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({CipherProperties.class, LoginAttemptProperties.class,
        IdleSessionProperties.class, PasswordExpiryProperties.class})
public class SecurityConfig {

    private final OnDemandAuthenticationProvider authenticationProvider;

    /** AuthenticationManager con nuestro provider, para uso programatico en el controller. */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider);
    }

    /** Persistencia del SecurityContext en la HttpSession. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Distingue el logout manual del logout disparado por el temporizador de
     * inactividad (idle-timeout.js hace POST a /logout?reason=idle), para mostrar
     * un mensaje distinto en /login.
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            String reason = request.getParameter("reason");
            String target = "idle".equals(reason) ? "/login?idle" : "/login?logout";
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    /**
     * Maneja 403 (CSRF invalido/expirado, autorizacion denegada) sin dejar caer al
     * usuario en la pagina blanca default de Spring. Redirige a login con aviso.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, e) -> {
            log.warn("Acceso denegado en '{}': {}", request.getRequestURI(), e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login?denied");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .antMatchers("/login", "/css/**", "/js/**", "/fonts/**", "/webjars/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                .accessDeniedHandler(accessDeniedHandler()))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            // Cabeceras de seguridad (OWASP A05). CSRF queda activo por defecto.
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; "
                      + "script-src 'self' https://cdn.tailwindcss.com 'unsafe-inline'; "
                      + "style-src 'self' 'unsafe-inline'; "
                      + "img-src 'self' data:; "
                      + "frame-ancestors 'none'"))
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
