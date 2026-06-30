package mx.infotec.imss.infrastructure.odwek.connection;

import com.ibm.edms.od.ODServer;

/**
 * Accion a ejecutar sobre una conexion {@link ODServer} ya tomada del pool y con
 * logon hecho. El template garantiza borrow/logon/logoff/return alrededor de esta
 * accion; la implementacion solo debe preocuparse por la logica de negocio
 * (abrir folder, buscar, recuperar) y SIEMPRE cerrar el ODFolder que abra.
 *
 * Declara {@code throws Exception} porque la API de ODWEK declara
 * java.lang.Exception en varios metodos (logon, openFolder, search...). El
 * template captura y traduce esas excepciones; la implementacion del callback no
 * necesita envolverlas.
 *
 * @param <T> tipo del resultado (DTOs propios, NO objetos ODHit/ODFolder, que no
 *            deben salir del alcance del execute).
 */
@FunctionalInterface
public interface ODServerCallback<T> {

    T doInServer(ODServer server) throws Exception;
}
