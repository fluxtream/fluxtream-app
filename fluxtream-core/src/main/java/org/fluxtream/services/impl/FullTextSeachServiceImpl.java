package org.fluxtream.services.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.services.FullTextSearchService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FullTextSeachServiceImpl implements FullTextSearchService {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Override
    public void reinitializeIndex() throws Exception {
        EntityManager em = entityManagerFactory.createEntityManager();
        FullTextEntityManager fullTextEntityManager = Search
                .getFullTextEntityManager(em);
        EntityTransaction tx = fullTextEntityManager.getTransaction();
        tx.begin();
        indexFacets(em, fullTextEntityManager,
                    "Facet_CalendarEventEntry",
                    "Facet_CallLog",
                    "Facet_FitbitActivity",
                    "Facet_FitbitLoggedActivity",
                    "Facet_FitbitSleep",
                    "Facet_FlickrPhoto",
                    "Facet_LastFmRecentTrack",
                    "Facet_SmsEntry",
                    "Facet_Tweet",
                    "Facet_TwitterDirectMessage",
                    "Facet_TwitterMention",
                    "Facet_WithingsBodyScaleMeasure",
                    "Facet_WithingsBPMMeasure",
                    "Facet_ZeoSleepStats");
        tx.commit();
        em.close();
    }

    private void indexFacets(EntityManager em,
                             FullTextEntityManager fullTextEntityManager,
                             String... facetEntityName) {
        for (String entityName : facetEntityName) {
            @SuppressWarnings("unchecked")
            List<AbstractFacet> facets = em.createQuery(
                    "select facet from " + entityName + " facet")
                    .getResultList();
            int count = 0;
            for (AbstractFacet facet : facets) {
                if (count++%10000==0)
                    fullTextEntityManager.flushToIndexes() ;
                fullTextEntityManager.index(facet);
            }
        }
    }

    @Override
    public List<AbstractFacet> searchFacetsIndex(long guestId, String terms)
            throws Exception {
        EntityManager em = entityManagerFactory.createEntityManager();
        FullTextEntityManager fullTextEntityManager = org.hibernate.search.jpa.Search
                .getFullTextEntityManager(em);

        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);
        org.apache.lucene.search.Query termsQuery = new QueryParser(
                Version.LUCENE_31, "fullTextDescription", analyzer)
                .parse(terms);
        org.apache.lucene.search.Query guestQuery = new QueryParser(
                Version.LUCENE_31, "guestId", analyzer)
                .parse(String.valueOf(guestId));

        BooleanQuery rootQuery = new BooleanQuery();
        rootQuery.add(new BooleanClause(guestQuery, Occur.MUST));
        rootQuery.add(new BooleanClause(termsQuery, Occur.MUST));

        // wrap Lucene query in a javax.persistence.Query
        Query persistenceQuery = fullTextEntityManager.createFullTextQuery(
                rootQuery, AbstractFacet.class);

        persistenceQuery.setMaxResults(100);

        // execute search
        @SuppressWarnings("unchecked")
        List<AbstractFacet> result = persistenceQuery.getResultList();
        em.close();
        return result;
    }
}
