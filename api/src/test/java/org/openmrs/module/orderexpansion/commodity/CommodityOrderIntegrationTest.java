package org.openmrs.module.orderexpansion.commodity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.GlobalProperty;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.APIException;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.orderexpansion.OrderExpansionConstants;
import org.openmrs.module.orderexpansion.api.CommodityDispenseService;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.MedicalSupplyOrder;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * CommodityOrder through core's OrderService on the OpenMRS standard test dataset: mapping, revise
 * and discontinue, the separation rules, and dispensing through commodity_dispense.
 */
public class CommodityOrderIntegrationTest extends BaseModuleContextSensitiveTest {
	
	private OrderService orderService;
	
	private OrderType nutritionType;
	
	private OrderType medicalSupplyType;
	
	private Concept rutf;
	
	private Concept sachets;
	
	@Before
	public void setUp() {
		orderService = Context.getOrderService();
		
		ConceptClass nutritionClass = new ConceptClass();
		nutritionClass.setName("Nutrition order");
		nutritionClass.setDescription("For nutrition orders");
		Context.getConceptService().saveConceptClass(nutritionClass);
		
		rutf = Context.getConceptService().getConcept(5497);
		rutf.setConceptClass(nutritionClass);
		Context.getConceptService().saveConcept(rutf);
		sachets = Context.getConceptService().getConcept(18);
		
		nutritionType = new OrderType("Nutrition Order", "Nutrition order type", CommodityOrder.class.getName());
		nutritionType.addConceptClass(nutritionClass);
		orderService.saveOrderType(nutritionType);
		
		// Like AMPATH's Medical Supply order type: Java class org.openmrs.Order
		medicalSupplyType = new OrderType("Medical Supply", "Medical supply", "org.openmrs.Order");
		orderService.saveOrderType(medicalSupplyType);
	}
	
	private <T extends Order> T fill(T order, OrderType type, Concept concept) {
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setEncounter(Context.getEncounterService().getEncounter(3));
		order.setOrderer(Context.getProviderService().getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setConcept(concept);
		order.setOrderType(type);
		return order;
	}
	
	private CommodityOrder newNutritionOrder(double quantity) {
		CommodityOrder order = fill(new CommodityOrder(), nutritionType, rutf);
		order.setQuantity(quantity);
		order.setQuantityUnits(sachets);
		return order;
	}
	
	private void assertRejected(Order order, String messagePart) {
		try {
			orderService.saveOrder(order, null);
			fail("Expected the order to be rejected with: " + messagePart);
		}
		catch (APIException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(messagePart));
		}
	}
	
	@Test
	public void shouldSaveAndLoadACommodityOrderWithItsQuantity() {
		Order saved = orderService.saveOrder(newNutritionOrder(10), null);
		Context.flushSession();
		Context.clearSession();
		
		Order loaded = orderService.getOrderByUuid(saved.getUuid());
		assertTrue(loaded instanceof CommodityOrder);
		assertEquals(Double.valueOf(10), ((CommodityOrder) loaded).getQuantity());
		assertEquals(sachets.getUuid(), ((CommodityOrder) loaded).getQuantityUnits().getUuid());
		assertEquals(nutritionType.getUuid(), loaded.getOrderType().getUuid());
	}
	
	@Test
	public void shouldReviseKeepingTheCommodityClass() {
		Order original = orderService.saveOrder(newNutritionOrder(10), null);
		Context.flushSession();
		
		CommodityOrder revision = ((CommodityOrder) original).cloneForRevision();
		revision.setQuantity(6.0);
		revision.setEncounter(Context.getEncounterService().getEncounter(3));
		revision.setOrderer(Context.getProviderService().getProvider(1));
		Order savedRevision = orderService.saveOrder(revision, null);
		
		assertTrue(savedRevision instanceof CommodityOrder);
		assertEquals(Double.valueOf(6), ((CommodityOrder) savedRevision).getQuantity());
		assertNotNull(orderService.getOrderByUuid(original.getUuid()).getDateStopped());
	}
	
	@Test
	public void shouldDiscontinueThroughCore() throws Exception {
		Order original = orderService.saveOrder(newNutritionOrder(10), null);
		Context.flushSession();
		
		Order discontinuation = orderService.discontinueOrder(original, "No longer needed", new Date(),
		    Context.getProviderService().getProvider(1), Context.getEncounterService().getEncounter(3));
		
		assertTrue(discontinuation instanceof CommodityOrder);
		assertEquals(Order.Action.DISCONTINUE, discontinuation.getAction());
	}
	
	@Test
	public void shouldRejectACommodityOrderUnderANonCommodityOrderType() {
		CommodityOrder order = newNutritionOrder(10);
		order.setOrderType(medicalSupplyType);
		assertRejected(order, "orderexpansion.commodityOrder.orderType.class");
	}
	
	@Test
	public void shouldRejectAConceptOutsideTheOrderTypesConceptClasses() {
		CommodityOrder order = newNutritionOrder(10);
		order.setConcept(sachets);
		assertRejected(order, "orderexpansion.commodityOrder.concept.class");
	}
	
	@Test
	public void shouldRejectAMissingQuantity() {
		CommodityOrder order = newNutritionOrder(10);
		order.setQuantity(null);
		assertRejected(order, "orderexpansion.commodityOrder.quantity.positive");
	}
	
	@Test(expected = APIException.class)
	public void shouldNotAllowAMedicalSupplyOrderUnderTheNutritionOrderType() {
		MedicalSupplyOrder order = fill(new MedicalSupplyOrder(), nutritionType, rutf);
		order.setQuantity(1.0);
		order.setQuantityUnits(sachets);
		orderService.saveOrder(order, null);
	}
	
	@Test
	public void shouldBeDispensableThroughCommodityDispense() {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(OrderExpansionConstants.GP_ORDER_TYPE_RULES,
		        "{\"" + nutritionType.getUuid() + "\": {\"dispensable\": true}}"));
		Order order = orderService.saveOrder(newNutritionOrder(10), null);
		
		CommodityDispense dispense = new CommodityDispense();
		dispense.setOrder(order);
		dispense.setStatus(CommodityDispense.Status.COMPLETED);
		dispense.setQuantity(10.0);
		dispense.setQuantityUnits(sachets);
		dispense.setDispenser(Context.getProviderService().getProvider(1));
		dispense.setLocation(Context.getLocationService().getLocation(1));
		Context.getService(CommodityDispenseService.class).saveCommodityDispense(dispense);
		
		Context.flushSession();
		Context.clearSession();
		assertEquals(Order.FulfillerStatus.COMPLETED, orderService.getOrderByUuid(order.getUuid()).getFulfillerStatus());
	}
}
