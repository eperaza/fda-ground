package com.boeing.cas.supa.ground.pojos;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;

@MappedSuperclass
public class BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(updatable = false, insertable = false)
	private Date modifiedTime;
	private String createdBy;

	private String updatedBy;

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getModifiedTime() {
		if (modifiedTime == null) {
			modifiedTime = Calendar.getInstance().getTime();
		}
		return modifiedTime;
	}

	public void setModifiedTime(Date createdDate) {
		this.modifiedTime = createdDate;
	}

	public String getCreatedBy() {
		if (createdBy == null) {
			createdBy = "SYSTEM";
		}
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
}

