package com.app.icncards.infrastructure.odwek.connection;

/**
 * Puerto tecnico del modulo. Las capas de negocio (a traves de sus adaptadores de
 * salida) dependen de esta abstraccion, no del pool ni de ODServer directamente.
 */
public interface OnDemandOperations {

    /**
     * Toma una conexion del pool, hace logon con las credenciales dadas, ejecuta
     * la accion y garantiza logoff + devolucion (o invalidacion) de la conexion.
     *
     * @throws InvalidCredentialsException si el logon es rechazado por RACF
     *         (password invalido/revocado). La conexion es sana y se devuelve al
     *         pool; la capa superior debe invalidar la HttpSession y forzar
     *         re-login (NO reintentar, para no revocar el userid en RACF).
     * @throws PasswordExpiredException si el password es correcto pero vencio;
     *         la conexion es sana. La capa superior debe dirigir al usuario al
     *         cambio de password, NUNCA contar esto como intento fallido.
     * @throws OnDemandException ante error tecnico (red caida, servidor no
     *         disponible, fallo durante la transaccion).
     */
    <T> T execute(OnDemandCredentials credentials, ODServerCallback<T> action);

    /**
     * Cambia una contrasena VENCIDA usando el logon de 4 argumentos de ODWEK
     * (logon(host, user, oldPassword, newPassword)). Requiere la contrasena
     * actual (aunque este vencida) para que RACF autorice el cambio.
     *
     * @throws InvalidCredentialsException si oldPassword es incorrecto.
     * @throws OnDemandException ante fallo tecnico, o si newPassword no cumple
     *         la politica de contrasenas de RACF (el errorId especifico de esto
     *         aun no esta mapeado; capturar del log [ODWEK errorId] si aparece).
     */
    void changeExpiredPassword(String user, char[] oldPassword, char[] newPassword);
}
