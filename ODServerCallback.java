package mx.infotec.imss.infrastructure.odwek.connection;

import com.ibm.edms.od.ODException;
import com.ibm.edms.od.ODServer;

/**
 * Accion a ejecutar sobre una conexion {@link ODServer} ya tomada del pool y con
 * logon hecho. El template garantiza borrow/logon/logoff/return alrededor de esta
 * accion; la implementacion solo debe preocuparse por la logica de negocio
 * (abrir folder, buscar, recuperar) y SIEMPRE cerrar el ODFolder que abra.
 *
 * @param <T> tipo del resultado (normalmente un DTO o lista de DTOs propios, NO
 *            objetos ODHit/ODFolder, que no deben salir del alcance del execute).
 */
@FunctionalInterface
public interface ODServerCallback<T> {

    T doInServer(ODServer server) throws ODException;
}
