package org.openmrs.module.orderexpansion.api.model;

import org.openmrs.Concept;

/**
 * An order for a quantity of a commodity. Dispensing reads the ordered amount through this
 * interface, so it works the same for every commodity order class.
 */
public interface QuantifiedOrder {
	
	Double getQuantity();
	
	Concept getQuantityUnits();
}
