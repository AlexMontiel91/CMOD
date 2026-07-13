package com.app.icncards.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Bloqueo de intentos de login por usuario, en memoria. Protege el userid RACF de
 * ser revocado por intentos fallidos: al llegar a max-attempts, deja de aceptar
 * logons (no llegan a RACF) durante un enfriamiento.
 *
 * Solo debe registrarse un fallo cuando es de CREDENCIALES (no un error tecnico),
 * de lo contrario un problema de red bloquearia usuarios validos.
 *
 * Nota: es un contador por-instancia (memoria local). Con varias instancias detras
 * del F5, cada nodo cuenta por separado; como el F5 tiene afinidad de sesion, en la
 * practica un usuario pega siempre al mismo nodo. Si se requiere bloqueo global,
 * mover el contador a un store compartido (p. ej. Redis) mas adelante.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptProperties props;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** true si el usuario esta bloqueado ahora mismo. */
    public boolean isBlocked(String username) {
        Attempt a = attempts.get(key(username));
        if (a == null || a.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isBefore(a.lockedUntil)) {
            return true;
        }
        attempts.remove(key(username)); // enfriamiento vencido
        return false;
    }

    /** Registra un fallo de CREDENCIALES y bloquea si se alcanza el limite. */
    public void onFailure(String username) {
        Attempt a = attempts.computeIfAbsent(key(username), k -> new Attempt());
        a.count++;
        if (a.count >= props.getMaxAttempts()) {
            a.lockedUntil = Instant.now().plus(props.getLockMinutes(), ChronoUnit.MINUTES);
        }
    }

    /** Login exitoso: limpia el contador (equivale al reset de RACF por exito). */
    public void onSuccess(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class Attempt {
        private int count;
        private Instant lockedUntil;
    }
}
