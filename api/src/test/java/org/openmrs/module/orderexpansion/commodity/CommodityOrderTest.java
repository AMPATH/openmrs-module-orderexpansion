package org.openmrs.module.orderexpansion.commodity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;

public class CommodityOrderTest {
	
	private CommodityOrder order;
	
	private Concept sachets;
	
	@Before
	public void setUp() {
		sachets = new Concept(1);
		order = new CommodityOrder();
		order.setPatient(new Patient(1));
		order.setConcept(new Concept(2));
		order.setCareSetting(new CareSetting());
		order.setOrderType(new OrderType());
		order.setAction(Order.Action.NEW);
		order.setQuantity(10.0);
		order.setQuantityUnits(sachets);
	}
	
	@Test
	public void copy_shouldKeepQuantityAndUnits() {
		CommodityOrder copy = order.copy();
		assertEquals(Double.valueOf(10.0), copy.getQuantity());
		assertSame(sachets, copy.getQuantityUnits());
	}
	
	@Test
	public void cloneForRevision_shouldBeACommodityOrderKeepingQuantityAndUnits() {
		CommodityOrder revision = order.cloneForRevision();
		assertEquals(Order.Action.REVISE, revision.getAction());
		assertSame(order, revision.getPreviousOrder());
		assertEquals(Double.valueOf(10.0), revision.getQuantity());
		assertSame(sachets, revision.getQuantityUnits());
	}
	
	@Test
	public void cloneForDiscontinuing_shouldBeACommodityOrderWithTheOrdersIdentity() {
		CommodityOrder discontinuation = order.cloneForDiscontinuing();
		assertEquals(Order.Action.DISCONTINUE, discontinuation.getAction());
		assertSame(order, discontinuation.getPreviousOrder());
		assertSame(order.getPatient(), discontinuation.getPatient());
		assertSame(order.getConcept(), discontinuation.getConcept());
		assertSame(order.getOrderType(), discontinuation.getOrderType());
		assertSame(order.getCareSetting(), discontinuation.getCareSetting());
		assertNull(discontinuation.getQuantity());
	}
}
