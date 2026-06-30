package mx.infotec.imss.infrastructure.odwek.connection;

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
     *         (password invalido/revocado/expirado). La conexion es sana y se
     *         devuelve al pool; la capa superior debe invalidar la HttpSession y
     *         forzar re-login (NO reintentar, para no revocar el userid en RACF).
     * @throws OnDemandException ante error tecnico (red caida, servidor no
     *         disponible, fallo durante la transaccion).
     */
    <T> T execute(OnDemandCredentials credentials, ODServerCallback<T> action);
}
