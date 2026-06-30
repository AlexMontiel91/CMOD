package mx.infotec.imss.infrastructure.odwek.connection;

/**
 * Fallo de autenticacion al hacer logon (password incorrecto, userid revocado o
 * password expirado). Semanticamente distinta de un error tecnico: la conexion
 * del pool sigue sana (el logon nunca llego a establecer sesion), por lo que se
 * DEVUELVE al pool, no se invalida.
 *
 * La capa superior debe responder invalidando la HttpSession y mandando a
 * re-login. Nunca reintentar con la misma credencial almacenada: reintentos
 * fallidos consecutivos pueden REVOCAR el userid en RACF.
 */
public class InvalidCredentialsException extends OnDemandException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
