package com.boeing.cas.supa.ground.pojos;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "Property")
public class Property extends BaseEntity {
	private String name;
	
	@OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<PlatformProperty> platformProperties;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Set<PlatformProperty> getPlatformProperties() {
		if (platformProperties == null) {
			platformProperties = new HashSet<PlatformProperty>();
		}
		return platformProperties;
	}

	public void setPlatformProperties(Set<PlatformProperty> platformProperties) {
		this.platformProperties = platformProperties;
	}
}