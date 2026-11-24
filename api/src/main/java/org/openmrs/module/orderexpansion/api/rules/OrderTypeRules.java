package org.openmrs.module.orderexpansion.api.rules;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.OrderType;
import org.openmrs.api.APIException;

/**
 * Per-order-type rules read from the {@code orderexpansion.orderTypeRules} global property, a JSON
 * object keyed by order type UUID. Unknown keys are ignored so later releases can add rules (for
 * example units sets or required attribute types) without breaking older configuration.
 */
public class OrderTypeRules {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Rule {
		
		private boolean dispensable;
		
		public boolean isDispensable() {
			return dispensable;
		}
		
		public void setDispensable(boolean dispensable) {
			this.dispensable = dispensable;
		}
	}
	
	private final Map<String, Rule> rulesByOrderTypeUuid;
	
	private OrderTypeRules(Map<String, Rule> rulesByOrderTypeUuid) {
		this.rulesByOrderTypeUuid = rulesByOrderTypeUuid;
	}
	
	/**
	 * @param json the global property value; blank means no rules
	 * @throws APIException if the value is not a JSON object of rules
	 */
	public static OrderTypeRules parse(String json) {
		if (StringUtils.isBlank(json)) {
			return new OrderTypeRules(Collections.<String, Rule> emptyMap());
		}
		try {
			Map<String, Rule> rules = MAPPER.readValue(json, new TypeReference<Map<String, Rule>>() {});
			return new OrderTypeRules(rules == null ? Collections.<String, Rule> emptyMap() : rules);
		}
		catch (IOException e) {
			throw new APIException("The orderexpansion.orderTypeRules global property is not valid JSON: " + e.getMessage(),
			        e);
		}
	}
	
	public boolean isDispensable(OrderType orderType) {
		if (orderType == null) {
			return false;
		}
		Rule rule = rulesByOrderTypeUuid.get(orderType.getUuid());
		return rule != null && rule.isDispensable();
	}
}
