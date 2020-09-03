package com.boeing.cas.supa.ground.pojos;

public class KeyValueUpdate {

	private String key;
	private String role;
	private String value;

	public KeyValueUpdate() {}

	public KeyValueUpdate(String key, String role, String value) {
		this.key = key;
		this.role = role;
		this.value = value;
	}

	public KeyValueUpdate(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {

		return new StringBuilder("[").append(this.getClass().getSimpleName()).append("]").append(":")
				.append(this.key).append(',')
				.append(this.role).append(',')
				.append(this.value)
				.append(']')
			.toString();
	}
}
