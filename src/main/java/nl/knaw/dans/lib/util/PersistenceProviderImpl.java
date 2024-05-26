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
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

public class PersistenceProviderImpl<E> extends AbstractDAO<E> implements PersistenceProvider<E> {
    private final Class<E> entityClass;

    public PersistenceProviderImpl(SessionFactory sessionFactory, Class<E> entityClass) {
        super(sessionFactory);
        this.entityClass = entityClass;
    }

    @Override
    public E persist(E entity) {
        return super.persist(entity);
    }

    @Override
    public void update(E entity) {
        currentSession().update(entity);
    }

    @Override
    public void delete(E entity) {
        currentSession().delete(entity);
    }

    @Override
    public E get(Long id) {
        return currentSession().get(entityClass, id);
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
