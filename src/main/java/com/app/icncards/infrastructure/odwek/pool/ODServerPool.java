package com.app.icncards.infrastructure.odwek.pool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.ibm.edms.od.ODConfig;
import com.ibm.edms.od.ODServer;

import lombok.extern.slf4j.Slf4j;
import com.app.icncards.infrastructure.odwek.config.OnDemandProperties;
import com.app.icncards.infrastructure.odwek.connection.OnDemandException;

/**
 * Pool de conexiones {@link ODServer} para ODWEK. Encapsula TODO el ciclo de vida
 * en un solo punto observable:
 *
 *   ARRANQUE  -> {@link #start()} (@PostConstruct): precrea los shells (warm-up).
 *   BAJA      -> {@link #shutdown()} (@PreDestroy): logoff + terminate de todos.
 *   BORROW    -> {@link #borrow()}: toma un shell (validado en borrow).
 *   RETIRO    -> {@link #release(ODServer)} / {@link #invalidate(ODServer)}.
 *   VALIDACION-> delegada a {@link ODServerPooledObjectFactory#validateObject}.
 *
 * Modelo "shell": el pool guarda objetos inicializados SIN logon; el logon/logoff
 * por usuario lo hace el template en cada transaccion.
 *
 * Nota: no usa @RequiredArgsConstructor porque el constructor ademas CONSTRUYE el
 * pool (buildPool), no solo asigna campos.
 */
@Slf4j
public class ODServerPool {

    private final OnDemandProperties props;
    private final GenericObjectPool<ODServer> pool;

    public ODServerPool(OnDemandProperties props, ODConfig odConfig) {
        this.props = props;
        this.pool = buildPool(props, odConfig);
    }

    /** Construye el GenericObjectPool con su factory y la configuracion de sizing/validacion. */
    private static GenericObjectPool<ODServer> buildPool(OnDemandProperties props, ODConfig odConfig) {
        OnDemandProperties.Pool pp = props.getPool();

        GenericObjectPoolConfig<ODServer> cfg = new GenericObjectPoolConfig<>();
        cfg.setMaxTotal(pp.getMaxTotal());            // = transacciones OnDemand concurrentes
        cfg.setMaxIdle(pp.getMaxIdle());
        cfg.setMinIdle(pp.getMinIdle());
        cfg.setMaxWaitMillis(pp.getMaxWaitMillis());  // espera maxima por un shell antes de fallar
        cfg.setBlockWhenExhausted(true);

        // --- VALIDACION ---
        cfg.setTestOnBorrow(pp.isTestOnBorrow());     // valida (isInitialized) antes de prestar
        cfg.setTestOnReturn(false);
        cfg.setTestWhileIdle(pp.isTestWhileIdle());   // el evictor barre shells inservibles
        cfg.setTimeBetweenEvictionRunsMillis(pp.getTimeBetweenEvictionRunsMillis());
        cfg.setMinEvictableIdleTimeMillis(pp.getMinEvictableIdleTimeMillis());
        cfg.setNumTestsPerEvictionRun(-1);            // -1 = revisa todos los idle en cada corrida

        cfg.setJmxEnabled(true);
        cfg.setJmxNameBase("odwek.pool.");
        cfg.setJmxNamePrefix("odServer");

        return new GenericObjectPool<>(new ODServerPooledObjectFactory(props, odConfig), cfg);
    }

    // ===================== ARRANQUE =====================

    /**
     * Calienta el pool al arrancar la aplicacion: precrea los minIdle shells
     * (new ODServer + initialize, SIN logon). No lanza acciones de usuario.
     * Si falla, no tumba el arranque: el pool creara conexiones bajo demanda.
     */
    @PostConstruct
    public void start() {
        if (!props.getPool().isWarmUpOnStartup()) {
            log.info("Warm-up del pool ODWEK desactivado; las conexiones se crearan bajo demanda");
            return;
        }
        try {
            pool.preparePool();
            log.info("Pool ODWEK calentado: {} idle / maxTotal {}", pool.getNumIdle(), pool.getMaxTotal());
        } catch (Exception e) {
            log.warn("No se pudo precalentar el pool ODWEK; se crearan conexiones bajo demanda", e);
        }
    }

    // ===================== BAJA =====================

    /**
     * Cierra el pool al bajar la aplicacion. pool.close() invoca destroyObject sobre
     * cada shell (logoff + terminate), liberando recursos en el servidor OnDemand.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Cerrando pool ODWEK (active={}, idle={})...", pool.getNumActive(), pool.getNumIdle());
        try {
            pool.close(); // -> destroyObject por cada conexion (logoff + terminate)
            log.info("Pool ODWEK cerrado correctamente");
        } catch (Exception e) {
            log.warn("Error al cerrar el pool ODWEK", e);
        }
    }

    // ===================== BORROW =====================

    /**
     * Toma un shell del pool. El objeto se valida (isInitialized) antes de entregarse;
     * si la validacion falla, Commons Pool2 lo destruye y crea uno nuevo de forma
     * transparente. Si el pool esta agotado, espera hasta maxWaitMillis y luego falla.
     */
    public ODServer borrow() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            // incluye NoSuchElementException por timeout (pool agotado)
            throw new OnDemandException("No se pudo obtener una conexion OnDemand del pool", e);
        }
    }

    // ===================== RETIRO / DEVOLUCION =====================

    /** Devuelve un shell sano al pool para que se reutilice. */
    public void release(ODServer server) {
        if (server == null) {
            return;
        }
        try {
            pool.returnObject(server);
        } catch (Exception e) {
            log.warn("Fallo al devolver la conexion al pool; se intenta invalidar", e);
            invalidate(server);
        }
    }

    /** Saca del pool un shell sospechoso (lo destruye); el pool recreara otro segun minIdle. */
    public void invalidate(ODServer server) {
        if (server == null) {
            return;
        }
        try {
            pool.invalidateObject(server);
        } catch (Exception e) {
            log.warn("Fallo al invalidar la conexion del pool", e);
        }
    }

    // ===================== METRICAS =====================

    public int getActive()   { return pool.getNumActive(); }
    public int getIdle()     { return pool.getNumIdle(); }
    public int getWaiters()  { return pool.getNumWaiters(); }
    public int getMaxTotal() { return pool.getMaxTotal(); }
    public boolean isClosed(){ return pool.isClosed(); }

    /** Snapshot legible para logs o un health indicator. */
    public String stats() {
        return String.format("ODServerPool[active=%d, idle=%d, waiters=%d, maxTotal=%d]",
                getActive(), getIdle(), getWaiters(), getMaxTotal());
    }
}
