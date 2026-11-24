package org.openmrs.module.orderexpansion;

public final class OrderExpansionConstants {
	
	/**
	 * Global property holding per-order-type rules as JSON, keyed by order type UUID. Example:
	 * {@code {"<order-type-uuid>": {"dispensable": true}}}. Set through Initializer.
	 */
	public static final String GP_ORDER_TYPE_RULES = "orderexpansion.orderTypeRules";
	
	private OrderExpansionConstants() {
	}
}
