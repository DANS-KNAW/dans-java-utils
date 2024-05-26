/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.util;

import io.dropwizard.hibernate.AbstractDAO;
import nl.knaw.dans.layerstore.ItemRecord;
import nl.knaw.dans.layerstore.PersistenceProvider;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

public class PersistenceProviderImpl extends AbstractDAO<ItemRecord> implements PersistenceProvider<ItemRecord> {

    public PersistenceProviderImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public ItemRecord persist(ItemRecord entity) {
        return super.persist(entity);
    }

    @Override
    public void update(ItemRecord entity) {
        currentSession().update(entity);
    }

    @Override
    public void delete(ItemRecord entity) {
        currentSession().delete(entity);
    }

    @Override
    public ItemRecord get(Long id) {
        return currentSession().get(ItemRecord.class, id);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return currentSession().getCriteriaBuilder();
    }

    @Override
    public <T> Query<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return currentSession().createQuery(criteriaQuery);
    }
}
