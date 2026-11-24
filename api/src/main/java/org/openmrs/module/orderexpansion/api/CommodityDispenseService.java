package org.openmrs.module.orderexpansion.api;

import java.util.Date;
import java.util.List;

import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.util.PrivilegeConstants;

/**
 * Records dispensing against commodity orders and keeps each order's fulfiller status in step.
 */
public interface CommodityDispenseService extends OpenmrsService {
	
	@Authorized(PrivilegeConstants.GET_ORDERS)
	CommodityDispense getCommodityDispenseByUuid(String uuid);
	
	/**
	 * @param order if set, only rows for this order
	 * @param patient if set, only rows for this patient
	 * @param orderTypes if not empty, only rows whose order has one of these types
	 * @param location if set, only rows dispensed at this location
	 * @param fromDate if set, only rows dispensed on or after this date (excludes PENDING rows)
	 * @param toDate if set, only rows dispensed on or before this date (excludes PENDING rows)
	 * @param includeVoided whether to include voided rows
	 */
	@Authorized(PrivilegeConstants.GET_ORDERS)
	List<CommodityDispense> getCommodityDispenses(Order order, Patient patient, List<OrderType> orderTypes,
	        Location location, Date fromDate, Date toDate, boolean includeVoided);
	
	/**
	 * Creates or updates a dispense, then updates the order's fulfiller status.
	 *
	 * @throws org.openmrs.api.ValidationException (an {@link APIException}, returned by REST as 400) if
	 *             the dispense is invalid, the order type is not dispensable, the order is not active,
	 *             the change is not an allowed status change, or the quantity would exceed the order
	 */
	@Authorized(PrivilegeConstants.EDIT_ORDERS)
	CommodityDispense saveCommodityDispense(CommodityDispense dispense) throws APIException;
	
	/**
	 * Voids a dispense, releasing its quantity, then recomputes the order's fulfiller status. A linked
	 * bill line is not touched; refunds are handled in billing.
	 */
	@Authorized(PrivilegeConstants.EDIT_ORDERS)
	CommodityDispense voidCommodityDispense(CommodityDispense dispense, String reason) throws APIException;
}
