package org.openmrs.module.orderexpansion.api.validator;

import org.openmrs.Order;
import org.openmrs.annotation.Handler;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.openmrs.module.orderexpansion.api.model.QuantifiedOrder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Checks a single dispense row against its order. Rules that need other rows or the global property
 * (quantity reservation, declines, dispensable order types) live in the service.
 */
@Handler(supports = { CommodityDispense.class }, order = 50)
public class CommodityDispenseValidator implements Validator {
	
	static final int BILL_REFERENCE_MAX_LENGTH = 38;
	
	@Override
	public boolean supports(Class<?> clazz) {
		return CommodityDispense.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		CommodityDispense dispense = (CommodityDispense) target;
		if (dispense == null) {
			errors.reject("error.general");
			return;
		}
		
		Order order = dispense.getOrder();
		if (order == null) {
			errors.rejectValue("order", "error.null", "Order is required");
		} else {
			if (!(order instanceof QuantifiedOrder)) {
				errors.rejectValue("order", "orderexpansion.dispense.order.notQuantified",
				    "Only commodity orders with a quantity can be dispensed");
			}
			if (order.getVoided()) {
				errors.rejectValue("order", "orderexpansion.dispense.order.voided", "Cannot dispense a voided order");
			}
			if (order.getAction() == Order.Action.DISCONTINUE) {
				errors.rejectValue("order", "orderexpansion.dispense.order.discontinue",
				    "Cannot dispense a discontinuation order");
			}
			if (dispense.getPatient() != null && order.getPatient() != null
			        && !dispense.getPatient().equals(order.getPatient())) {
				errors.rejectValue("patient", "orderexpansion.dispense.patient.mismatch",
				    "Patient must be the order's patient");
			}
		}
		
		if (dispense.getPatient() == null) {
			errors.rejectValue("patient", "error.null", "Patient is required");
		}
		if (dispense.getStatus() == null) {
			errors.rejectValue("status", "error.null", "Status is required");
		}
		if (dispense.getDispenser() == null) {
			errors.rejectValue("dispenser", "error.null", "Dispenser is required");
		}
		if (dispense.getLocation() == null) {
			errors.rejectValue("location", "error.null", "Location is required");
		}
		
		if (dispense.getStatus() == Status.PENDING || dispense.getStatus() == Status.COMPLETED) {
			if (dispense.getQuantity() == null || dispense.getQuantity() <= 0) {
				errors.rejectValue("quantity", "orderexpansion.dispense.quantity.positive",
				    "Quantity must be greater than zero");
			}
			if (dispense.getQuantityUnits() == null) {
				errors.rejectValue("quantityUnits", "error.null", "Quantity units are required");
			} else if (order instanceof QuantifiedOrder
			        && !dispense.getQuantityUnits().equals(((QuantifiedOrder) order).getQuantityUnits())) {
				errors.rejectValue("quantityUnits", "orderexpansion.dispense.units.mismatch",
				    "Quantity units must match the order's units");
			}
		}
		if (dispense.getStatus() == Status.DECLINED && dispense.getStatusReason() == null) {
			errors.rejectValue("statusReason", "orderexpansion.dispense.statusReason.required",
			    "A reason is required to decline a dispense");
		}
		
		if (dispense.getBillReference() != null && dispense.getBillReference().length() > BILL_REFERENCE_MAX_LENGTH) {
			errors.rejectValue("billReference", "error.exceededMaxLengthOfField",
			    "Bill reference must be at most " + BILL_REFERENCE_MAX_LENGTH + " characters");
		}
	}
}
