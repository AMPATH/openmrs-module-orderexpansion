package org.openmrs.module.orderexpansion.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.GlobalProperty;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.orderexpansion.OrderExpansionConstants;
import org.openmrs.module.orderexpansion.api.CommodityDispenseService;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.openmrs.module.orderexpansion.api.model.MedicalSupplyOrder;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Runs the dispense service against an in-memory database with the OpenMRS standard test dataset:
 * Hibernate mapping, persisted-state reads, quantity rules and the order's fulfiller status.
 */
public class CommodityDispenseServiceImplTest extends BaseModuleContextSensitiveTest {
	
	private CommodityDispenseService service;
	
	private OrderType medicalSupplyType;
	
	private Concept pieces;
	
	@Before
	public void setUp() {
		service = Context.getService(CommodityDispenseService.class);
		pieces = Context.getConceptService().getConcept(18);
		
		// Like AMPATH's Initializer config, the order type's Java class is org.openmrs.Order.
		medicalSupplyType = new OrderType("Medical Supply", "Medical supply", "org.openmrs.Order");
		Context.getOrderService().saveOrderType(medicalSupplyType);
		setRules("{\"" + medicalSupplyType.getUuid() + "\": {\"dispensable\": true}}");
	}
	
	private void setRules(String json) {
		Context.getAdministrationService()
		        .saveGlobalProperty(new GlobalProperty(OrderExpansionConstants.GP_ORDER_TYPE_RULES, json));
	}
	
	private MedicalSupplyOrder newOrder(double quantity) {
		MedicalSupplyOrder order = new MedicalSupplyOrder();
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setEncounter(Context.getEncounterService().getEncounter(3));
		order.setOrderer(Context.getProviderService().getProvider(1));
		order.setCareSetting(Context.getOrderService().getCareSetting(1));
		order.setConcept(Context.getConceptService().getConcept(5497));
		order.setOrderType(medicalSupplyType);
		order.setQuantity(quantity);
		order.setQuantityUnits(pieces);
		return (MedicalSupplyOrder) Context.getOrderService().saveOrder(order, null);
	}
	
	private CommodityDispense newDispense(Order order, Status status, Double quantity) {
		CommodityDispense dispense = new CommodityDispense();
		dispense.setOrder(order);
		dispense.setStatus(status);
		dispense.setQuantity(quantity);
		dispense.setQuantityUnits(pieces);
		dispense.setDispenser(Context.getProviderService().getProvider(1));
		dispense.setLocation(Context.getLocationService().getLocation(1));
		return dispense;
	}
	
	private Order.FulfillerStatus fulfillerStatusOf(Order order) {
		Context.flushSession();
		Context.clearSession();
		return Context.getOrderService().getOrderByUuid(order.getUuid()).getFulfillerStatus();
	}
	
	@Test
	public void shouldPersistADispenseAndDefaultThePatientFromTheOrder() {
		MedicalSupplyOrder order = newOrder(5);
		CommodityDispense saved = service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 2.0));
		Context.flushSession();
		Context.clearSession();
		
		CommodityDispense loaded = service.getCommodityDispenseByUuid(saved.getUuid());
		assertNotNull(loaded);
		assertEquals(order.getPatient().getUuid(), loaded.getPatient().getUuid());
		assertEquals(Double.valueOf(2.0), loaded.getQuantity());
		assertNotNull("COMPLETED dispenses get a dispensed date", loaded.getDateDispensed());
		assertNotNull(loaded.getCreator());
	}
	
	@Test
	public void shouldMoveTheOrderThroughInProgressToCompleted() {
		MedicalSupplyOrder order = newOrder(5);
		
		CommodityDispense pending = service.saveCommodityDispense(newDispense(order, Status.PENDING, 2.0));
		assertEquals(Order.FulfillerStatus.IN_PROGRESS, fulfillerStatusOf(order));
		
		pending = service.getCommodityDispenseByUuid(pending.getUuid());
		pending.setBillReference("4c2a1b3e-0000-4000-8000-000000000001");
		pending.setStatus(Status.COMPLETED);
		service.saveCommodityDispense(pending);
		assertEquals(Order.FulfillerStatus.IN_PROGRESS, fulfillerStatusOf(order));
		
		order = (MedicalSupplyOrder) Context.getOrderService().getOrderByUuid(order.getUuid());
		service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 3.0));
		assertEquals(Order.FulfillerStatus.COMPLETED, fulfillerStatusOf(order));
	}
	
	@Test
	public void shouldReadTheStoredRowNotTheUnsavedChangeWhenCheckingEdits() {
		MedicalSupplyOrder order = newOrder(5);
		CommodityDispense completed = service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 2.0));
		Context.flushSession();
		
		completed.setQuantity(1.0);
		try {
			service.saveCommodityDispense(completed);
			fail("Changing the quantity of a COMPLETED dispense must be rejected");
		}
		catch (APIException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("cannot be changed"));
		}
	}
	
	@Test
	public void shouldRejectDispensingMoreThanOrdered() {
		MedicalSupplyOrder order = newOrder(5);
		service.saveCommodityDispense(newDispense(order, Status.PENDING, 4.0));
		assertRejected(newDispense(order, Status.COMPLETED, 2.0), "would exceed the ordered quantity");
	}
	
	private void assertRejected(CommodityDispense dispense, String messagePart) {
		try {
			service.saveCommodityDispense(dispense);
			fail("Expected the dispense to be rejected with: " + messagePart);
		}
		catch (APIException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(messagePart));
		}
	}
	
	@Test
	public void voidingShouldReleaseTheQuantityAndReopenTheOrder() {
		MedicalSupplyOrder order = newOrder(5);
		CommodityDispense all = service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 5.0));
		assertEquals(Order.FulfillerStatus.COMPLETED, fulfillerStatusOf(order));
		
		all = service.getCommodityDispenseByUuid(all.getUuid());
		service.voidCommodityDispense(all, "Entered in error");
		assertEquals(Order.FulfillerStatus.IN_PROGRESS, fulfillerStatusOf(order));
		
		order = (MedicalSupplyOrder) Context.getOrderService().getOrderByUuid(order.getUuid());
		service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 5.0));
		assertEquals(Order.FulfillerStatus.COMPLETED, fulfillerStatusOf(order));
	}
	
	@Test
	public void decliningShouldDeclineTheOrderWithTheReason() {
		MedicalSupplyOrder order = newOrder(5);
		CommodityDispense decline = newDispense(order, Status.DECLINED, null);
		decline.setStatusReason(Context.getConceptService().getConcept(3));
		decline.setComment("Out of stock");
		service.saveCommodityDispense(decline);
		
		Context.flushSession();
		Context.clearSession();
		Order reloaded = Context.getOrderService().getOrderByUuid(order.getUuid());
		assertEquals(Order.FulfillerStatus.DECLINED, reloaded.getFulfillerStatus());
		assertTrue(reloaded.getFulfillerComment(), reloaded.getFulfillerComment().endsWith("Out of stock"));
	}
	
	@Test
	public void shouldRejectOrderTypesNotMarkedDispensable() {
		MedicalSupplyOrder order = newOrder(5);
		setRules("{}");
		assertRejected(newDispense(order, Status.COMPLETED, 1.0), "are not dispensable");
	}
	
	@Test
	public void shouldSearchByOrderTypeAndLocationExcludingVoided() {
		MedicalSupplyOrder order = newOrder(5);
		service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 1.0));
		CommodityDispense toVoid = service.saveCommodityDispense(newDispense(order, Status.COMPLETED, 1.0));
		service.voidCommodityDispense(toVoid, "Entered in error");
		Context.flushSession();
		
		List<CommodityDispense> active = service.getCommodityDispenses(null, null,
		    Collections.singletonList(medicalSupplyType), Context.getLocationService().getLocation(1), null, null, false);
		assertEquals(1, active.size());
		
		List<CommodityDispense> all = service.getCommodityDispenses(order, null, null, null, null, null, true);
		assertEquals(2, all.size());
	}
	
}
