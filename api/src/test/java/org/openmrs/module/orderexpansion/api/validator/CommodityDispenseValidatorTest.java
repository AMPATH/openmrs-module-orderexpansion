package org.openmrs.module.orderexpansion.api.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.orderexpansion.api.rules.DispenseTestData.BOXES;
import static org.openmrs.module.orderexpansion.api.rules.DispenseTestData.dispense;
import static org.openmrs.module.orderexpansion.api.rules.DispenseTestData.order;

import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.TestOrder;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.openmrs.module.orderexpansion.api.model.MedicalSupplyOrder;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

public class CommodityDispenseValidatorTest {
	
	private final CommodityDispenseValidator validator = new CommodityDispenseValidator();
	
	private Errors validate(CommodityDispense dispense) {
		Errors errors = new BindException(dispense, "dispense");
		validator.validate(dispense, errors);
		return errors;
	}
	
	@Test
	public void shouldAcceptAValidCompletedDispense() {
		Errors errors = validate(dispense(order(5), Status.COMPLETED, 2.0));
		assertFalse(errors.toString(), errors.hasErrors());
	}
	
	@Test
	public void shouldRejectAnOrderWithoutAQuantity() {
		TestOrder testOrder = new TestOrder();
		testOrder.setPatient(order(5).getPatient());
		testOrder.setAction(Order.Action.NEW);
		CommodityDispense dispense = dispense(order(5), Status.COMPLETED, 2.0);
		dispense.setOrder(testOrder);
		assertTrue(validate(dispense).hasFieldErrors("order"));
	}
	
	@Test
	public void shouldRejectADiscontinuationOrder() {
		MedicalSupplyOrder order = order(5);
		order.setAction(Order.Action.DISCONTINUE);
		assertTrue(validate(dispense(order, Status.COMPLETED, 2.0)).hasFieldErrors("order"));
	}
	
	@Test
	public void shouldRejectAPatientOtherThanTheOrders() {
		CommodityDispense dispense = dispense(order(5), Status.COMPLETED, 2.0);
		Patient other = new Patient(2);
		other.setUuid("other-patient-uuid");
		dispense.setPatient(other);
		assertTrue(validate(dispense).hasFieldErrors("patient"));
	}
	
	@Test
	public void shouldRequireAPositiveQuantityToDispense() {
		assertTrue(validate(dispense(order(5), Status.PENDING, 0.0)).hasFieldErrors("quantity"));
		assertTrue(validate(dispense(order(5), Status.COMPLETED, null)).hasFieldErrors("quantity"));
	}
	
	@Test
	public void shouldRequireTheOrdersUnits() {
		CommodityDispense dispense = dispense(order(5), Status.COMPLETED, 2.0);
		dispense.setQuantityUnits(BOXES);
		assertTrue(validate(dispense).hasFieldErrors("quantityUnits"));
	}
	
	@Test
	public void shouldRequireAReasonToDecline() {
		CommodityDispense decline = dispense(order(5), Status.DECLINED, null);
		assertTrue(validate(decline).hasFieldErrors("statusReason"));
		assertFalse(validate(decline).hasFieldErrors("quantity"));
	}
	
	@Test
	public void shouldRequireDispenserAndLocation() {
		CommodityDispense dispense = dispense(order(5), Status.COMPLETED, 2.0);
		dispense.setDispenser(null);
		dispense.setLocation(null);
		Errors errors = validate(dispense);
		assertTrue(errors.hasFieldErrors("dispenser"));
		assertTrue(errors.hasFieldErrors("location"));
	}
	
	@Test
	public void shouldRejectAnOverlongBillReference() {
		CommodityDispense dispense = dispense(order(5), Status.COMPLETED, 2.0);
		dispense.setBillReference("123456789012345678901234567890123456789");
		assertTrue(validate(dispense).hasFieldErrors("billReference"));
	}
}
