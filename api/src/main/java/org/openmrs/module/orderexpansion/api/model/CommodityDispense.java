package org.openmrs.module.orderexpansion.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.util.Date;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

/**
 * One dispensing event against a commodity order (medical supply, nutrition, blood products, ...).
 * The commodity is the order's order type; several rows per order allow partial dispensing.
 */
@Entity
@Table(name = "commodity_dispense")
public class CommodityDispense extends BaseOpenmrsData {
	
	private static final long serialVersionUID = 1L;
	
	public enum Status {
		/** Quantity set and reserved, awaiting payment before hand-over (prepaid payment modes). */
		PENDING,
		/** Handed over to the patient. */
		COMPLETED,
		/** Not dispensed; closes the order's remaining quantity. */
		DECLINED
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "commodity_dispense_id")
	private Integer commodityDispenseId;
	
	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;
	
	@ManyToOne
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;
	
	@ManyToOne
	@JoinColumn(name = "encounter_id")
	private Encounter encounter;
	
	@Column(name = "quantity")
	private Double quantity;
	
	@ManyToOne
	@JoinColumn(name = "quantity_units")
	private Concept quantityUnits;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private Status status;
	
	@ManyToOne
	@JoinColumn(name = "status_reason")
	private Concept statusReason;
	
	@Column(name = "bill_reference", length = 38)
	private String billReference;
	
	@ManyToOne
	@JoinColumn(name = "dispenser", nullable = false)
	private Provider dispenser;
	
	@ManyToOne
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;
	
	@Column(name = "date_dispensed")
	private Date dateDispensed;
	
	@Column(name = "comment", columnDefinition = "text")
	private String comment;
	
	@Override
	public Integer getId() {
		return commodityDispenseId;
	}
	
	@Override
	public void setId(Integer id) {
		this.commodityDispenseId = id;
	}
	
	public Integer getCommodityDispenseId() {
		return commodityDispenseId;
	}
	
	public void setCommodityDispenseId(Integer commodityDispenseId) {
		this.commodityDispenseId = commodityDispenseId;
	}
	
	public Order getOrder() {
		return order;
	}
	
	/**
	 * Sets the order and, when no patient is set yet, takes the order's patient. This has to happen
	 * here rather than in the service: core validates the arguments of service save methods before the
	 * method body runs. Hibernate uses field access, so loading a row does not call this.
	 */
	public void setOrder(Order order) {
		this.order = order;
		if (patient == null && order != null) {
			patient = order.getPatient();
		}
	}
	
	public Patient getPatient() {
		return patient;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	public Encounter getEncounter() {
		return encounter;
	}
	
	public void setEncounter(Encounter encounter) {
		this.encounter = encounter;
	}
	
	public Double getQuantity() {
		return quantity;
	}
	
	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
	
	public Concept getQuantityUnits() {
		return quantityUnits;
	}
	
	public void setQuantityUnits(Concept quantityUnits) {
		this.quantityUnits = quantityUnits;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Concept getStatusReason() {
		return statusReason;
	}
	
	public void setStatusReason(Concept statusReason) {
		this.statusReason = statusReason;
	}
	
	public String getBillReference() {
		return billReference;
	}
	
	public void setBillReference(String billReference) {
		this.billReference = billReference;
	}
	
	public Provider getDispenser() {
		return dispenser;
	}
	
	public void setDispenser(Provider dispenser) {
		this.dispenser = dispenser;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Date getDateDispensed() {
		return dateDispensed;
	}
	
	public void setDateDispensed(Date dateDispensed) {
		this.dateDispensed = dateDispensed;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
}
