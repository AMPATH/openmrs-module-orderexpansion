package org.openmrs.module.orderexpansion.web.resources;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.StringProperty;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.orderexpansion.commodity.CommodityOrder;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.SubClassHandler;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingSubclassHandler;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubclassHandler;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_10.OrderResource1_10;

/**
 * REST type {@code commodityorder} on {@code /order}: commodity orders (nutrition, later blood
 * products), told apart by their order type.
 */
@SubClassHandler(supportedClass = CommodityOrder.class, supportedOpenmrsVersions = { "2.6.* - 9.*" })
public class CommodityOrderSubclassHandler extends BaseDelegatingSubclassHandler<Order, CommodityOrder> implements DelegatingSubclassHandler<Order, CommodityOrder> {
	
	private static OrderResource1_10 orderResource() {
		return (OrderResource1_10) Context.getService(RestService.class).getResourceBySupportedClass(Order.class);
	}
	
	@Override
	public String getTypeName() {
		return "commodityorder";
	}
	
	@Override
	public PageableResult getAllByType(RequestContext requestContext) throws ResourceDoesNotSupportOperationException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public CommodityOrder newDelegate() {
		return new CommodityOrder();
	}
	
	@PropertyGetter("display")
	public static String getDisplay(CommodityOrder delegate) {
		return orderResource().getDisplayString(delegate);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			DelegatingResourceDescription d = orderResource().getRepresentationDescription(rep);
			d.addProperty("quantity");
			d.addProperty("quantityUnits", rep instanceof FullRepresentation ? Representation.DEFAULT : Representation.REF);
			return d;
		}
		return null;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription d = orderResource().getCreatableProperties();
		d.addProperty("quantity");
		d.addProperty("quantityUnits");
		return d;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		return orderResource().getUpdatableProperties();
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl model = (ModelImpl) orderResource().getGETModel(rep);
		return model.property("quantity", new DoubleProperty()).property("quantityUnits", new StringProperty());
	}
	
	@Override
	public Model getCREATEModel(Representation rep) {
		ModelImpl model = (ModelImpl) orderResource().getCREATEModel(rep);
		return model.property("quantity", new DoubleProperty()).property("quantityUnits",
		    new StringProperty().example("uuid"));
	}
}
