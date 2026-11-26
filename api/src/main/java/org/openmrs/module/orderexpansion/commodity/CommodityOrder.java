package org.openmrs.module.orderexpansion.commodity;

import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.module.orderexpansion.api.model.QuantifiedOrder;

/**
 * An order for a quantity of a commodity other than a medical supply (nutrition now, blood products
 * later). All commodities share this class and the commodity_order table; the commodity is the
 * order's order type, whose Java class must be this class (see {@link CommodityOrderValidator}).
 * Dispensing works through commodity_dispense like any other {@link QuantifiedOrder}.
 */
public class CommodityOrder extends Order implements QuantifiedOrder {
	
	private static final long serialVersionUID = 1L;
	
	private Double quantity;
	
	private Concept quantityUnits;
	
	public CommodityOrder() {
	}
	
	@Override
	public CommodityOrder copy() {
		CommodityOrder newOrder = new CommodityOrder();
		super.copyHelper(newOrder);
		copyCommodityFields(newOrder);
		return newOrder;
	}
	
	@Override
	public CommodityOrder cloneForRevision() {
		CommodityOrder newOrder = new CommodityOrder();
		super.cloneForRevisionHelper(newOrder);
		copyCommodityFields(newOrder);
		return newOrder;
	}
	
	/**
	 * Same fields as core's {@link Order#cloneForDiscontinuing()}, which would return a plain Order and
	 * fail core's check that a discontinuation has the class of the order it stops.
	 */
	@Override
	public CommodityOrder cloneForDiscontinuing() {
		CommodityOrder newOrder = new CommodityOrder();
		newOrder.setCareSetting(getCareSetting());
		newOrder.setConcept(getConcept());
		newOrder.setAction(Action.DISCONTINUE);
		newOrder.setPreviousOrder(this);
		newOrder.setPatient(getPatient());
		newOrder.setOrderType(getOrderType());
		return newOrder;
	}
	
	private void copyCommodityFields(CommodityOrder target) {
		target.setQuantity(getQuantity());
		target.setQuantityUnits(getQuantityUnits());
	}
	
	@Override
	public Double getQuantity() {
		return quantity;
	}
	
	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
	
	@Override
	public Concept getQuantityUnits() {
		return quantityUnits;
	}
	
	public void setQuantityUnits(Concept quantityUnits) {
		this.quantityUnits = quantityUnits;
	}
}
