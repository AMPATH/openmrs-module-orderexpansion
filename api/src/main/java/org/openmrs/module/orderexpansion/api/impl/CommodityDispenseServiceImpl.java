package org.openmrs.module.orderexpansion.api.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.orderexpansion.OrderExpansionConstants;
import org.openmrs.module.orderexpansion.api.CommodityDispenseService;
import org.openmrs.module.orderexpansion.api.dao.CommodityDispenseDao;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.openmrs.module.orderexpansion.api.model.QuantifiedOrder;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules.FulfillerOutcome;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules.PersistedState;
import org.openmrs.module.orderexpansion.api.rules.OrderTypeRules;
import org.openmrs.validator.ValidateUtil;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CommodityDispenseServiceImpl extends BaseOpenmrsService implements CommodityDispenseService {
	
	private CommodityDispenseDao dao;
	
	public void setDao(CommodityDispenseDao dao) {
		this.dao = dao;
	}
	
	@Override
	@Transactional(readOnly = true)
	public CommodityDispense getCommodityDispenseByUuid(String uuid) {
		return dao.getByUuid(uuid);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<CommodityDispense> getCommodityDispenses(Order order, Patient patient, List<OrderType> orderTypes,
	        Location location, Date fromDate, Date toDate, boolean includeVoided) {
		return dao.getDispenses(order, patient, orderTypes, location, fromDate, toDate, includeVoided);
	}
	
	@Override
	public CommodityDispense saveCommodityDispense(CommodityDispense dispense) throws APIException {
		Order order = dispense.getOrder();
		if (dispense.getPatient() == null && order != null) {
			dispense.setPatient(order.getPatient());
		}
		ValidateUtil.validate(dispense);
		
		OrderTypeRules rules = OrderTypeRules
		        .parse(Context.getAdministrationService().getGlobalProperty(OrderExpansionConstants.GP_ORDER_TYPE_RULES));
		if (!rules.isDispensable(order.getOrderType())) {
			throw DispenseRules.reject(dispense, "Orders of type " + order.getOrderType().getName()
			        + " are not dispensable; enable them in " + OrderExpansionConstants.GP_ORDER_TYPE_RULES);
		}
		
		PersistedState before = dao.getPersistedState(dispense.getId());
		DispenseRules.checkChange(before, dispense);
		
		// Final rows (COMPLETED, DECLINED) only take a comment or a first bill reference, which never
		// affects quantities, so the order-level checks apply to new rows and rows leaving PENDING.
		boolean changesDispensing = before == null || before.getStatus() == Status.PENDING;
		if (changesDispensing && dispense.getStatus() != Status.DECLINED && !order.isActive()) {
			throw DispenseRules.reject(dispense, "Cannot dispense against an order that is no longer active");
		}
		
		double orderedQuantity = orderedQuantity(dispense);
		if (changesDispensing) {
			DispenseRules.checkAgainstOtherDispenses(orderedQuantity, otherActiveRows(dispense), dispense);
		}
		
		if (dispense.getStatus() == Status.COMPLETED && dispense.getDateDispensed() == null) {
			dispense.setDateDispensed(new Date());
		}
		CommodityDispense saved = dao.save(dispense);
		syncFulfillerStatus(order, orderedQuantity);
		return saved;
	}
	
	@Override
	public CommodityDispense voidCommodityDispense(CommodityDispense dispense, String reason) throws APIException {
		if (StringUtils.isBlank(reason)) {
			throw DispenseRules.reject(dispense, "A reason is required to void a dispense");
		}
		// Core's RequiredDataAdvice usually marks the row voided before this method runs, so the void
		// fields may already be set; the order's status still has to be recomputed either way.
		if (!dispense.getVoided()) {
			dispense.setVoided(true);
			dispense.setVoidReason(reason);
			dispense.setVoidedBy(Context.getAuthenticatedUser());
			dispense.setDateVoided(new Date());
		}
		CommodityDispense saved = dao.save(dispense);
		syncFulfillerStatus(dispense.getOrder(), orderedQuantity(dispense));
		return saved;
	}
	
	private List<CommodityDispense> otherActiveRows(CommodityDispense dispense) {
		List<CommodityDispense> others = new ArrayList<CommodityDispense>();
		for (CommodityDispense row : dao.getDispenses(dispense.getOrder(), null, null, null, null, null, false)) {
			if (row != dispense && (dispense.getId() == null || !dispense.getId().equals(row.getId()))) {
				others.add(row);
			}
		}
		return others;
	}
	
	private void syncFulfillerStatus(Order order, double orderedQuantity) {
		List<CommodityDispense> rows = dao.getDispenses(order, null, null, null, null, null, false);
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(orderedQuantity, rows);
		if (outcome.getStatus() != order.getFulfillerStatus()
		        || !Objects.equals(outcome.getComment(), order.getFulfillerComment())) {
			Context.getOrderService().updateOrderFulfillerStatus(order, outcome.getStatus(), outcome.getComment());
		}
	}
	
	private static double orderedQuantity(CommodityDispense dispense) {
		Order order = dispense.getOrder();
		Double quantity = order instanceof QuantifiedOrder ? ((QuantifiedOrder) order).getQuantity() : null;
		if (quantity == null) {
			throw DispenseRules.reject(dispense, "The order has no quantity to dispense against");
		}
		return quantity;
	}
}
