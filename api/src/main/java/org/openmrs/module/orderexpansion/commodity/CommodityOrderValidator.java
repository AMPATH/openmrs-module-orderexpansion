package org.openmrs.module.orderexpansion.commodity;

import org.openmrs.ConceptClass;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.annotation.Handler;
import org.openmrs.api.APIException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Keeps commodities apart. Runs alongside core's OrderValidator for every {@link CommodityOrder}.
 * <ul>
 * <li>The order type's Java class must be CommodityOrder, so a commodity order cannot be filed
 * under a medical supply (or any other) order type. Core already stops other order classes from
 * using a CommodityOrder order type.</li>
 * <li>The concept's class must be one of the order type's concept classes, so for example a
 * nutrition item cannot be ordered as a blood product.</li>
 * <li>New and revised orders need a quantity above zero and quantity units.</li>
 * </ul>
 */
@Handler(supports = { CommodityOrder.class }, order = 50)
public class CommodityOrderValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return CommodityOrder.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		CommodityOrder order = (CommodityOrder) target;
		if (order == null) {
			errors.reject("error.general");
			return;
		}
		
		OrderType orderType = order.getOrderType();
		if (orderType != null) {
			if (!isCommodityOrderType(orderType)) {
				errors.rejectValue("orderType", "orderexpansion.commodityOrder.orderType.class",
				    "The order type " + orderType.getName() + " is not a commodity order type (Java class "
				            + CommodityOrder.class.getName() + ")");
			}
			if (order.getConcept() != null && !orderType.getConceptClasses().isEmpty()) {
				ConceptClass conceptClass = order.getConcept().getConceptClass();
				if (conceptClass == null || !orderType.getConceptClasses().contains(conceptClass)) {
					errors.rejectValue("concept", "orderexpansion.commodityOrder.concept.class",
					    "The concept's class is not one of the concept classes of order type " + orderType.getName());
				}
			}
		}
		
		if (order.getAction() != Order.Action.DISCONTINUE) {
			if (order.getQuantity() == null || order.getQuantity() <= 0) {
				errors.rejectValue("quantity", "orderexpansion.commodityOrder.quantity.positive",
				    "Quantity must be greater than zero");
			}
			if (order.getQuantityUnits() == null) {
				errors.rejectValue("quantityUnits", "error.null", "Quantity units are required");
			}
		}
	}
	
	private static boolean isCommodityOrderType(OrderType orderType) {
		try {
			return CommodityOrder.class.isAssignableFrom(orderType.getJavaClass());
		}
		catch (APIException e) {
			// The order type names a class that cannot be loaded
			return false;
		}
	}
}
