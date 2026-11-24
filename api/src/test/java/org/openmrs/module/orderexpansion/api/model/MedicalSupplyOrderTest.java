package org.openmrs.module.orderexpansion.api.model;

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

public class MedicalSupplyOrderTest {
	
	private MedicalSupplyOrder order;
	
	private Concept units;
	
	@Before
	public void setUp() {
		units = new Concept(1);
		order = new MedicalSupplyOrder();
		order.setPatient(new Patient(1));
		order.setConcept(new Concept(2));
		order.setCareSetting(new CareSetting());
		order.setOrderType(new OrderType());
		order.setAction(Order.Action.NEW);
		order.setQuantity(3.0);
		order.setQuantityUnits(units);
		order.setBrandName("Brand A");
		order.setMedicalSuppliesInventoryId(42);
	}
	
	@Test
	public void copy_shouldKeepMedicalSupplyFields() {
		MedicalSupplyOrder copy = order.copy();
		
		assertEquals(Double.valueOf(3.0), copy.getQuantity());
		assertSame(units, copy.getQuantityUnits());
		assertEquals("Brand A", copy.getBrandName());
		assertEquals(Integer.valueOf(42), copy.getMedicalSuppliesInventoryId());
	}
	
	@Test
	public void cloneForRevision_shouldKeepMedicalSupplyFields() {
		MedicalSupplyOrder revision = order.cloneForRevision();
		
		assertEquals(Order.Action.REVISE, revision.getAction());
		assertSame(order, revision.getPreviousOrder());
		assertEquals(Double.valueOf(3.0), revision.getQuantity());
		assertSame(units, revision.getQuantityUnits());
		assertEquals("Brand A", revision.getBrandName());
		assertEquals(Integer.valueOf(42), revision.getMedicalSuppliesInventoryId());
	}
	
	@Test
	public void cloneForDiscontinuing_shouldReturnMedicalSupplyOrderWithoutQuantity() {
		MedicalSupplyOrder discontinuation = order.cloneForDiscontinuing();
		
		assertEquals(Order.Action.DISCONTINUE, discontinuation.getAction());
		assertSame(order, discontinuation.getPreviousOrder());
		assertNull(discontinuation.getQuantity());
	}
}
