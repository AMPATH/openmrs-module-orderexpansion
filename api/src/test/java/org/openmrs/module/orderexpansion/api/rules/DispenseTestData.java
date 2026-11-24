package org.openmrs.module.orderexpansion.api.rules;

import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.openmrs.module.orderexpansion.api.model.MedicalSupplyOrder;

/** Builds orders and dispenses without an OpenMRS context. */
public final class DispenseTestData {
	
	public static final Concept PIECES = concept(10, "pieces-uuid");
	
	public static final Concept BOXES = concept(11, "boxes-uuid");
	
	private DispenseTestData() {
	}
	
	public static Concept concept(int id, String uuid) {
		Concept concept = new Concept(id);
		concept.setUuid(uuid);
		return concept;
	}
	
	public static MedicalSupplyOrder order(double quantity) {
		MedicalSupplyOrder order = new MedicalSupplyOrder();
		order.setOrderId(100);
		Patient patient = new Patient(1);
		patient.setUuid("patient-uuid");
		order.setPatient(patient);
		order.setConcept(concept(20, "gauze-uuid"));
		order.setCareSetting(new CareSetting());
		OrderType orderType = new OrderType();
		orderType.setUuid("medical-supply-type-uuid");
		order.setOrderType(orderType);
		order.setAction(Order.Action.NEW);
		order.setQuantity(quantity);
		order.setQuantityUnits(PIECES);
		return order;
	}
	
	public static CommodityDispense dispense(Order order, Status status, Double quantity) {
		CommodityDispense dispense = new CommodityDispense();
		dispense.setOrder(order);
		dispense.setPatient(order.getPatient());
		dispense.setStatus(status);
		dispense.setQuantity(quantity);
		dispense.setQuantityUnits(PIECES);
		dispense.setDispenser(new Provider(1));
		dispense.setLocation(new Location(1));
		return dispense;
	}
}
