package com.app.icncards.infrastructure.odwek.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ibm.edms.od.ODConfig;
import com.ibm.edms.od.ODConstant;
import com.ibm.edms.od.ODException;

import com.app.icncards.infrastructure.odwek.connection.ODErrorClassifier;
import com.app.icncards.infrastructure.odwek.connection.OnDemandOperations;
import com.app.icncards.infrastructure.odwek.connection.OnDemandTemplate;
import com.app.icncards.infrastructure.odwek.pool.ODServerPool;

/**
 * Configuracion del modulo OnDemand. Crea y cablea los beans del adaptador de
 * salida hacia CMOD:
 *
 *   ODConfig -> ODServerPool -> OnDemandTemplate
 *                  ^
 *           ODErrorClassifier
 *
 * Spring la detecta por component-scan (vive bajo el paquete de la aplicacion),
 * por eso no necesita registro en META-INF ni @AutoConfiguration: este modulo es
 * parte de backend-imss, no una libreria reutilizable.
 *
 * El ciclo de vida del pool (arranque/baja) lo gestiona el propio ODServerPool con
 * @PostConstruct/@PreDestroy.
 */
@Configuration
@EnableConfigurationProperties(OnDemandProperties.class)
public class OnDemandConfiguration {

    /** ODConfig es inmutable; se construye una vez y se comparte entre todos los shells. */
    @Bean
    public ODConfig odConfig(OnDemandProperties p) throws ODException {
        String traceDir = (p.getTraceDir() != null) ? p.getTraceDir() : p.getTempDir();
        int traceLevel = p.isTraceEnabled() ? p.getTraceLevel() : 0;
        return new ODConfig(
                ODConstant.PLUGIN,   // AfpViewer
                ODConstant.APPLET,   // LineViewer
                null,                // MetaViewer (default)
                p.getMaxHits(),      // tope de memoria
                "/applets",          // AppletDir
                p.getLanguage(),     // Language
                p.getTempDir(),      // TempDir
                traceDir,            // TraceDir
                traceLevel);         // TraceLevel (0 = off en prod)
    }

    @Bean
    public ODErrorClassifier odErrorClassifier(OnDemandProperties p) {
        return new ODErrorClassifier(p.getAuthErrorIds(), p.getPasswordExpiredErrorIds());
    }

    /** Pool con ciclo de vida propio (warm-up en @PostConstruct, cierre en @PreDestroy). */
    @Bean
    public ODServerPool odServerPool(OnDemandProperties p, ODConfig odConfig) {
        return new ODServerPool(p, odConfig);
    }

    @Bean
    public OnDemandOperations onDemandTemplate(ODServerPool pool,
                                               OnDemandProperties p,
                                               ODErrorClassifier classifier) {
        return new OnDemandTemplate(pool, p, classifier);
    }
}
