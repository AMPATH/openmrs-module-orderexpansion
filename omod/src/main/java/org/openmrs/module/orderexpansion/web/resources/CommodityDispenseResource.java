package org.openmrs.module.orderexpansion.web.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.orderexpansion.api.CommodityDispenseService;
import org.openmrs.module.orderexpansion.api.model.CommodityDispense;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.InvalidSearchException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * {@code /ws/rest/v1/commoditydispense}: dispensing against any dispensable commodity order.
 * <ul>
 * <li>{@code POST} creates a PENDING, COMPLETED or DECLINED dispense</li>
 * <li>{@code POST /{uuid}} sets the bill reference, or moves a PENDING dispense on</li>
 * <li>{@code GET ?order=} / {@code ?patient=&orderType=} / {@code ?orderType=&location=}, each with
 * optional {@code fromDate} and {@code toDate} on the dispensed date</li>
 * <li>{@code DELETE /{uuid}?reason=} voids; purging is not supported</li>
 * </ul>
 */
@Resource(name = RestConstants.VERSION_1
        + "/commoditydispense", supportedClass = CommodityDispense.class, supportedOpenmrsVersions = { "2.6.* - 9.*" })
public class CommodityDispenseResource extends DataDelegatingCrudResource<CommodityDispense> {
	
	private CommodityDispenseService service() {
		return Context.getService(CommodityDispenseService.class);
	}
	
	@Override
	public CommodityDispense newDelegate() {
		return new CommodityDispense();
	}
	
	@Override
	public CommodityDispense getByUniqueId(String uuid) {
		return service().getCommodityDispenseByUuid(uuid);
	}
	
	@Override
	public CommodityDispense save(CommodityDispense delegate) {
		return service().saveCommodityDispense(delegate);
	}
	
	@Override
	protected void delete(CommodityDispense delegate, String reason, RequestContext context) throws ResponseException {
		service().voidCommodityDispense(delegate, reason);
	}
	
	@Override
	public void purge(CommodityDispense delegate, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Commodity dispenses can be voided but not purged");
	}
	
	@PropertyGetter("display")
	public String getDisplayString(CommodityDispense delegate) {
		String concept = delegate.getOrder() != null && delegate.getOrder().getConcept() != null
		        ? delegate.getOrder().getConcept().getDisplayString()
		        : "";
		String quantity = delegate.getQuantity() == null ? "" : delegate.getQuantity() + " ";
		return (quantity + concept + " (" + delegate.getStatus() + ")").trim();
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			boolean full = rep instanceof FullRepresentation;
			Representation nested = full ? Representation.DEFAULT : Representation.REF;
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("order", nested);
			description.addProperty("patient", Representation.REF);
			description.addProperty("encounter", Representation.REF);
			description.addProperty("quantity");
			description.addProperty("quantityUnits", nested);
			description.addProperty("status");
			description.addProperty("statusReason", nested);
			description.addProperty("billReference");
			description.addProperty("dispenser", Representation.REF);
			description.addProperty("location", Representation.REF);
			description.addProperty("dateDispensed");
			description.addProperty("comment");
			description.addProperty("voided");
			if (full) {
				description.addProperty("auditInfo");
			}
			description.addSelfLink();
			if (!full) {
				description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			}
			return description;
		}
		return null;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("order");
		description.addRequiredProperty("status");
		description.addRequiredProperty("dispenser");
		description.addRequiredProperty("location");
		description.addProperty("patient");
		description.addProperty("encounter");
		description.addProperty("quantity");
		description.addProperty("quantityUnits");
		description.addProperty("statusReason");
		description.addProperty("billReference");
		description.addProperty("dateDispensed");
		description.addProperty("comment");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("status");
		description.addProperty("statusReason");
		description.addProperty("quantity");
		description.addProperty("quantityUnits");
		description.addProperty("billReference");
		description.addProperty("dateDispensed");
		description.addProperty("comment");
		return description;
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		String orderUuid = context.getParameter("order");
		String patientUuid = context.getParameter("patient");
		String orderTypeUuids = context.getParameter("orderType");
		String locationUuid = context.getParameter("location");
		if (StringUtils.isBlank(orderUuid) && StringUtils.isBlank(patientUuid) && StringUtils.isBlank(orderTypeUuids)) {
			throw new InvalidSearchException("Search commodity dispenses by order, patient or orderType");
		}
		
		Order order = null;
		if (StringUtils.isNotBlank(orderUuid)) {
			order = Context.getOrderService().getOrderByUuid(orderUuid);
			if (order == null) {
				throw new ObjectNotFoundException("No order with uuid " + orderUuid);
			}
		}
		Patient patient = null;
		if (StringUtils.isNotBlank(patientUuid)) {
			patient = Context.getPatientService().getPatientByUuid(patientUuid);
			if (patient == null) {
				throw new ObjectNotFoundException("No patient with uuid " + patientUuid);
			}
		}
		List<OrderType> orderTypes = new ArrayList<OrderType>();
		if (StringUtils.isNotBlank(orderTypeUuids)) {
			for (String uuid : orderTypeUuids.split(",")) {
				OrderType orderType = Context.getOrderService().getOrderTypeByUuid(uuid.trim());
				if (orderType == null) {
					throw new ObjectNotFoundException("No order type with uuid " + uuid.trim());
				}
				orderTypes.add(orderType);
			}
		}
		Location location = null;
		if (StringUtils.isNotBlank(locationUuid)) {
			location = Context.getLocationService().getLocationByUuid(locationUuid);
			if (location == null) {
				throw new ObjectNotFoundException("No location with uuid " + locationUuid);
			}
		}
		Date fromDate = parseDate(context.getParameter("fromDate"));
		Date toDate = parseDate(context.getParameter("toDate"));
		
		List<CommodityDispense> results = service().getCommodityDispenses(order, patient, orderTypes, location, fromDate,
		    toDate, context.getIncludeAll());
		return new NeedsPaging<CommodityDispense>(results, context);
	}
	
	private static Date parseDate(String value) {
		return StringUtils.isBlank(value) ? null : (Date) ConversionUtil.convert(value, Date.class);
	}
}
