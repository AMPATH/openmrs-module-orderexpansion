package org.openmrs.module.orderexpansion.api.dao.impl;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Location;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.module.orderexpansion.api.dao.CommodityDispenseDao;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules.PersistedState;

public class CommodityDispenseDaoImpl implements CommodityDispenseDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private Session session() {
		return sessionFactory.getCurrentSession();
	}
	
	@Override
	public CommodityDispense getByUuid(String uuid) {
		return (CommodityDispense) session().createCriteria(CommodityDispense.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<CommodityDispense> getDispenses(org.openmrs.Order order, Patient patient, List<OrderType> orderTypes,
	        Location location, Date fromDate, Date toDate, boolean includeVoided) {
		Criteria criteria = session().createCriteria(CommodityDispense.class, "d");
		if (order != null) {
			criteria.add(Restrictions.eq("d.order", order));
		}
		if (patient != null) {
			criteria.add(Restrictions.eq("d.patient", patient));
		}
		if (orderTypes != null && !orderTypes.isEmpty()) {
			criteria.createAlias("d.order", "o");
			criteria.add(Restrictions.in("o.orderType", orderTypes));
		}
		if (location != null) {
			criteria.add(Restrictions.eq("d.location", location));
		}
		if (fromDate != null) {
			criteria.add(Restrictions.ge("d.dateDispensed", fromDate));
		}
		if (toDate != null) {
			criteria.add(Restrictions.le("d.dateDispensed", toDate));
		}
		if (!includeVoided) {
			criteria.add(Restrictions.eq("d.voided", false));
		}
		criteria.addOrder(Order.asc("d.dateCreated")).addOrder(Order.asc("d.commodityDispenseId"));
		return criteria.list();
	}
	
	@Override
	public PersistedState getPersistedState(Integer commodityDispenseId) {
		if (commodityDispenseId == null) {
			return null;
		}
		// Without MANUAL flush mode, Hibernate would flush the pending change to the row first and the
		// query would return the new values instead of the stored ones.
		Session session = session();
		FlushMode previous = session.getHibernateFlushMode();
		session.setHibernateFlushMode(FlushMode.MANUAL);
		try {
			Object[] row = (Object[]) session
			        .createQuery("select d.order.orderId, d.status, d.quantity, d.billReference "
			                + "from CommodityDispense d where d.commodityDispenseId = :id")
			        .setParameter("id", commodityDispenseId).uniqueResult();
			if (row == null) {
				return null;
			}
			return new PersistedState((Integer) row[0], (CommodityDispense.Status) row[1], (Double) row[2], (String) row[3]);
		}
		finally {
			session.setHibernateFlushMode(previous);
		}
	}
	
	@Override
	public CommodityDispense save(CommodityDispense dispense) {
		session().saveOrUpdate(dispense);
		return dispense;
	}
}
