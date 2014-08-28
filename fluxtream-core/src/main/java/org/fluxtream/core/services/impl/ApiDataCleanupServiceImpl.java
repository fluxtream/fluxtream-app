package org.fluxtream.core.services.impl;

import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.utils.JPAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * User: candide
 * Date: 18/08/14
 * Time: 20:32
 */
@Service
public class ApiDataCleanupServiceImpl implements ApiDataCleanupService {

    static FlxLogger logger = FlxLogger.getLogger(ApiDataServiceImpl.class);

    @Autowired
    @Qualifier("txTemplate")
    TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;

    @PersistenceContext
    EntityManager em;

    @Override
    public void cleanupStaleData() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Set<BeanDefinition> components = scanner.findCandidateComponents("org.fluxtream");
        for (BeanDefinition component : components) {
            Class cls = Class.forName(component.getBeanClassName());
            final String entityName = JPAUtils.getEntityName(cls);
            System.out.println("cleaning up " + entityName + "...");
            if (entityName.startsWith("Facet_")) {
                if (!JPAUtils.hasRelation(cls)) {
                    // Clean up entries for apiKeyId's which are no longer present in the system, but preserve items with
                    // api=0 to preserve the locations generated from reverse IP lookup when the users log in.
                    cleanupStaleFacetsInBulk(entityName);
                } else {
                    cleanupStaleFacetEntities(cls, entityName);
                }
            }
        }
        final int i = jdbcTemplate.update("DELETE FROM ApiUpdates WHERE apiKeyId NOT IN (SELECT DISTINCT id from ApiKey);");
        StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=deleteStaleData").append(" facetTable=ApiUpdates").append(" facetsDeleted=").append(i);
        logger.info(sb.toString());
    }

    @Transactional(readOnly=false)
    private void cleanupStaleFacetEntities(final Class cls, final String entityName) {
        final String sqlString = "SELECT * FROM " + entityName + " WHERE (apiKeyId NOT IN (SELECT DISTINCT id from ApiKey)) AND api!=0;";
        Query query = em.createNativeQuery(sqlString, cls);
        final String txIsolation = (String)em.createNativeQuery("SELECT @@tx_isolation").getSingleResult();
        System.out.println("jpaTxManager isolation: " + txIsolation);
        final List<? extends AbstractFacet> facetsToDelete = query.getResultList();
        final int i = facetsToDelete.size();
        if (i > 0) {
            for (AbstractFacet facet : facetsToDelete) {
                em.remove(facet);
            }
            StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=deleteStaleData").append(" facetTable=").append(entityName).append(" facetsDeleted=").append(i);
            logger.info(sb.toString());
        }
    }

    private void cleanupStaleFacetsInBulk(final String entityName) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                final String txIsolation = jdbcTemplate.queryForObject("SELECT @@tx_isolation", String.class);
                System.out.println("txManager isolation: " + txIsolation);
                final int i = jdbcTemplate.update("DELETE FROM " + entityName + " WHERE (apiKeyId NOT IN (SELECT DISTINCT id from ApiKey)) AND api!=0;");
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=deleteStaleData").append(" facetTable=").append(entityName).append(" facetsDeleted=").append(i);
                logger.info(sb.toString());
            }
        });
    }
}
