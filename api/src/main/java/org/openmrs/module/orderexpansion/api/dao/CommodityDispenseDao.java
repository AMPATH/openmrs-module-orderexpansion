package org.openmrs.module.orderexpansion.api.dao;

import java.util.Date;
import java.util.List;

import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules.PersistedState;

public interface CommodityDispenseDao {
	
	CommodityDispense getByUuid(String uuid);
	
	List<CommodityDispense> getDispenses(Order order, Patient patient, List<OrderType> orderTypes, Location location,
	        Date fromDate, Date toDate, boolean includeVoided);
	
	/** The row as stored in the database, ignoring unsaved changes in the current session. */
	PersistedState getPersistedState(Integer commodityDispenseId);
	
	CommodityDispense save(CommodityDispense dispense);
}
