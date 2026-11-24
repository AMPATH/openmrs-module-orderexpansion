package org.openmrs.module.orderexpansion.api.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openmrs.OrderType;
import org.openmrs.api.APIException;

public class OrderTypeRulesTest {
	
	private static OrderType orderType(String uuid) {
		OrderType orderType = new OrderType();
		orderType.setUuid(uuid);
		return orderType;
	}
	
	@Test
	public void isDispensable_shouldBeTrueOnlyForOrderTypesMarkedDispensable() {
		OrderTypeRules rules = OrderTypeRules
		        .parse("{\"supply\": {\"dispensable\": true}, \"other\": {\"dispensable\": false}}");
		assertTrue(rules.isDispensable(orderType("supply")));
		assertFalse(rules.isDispensable(orderType("other")));
		assertFalse(rules.isDispensable(orderType("unlisted")));
		assertFalse(rules.isDispensable(null));
	}
	
	@Test
	public void parse_shouldIgnoreRuleKeysAddedByLaterReleases() {
		OrderTypeRules rules = OrderTypeRules.parse(
		    "{\"nutrition\": {\"dispensable\": true, \"unitsConceptSet\": \"x\", \"requiredAttributeTypes\": []}}");
		assertTrue(rules.isDispensable(orderType("nutrition")));
	}
	
	@Test
	public void parse_shouldTreatBlankAsNoRules() {
		assertFalse(OrderTypeRules.parse("  ").isDispensable(orderType("supply")));
		assertFalse(OrderTypeRules.parse(null).isDispensable(orderType("supply")));
	}
	
	@Test(expected = APIException.class)
	public void parse_shouldRejectInvalidJson() {
		OrderTypeRules.parse("{not json");
	}
}
