package org.openmrs.module.orderexpansion.api.rules;

import java.util.Collection;
import java.util.Objects;

import org.openmrs.Order;
import org.openmrs.api.ValidationException;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

/**
 * Rules that span several dispense rows or a row's saved state. Pure functions, so they are unit
 * tested without an OpenMRS context; the service supplies the rows and the persisted state.
 */
public final class DispenseRules {
	
	private static final double EPSILON = 1e-9;
	
	private DispenseRules() {
	}
	
	/** The values of a dispense row as stored in the database, before the current change. */
	public static class PersistedState {
		
		private final Integer orderId;
		
		private final Status status;
		
		private final Double quantity;
		
		private final String billReference;
		
		public PersistedState(Integer orderId, Status status, Double quantity, String billReference) {
			this.orderId = orderId;
			this.status = status;
			this.quantity = quantity;
			this.billReference = billReference;
		}
		
		public Integer getOrderId() {
			return orderId;
		}
		
		public Status getStatus() {
			return status;
		}
		
		public Double getQuantity() {
			return quantity;
		}
		
		public String getBillReference() {
			return billReference;
		}
	}
	
	/** The fulfiller status (and comment) an order should carry, given its dispense rows. */
	public static class FulfillerOutcome {
		
		private final Order.FulfillerStatus status;
		
		private final String comment;
		
		public FulfillerOutcome(Order.FulfillerStatus status, String comment) {
			this.status = status;
			this.comment = comment;
		}
		
		public Order.FulfillerStatus getStatus() {
			return status;
		}
		
		public String getComment() {
			return comment;
		}
	}
	
	/**
	 * Checks a change to an existing row. A new row ({@code before == null}) may start in any status. A
	 * PENDING row may change quantity until it is billed, and may move to COMPLETED or DECLINED. A
	 * COMPLETED or DECLINED row is final: only its comment and an unset bill reference may change. The
	 * order a row belongs to and a bill reference, once set, never change.
	 */
	public static void checkChange(PersistedState before, CommodityDispense after) {
		if (before == null) {
			return;
		}
		if (!Objects.equals(before.getOrderId(), after.getOrder().getOrderId())) {
			throw reject(after, "A dispense cannot be moved to a different order");
		}
		if (before.getBillReference() != null && !before.getBillReference().equals(after.getBillReference())) {
			throw reject(after, "The bill reference of a dispense cannot be changed once set");
		}
		boolean quantityChanged = !sameQuantity(before.getQuantity(), after.getQuantity());
		if (before.getStatus() == Status.PENDING) {
			if (quantityChanged && before.getBillReference() != null) {
				throw reject(after, "The quantity of a billed dispense cannot be changed; void it and dispense again");
			}
			return;
		}
		if (after.getStatus() != before.getStatus()) {
			throw reject(after, "A " + before.getStatus() + " dispense cannot change status; void it instead");
		}
		if (quantityChanged) {
			throw reject(after, "The quantity of a " + before.getStatus() + " dispense cannot be changed");
		}
	}
	
	/**
	 * Checks a row against the order's other active (non-voided) rows: a DECLINED row closes the order,
	 * a decline cannot be recorded while a dispense awaits payment, and PENDING plus COMPLETED
	 * quantities may not exceed the ordered quantity.
	 *
	 * @param others the order's other active rows, excluding {@code dispense}
	 */
	public static void checkAgainstOtherDispenses(double orderedQuantity, Collection<CommodityDispense> others,
	        CommodityDispense dispense) {
		if (dispense.isVoided()) {
			return;
		}
		boolean otherDeclined = false;
		boolean otherPending = false;
		double reserved = 0;
		for (CommodityDispense other : others) {
			if (other.getStatus() == Status.DECLINED) {
				otherDeclined = true;
			} else {
				reserved += quantityOf(other);
				otherPending |= other.getStatus() == Status.PENDING;
			}
		}
		if (otherDeclined) {
			throw reject(dispense, "This order has a declined dispense, which closes it to further dispensing");
		}
		if (dispense.getStatus() == Status.DECLINED) {
			if (otherPending) {
				throw reject(dispense, "Complete or void the dispense awaiting payment before declining this order");
			}
			return;
		}
		if (reserved + quantityOf(dispense) > orderedQuantity + EPSILON) {
			throw reject(dispense, "Dispensing " + format(quantityOf(dispense)) + " would exceed the ordered quantity of "
			        + format(orderedQuantity) + " (" + format(reserved) + " already dispensed or awaiting payment)");
		}
	}
	
	/**
	 * Derives the order's fulfiller status from its active rows. A decline closes the order: DECLINED
	 * if nothing was handed over, otherwise COMPLETED with a note that the remainder was declined.
	 * Otherwise the order is COMPLETED once the full quantity is handed over, and IN_PROGRESS before.
	 */
	public static FulfillerOutcome fulfillerOutcome(double orderedQuantity, Collection<CommodityDispense> activeRows) {
		double completed = 0;
		CommodityDispense decline = null;
		for (CommodityDispense row : activeRows) {
			if (row.getStatus() == Status.COMPLETED) {
				completed += quantityOf(row);
			} else if (row.getStatus() == Status.DECLINED) {
				decline = row;
			}
		}
		if (decline != null) {
			String reason = declineReason(decline);
			if (completed > EPSILON) {
				return new FulfillerOutcome(Order.FulfillerStatus.COMPLETED, "Partially dispensed (" + format(completed)
				        + " of " + format(orderedQuantity) + "). Remainder declined: " + reason);
			}
			return new FulfillerOutcome(Order.FulfillerStatus.DECLINED, reason);
		}
		if (orderedQuantity > EPSILON && completed + EPSILON >= orderedQuantity) {
			return new FulfillerOutcome(Order.FulfillerStatus.COMPLETED, null);
		}
		return new FulfillerOutcome(Order.FulfillerStatus.IN_PROGRESS, null);
	}
	
	private static String declineReason(CommodityDispense decline) {
		if (decline.getStatusReason() != null) {
			String name = decline.getStatusReason().getDisplayString();
			if (name != null && !name.isEmpty()) {
				return decline.getComment() == null ? name : name + ". " + decline.getComment();
			}
		}
		return decline.getComment() == null ? "Declined" : decline.getComment();
	}
	
	/**
	 * A rule violation as a {@link ValidationException} carrying the message as a global error, so the
	 * REST layer answers 400 with the message instead of a 500.
	 */
	public static ValidationException reject(CommodityDispense dispense, String message) {
		Errors errors = new BindException(dispense, "commodityDispense");
		errors.reject("orderexpansion.dispense.rule", message);
		return new ValidationException(message, errors);
	}
	
	private static double quantityOf(CommodityDispense dispense) {
		return dispense.getQuantity() == null ? 0 : dispense.getQuantity();
	}
	
	private static boolean sameQuantity(Double a, Double b) {
		if (a == null || b == null) {
			return a == null && b == null;
		}
		return Math.abs(a - b) < EPSILON;
	}
	
	private static String format(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}
}
