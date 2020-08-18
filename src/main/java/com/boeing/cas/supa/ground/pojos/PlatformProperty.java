package com.boeing.cas.supa.ground.pojos;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "PlatformProperty")
public class PlatformProperty extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PlatformID", nullable = false)
	private Platform platform;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PropertyID", nullable = false)
	private Property property;
	
	@OneToMany(mappedBy = "platformProperty", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<PropertyValue> propertyValues;

	public Platform getPlatform() {
		return platform;
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public Set<PropertyValue> getPropertyValues() {
		if (propertyValues == null) {
			propertyValues = new HashSet<PropertyValue>();
		}
		return propertyValues;
	}

	public void setPropertyValues(Set<PropertyValue> propertyValues) {
		this.propertyValues = propertyValues;
	}
}