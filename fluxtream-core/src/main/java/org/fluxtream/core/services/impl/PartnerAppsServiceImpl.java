package org.fluxtream.core.services.impl;

import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.services.PartnerAppsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * User: candide
 * Date: 16/04/14
 * Time: 10:04
 */
@Service
@Transactional(readOnly=true)
public class PartnerAppsServiceImpl implements PartnerAppsService {

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional(readOnly=false)
    public void createApplication(final long guestId, final String name, final String description, final String website) {
        Application app = new Application(guestId, name, description, website);
        em.persist(app);
    }

    @Override
    @Transactional(readOnly=false)
    public void deleteApplication(final long guestId, final String uid) {
        final Application app = getApplication(guestId, uid);
        if (app!=null)
            em.remove(app);
    }

    @Override
    public List<Application> getApplications(final long guestId) {
        final TypedQuery<Application> query = em.createQuery("SELECT app FROM Application app WHERE app.guestId=?", Application.class);
        query.setParameter(1, guestId);
        return query.getResultList();
    }

    @Override
    public Application getApplication(long guestId, String uid) {
        final TypedQuery<Application> query = em.createQuery("SELECT app FROM Application app WHERE app.uid=?", Application.class);
        query.setParameter(1, uid);
        if (query.getResultList().size()>0) {
            final Application app = query.getResultList().get(0);
            if (app.guestId!=guestId)
                throw new RuntimeException("Could not delete app: guestIds don't match");
            return app;
        }
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public void updateApplication(long guestId, String uid, String name, String description, final String website) {
        final Application app = getApplication(guestId, uid);
        if (app!=null) {
            app.name = name;
            app.description = description;
            app.website = website;
            em.persist(app);
        }
    }

}
