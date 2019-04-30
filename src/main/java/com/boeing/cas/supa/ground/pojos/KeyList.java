package com.boeing.cas.supa.ground.pojos;

import java.util.ArrayList;
import java.util.List;

public class KeyList {

	List<KeyValueUpdate> keys;

	public KeyList() {}

	public KeyList(String key, String role, String value) {

		KeyValueUpdate keyValueUpdate = new KeyValueUpdate(key, role, value);
		this.keys = new ArrayList<KeyValueUpdate>();
		this.keys.add(keyValueUpdate);

	}

	public KeyList(List<KeyValueUpdate> keys) {
		this.keys = keys;
	}

	public List<KeyValueUpdate> getKeys() {
		return keys;
	}

	public void setKeys(List<KeyValueUpdate> newKeys) {
		this.keys = new ArrayList<>();
		if (newKeys != null) {
			for (KeyValueUpdate key : newKeys) {
				this.keys.add(key);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("KeyList{");
		for (KeyValueUpdate key : this.keys) {
			sb.append(key.getKey() + " " + key.getRole() + " " + key.getValue());
		}
		sb.append('}');
		return sb.toString();
	}
}
