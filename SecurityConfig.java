package mx.infotec.imss.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import lombok.RequiredArgsConstructor;

/**
 * Configuracion de Spring Security: form login server-rendered (FreeMarker) y
 * blindaje de sesion.
 *
 * IMPORTANTE: el timeout y las cookies de sesion NO se configuran aqui ni en
 * application.yml; los gobierna Liberty en server.xml (<httpSession>). Aqui solo
 * va lo que es responsabilidad de Spring: proveedor de autenticacion, rutas,
 * anti-fixation, logout que invalida la sesion (y con ella la credencial sellada).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(CipherProperties.class)
public class SecurityConfig {

    private final OnDemandAuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(authz -> authz
                .antMatchers("/login", "/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")            // GET: pantalla; POST: procesa (campos username/password)
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)     // borra la credencial sellada al salir
                .deleteCookies("JSESSIONID"))
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId()) // anti session-fixation
                .maximumSessions(1));            // una sesion por usuario
        return http.build();
    }

    /** Necesario para que el control de sesiones concurrentes reciba eventos del contenedor. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
