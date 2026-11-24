package org.openmrs.module.orderexpansion.api.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.orderexpansion.api.rules.DispenseTestData.dispense;
import static org.openmrs.module.orderexpansion.api.rules.DispenseTestData.order;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.api.APIException;
import org.openmrs.api.ValidationException;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense.Status;
import org.openmrs.module.orderexpansion.api.model.MedicalSupplyOrder;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules.FulfillerOutcome;
import org.openmrs.module.orderexpansion.api.rules.DispenseRules.PersistedState;

public class DispenseRulesTest {
	
	private final MedicalSupplyOrder order = order(5);
	
	// --- checkChange -------------------------------------------------------------------------
	
	@Test
	public void checkChange_shouldAllowAnyStatusForANewRow() {
		DispenseRules.checkChange(null, dispense(order, Status.COMPLETED, 2.0));
	}
	
	@Test
	public void checkChange_shouldAllowPendingToCompleted() {
		CommodityDispense row = dispense(order, Status.COMPLETED, 2.0);
		row.setBillReference("bill-1");
		DispenseRules.checkChange(new PersistedState(100, Status.PENDING, 2.0, "bill-1"), row);
	}
	
	@Test
	public void checkChange_shouldAllowQuantityChangeOnUnbilledPending() {
		DispenseRules.checkChange(new PersistedState(100, Status.PENDING, 2.0, null), dispense(order, Status.PENDING, 3.0));
	}
	
	@Test(expected = APIException.class)
	public void checkChange_shouldRejectQuantityChangeOnBilledPending() {
		CommodityDispense row = dispense(order, Status.PENDING, 3.0);
		row.setBillReference("bill-1");
		DispenseRules.checkChange(new PersistedState(100, Status.PENDING, 2.0, "bill-1"), row);
	}
	
	@Test(expected = APIException.class)
	public void checkChange_shouldRejectStatusChangeOnCompleted() {
		DispenseRules.checkChange(new PersistedState(100, Status.COMPLETED, 2.0, null),
		    dispense(order, Status.DECLINED, 2.0));
	}
	
	@Test(expected = APIException.class)
	public void checkChange_shouldRejectQuantityChangeOnCompleted() {
		DispenseRules.checkChange(new PersistedState(100, Status.COMPLETED, 2.0, null),
		    dispense(order, Status.COMPLETED, 1.0));
	}
	
	@Test
	public void checkChange_shouldAllowSettingFirstBillReferenceOnCompleted() {
		CommodityDispense row = dispense(order, Status.COMPLETED, 2.0);
		row.setBillReference("bill-1");
		DispenseRules.checkChange(new PersistedState(100, Status.COMPLETED, 2.0, null), row);
	}
	
	@Test(expected = APIException.class)
	public void checkChange_shouldRejectChangingABillReference() {
		CommodityDispense row = dispense(order, Status.COMPLETED, 2.0);
		row.setBillReference("bill-2");
		DispenseRules.checkChange(new PersistedState(100, Status.COMPLETED, 2.0, "bill-1"), row);
	}
	
	@Test(expected = APIException.class)
	public void checkChange_shouldRejectMovingToAnotherOrder() {
		DispenseRules.checkChange(new PersistedState(999, Status.PENDING, 2.0, null), dispense(order, Status.PENDING, 2.0));
	}
	
	// --- checkAgainstOtherDispenses -------------------------------------------------------------
	
	@Test
	public void checkAgainstOtherDispenses_shouldAllowPartialDispensesUpToTheOrderedQuantity() {
		DispenseRules.checkAgainstOtherDispenses(5,
		    Arrays.asList(dispense(order, Status.COMPLETED, 2.0), dispense(order, Status.PENDING, 1.0)),
		    dispense(order, Status.PENDING, 2.0));
	}
	
	@Test(expected = APIException.class)
	public void checkAgainstOtherDispenses_shouldCountPendingQuantityAsReserved() {
		DispenseRules.checkAgainstOtherDispenses(5,
		    Arrays.asList(dispense(order, Status.COMPLETED, 2.0), dispense(order, Status.PENDING, 2.0)),
		    dispense(order, Status.COMPLETED, 2.0));
	}
	
	@Test(expected = APIException.class)
	public void checkAgainstOtherDispenses_shouldRejectDispensingAfterADecline() {
		CommodityDispense decline = dispense(order, Status.DECLINED, null);
		DispenseRules.checkAgainstOtherDispenses(5, Collections.singletonList(decline),
		    dispense(order, Status.COMPLETED, 1.0));
	}
	
	@Test(expected = APIException.class)
	public void checkAgainstOtherDispenses_shouldRejectDeclineWhileADispenseAwaitsPayment() {
		DispenseRules.checkAgainstOtherDispenses(5, Collections.singletonList(dispense(order, Status.PENDING, 1.0)),
		    dispense(order, Status.DECLINED, null));
	}
	
	@Test
	public void checkAgainstOtherDispenses_shouldAllowDecliningTheRemainderAfterPartialDispense() {
		DispenseRules.checkAgainstOtherDispenses(5, Collections.singletonList(dispense(order, Status.COMPLETED, 2.0)),
		    dispense(order, Status.DECLINED, null));
	}
	
	@Test
	public void ruleViolations_shouldBeValidationExceptionsWithTheMessageAsAGlobalError() {
		try {
			DispenseRules.checkAgainstOtherDispenses(5, Collections.singletonList(dispense(order, Status.COMPLETED, 5.0)),
			    dispense(order, Status.COMPLETED, 1.0));
			org.junit.Assert.fail("expected a rule violation");
		}
		catch (ValidationException e) {
			// REST answers a ValidationException with 400 and lists its errors; a bare APIException is a 500
			assertEquals(1, e.getErrors().getGlobalErrorCount());
			assertTrue(e.getErrors().getGlobalError().getDefaultMessage().contains("would exceed the ordered quantity"));
		}
	}
	
	// --- fulfillerOutcome ------------------------------------------------------------------------
	
	@Test
	public void fulfillerOutcome_shouldBeInProgressWhilePartlyDispensed() {
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(5,
		    Collections.singletonList(dispense(order, Status.COMPLETED, 2.0)));
		assertEquals(Order.FulfillerStatus.IN_PROGRESS, outcome.getStatus());
	}
	
	@Test
	public void fulfillerOutcome_shouldBeInProgressWhileAwaitingPayment() {
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(5,
		    Collections.singletonList(dispense(order, Status.PENDING, 5.0)));
		assertEquals(Order.FulfillerStatus.IN_PROGRESS, outcome.getStatus());
	}
	
	@Test
	public void fulfillerOutcome_shouldBeCompletedWhenFullyDispensed() {
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(5,
		    Arrays.asList(dispense(order, Status.COMPLETED, 2.0), dispense(order, Status.COMPLETED, 3.0)));
		assertEquals(Order.FulfillerStatus.COMPLETED, outcome.getStatus());
		assertNull(outcome.getComment());
	}
	
	@Test
	public void fulfillerOutcome_shouldBeDeclinedWithTheReasonWhenNothingWasDispensed() {
		CommodityDispense decline = dispense(order, Status.DECLINED, null);
		decline.setComment("Out of stock");
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(5, Collections.singletonList(decline));
		assertEquals(Order.FulfillerStatus.DECLINED, outcome.getStatus());
		assertEquals("Out of stock", outcome.getComment());
	}
	
	@Test
	public void fulfillerOutcome_shouldCompletePartlyDispensedOrderWhenRemainderIsDeclined() {
		CommodityDispense decline = dispense(order, Status.DECLINED, null);
		decline.setComment("Out of stock");
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(5,
		    Arrays.asList(dispense(order, Status.COMPLETED, 2.0), decline));
		assertEquals(Order.FulfillerStatus.COMPLETED, outcome.getStatus());
		assertTrue(outcome.getComment().startsWith("Partially dispensed (2 of 5)"));
		assertTrue(outcome.getComment().endsWith("Out of stock"));
	}
	
	@Test
	public void fulfillerOutcome_shouldReturnToInProgressWhenAllRowsAreVoided() {
		FulfillerOutcome outcome = DispenseRules.fulfillerOutcome(5, Collections.<CommodityDispense> emptyList());
		assertEquals(Order.FulfillerStatus.IN_PROGRESS, outcome.getStatus());
	}
}
