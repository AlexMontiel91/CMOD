package mx.infotec.imss.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import lombok.RequiredArgsConstructor;

/**
 * Configuracion de Spring Security. La autenticacion la orquesta LoginController
 * con el AuthenticationManager (para validacion backend + bloqueo de intentos), no
 * el filtro de formLogin. Aqui se define autorizacion, entry point, logout,
 * cabeceras de seguridad (OWASP) y el repositorio de contexto.
 *
 * El timeout y las cookies de sesion los gobierna Liberty en server.xml, no aqui.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({CipherProperties.class, LoginAttemptProperties.class})
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .antMatchers("/login", "/css/**", "/js/**", "/fonts/**", "/webjars/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
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
