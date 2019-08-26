package com.boeing.cas.supa.ground.pojos;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class FlightCount {

	private String tail;
	private int count = 0;
	private int processed = 0;
	private String version;
    private Instant createTs;
    private Instant updateTs;

	public FlightCount(String tail) {
		this.tail = tail;
	}

	public FlightCount(String tail, int count) {

		this.tail = tail;
		this.count = count;
	}

	public FlightCount(String tail, int count, int processed, String version, Instant createTs, Instant updateTs) {

		this.tail = tail;
		this.count = count;
		this.processed = processed;
		if (version == null || version.equals("")) {
			this.version = "unknown";
		} else {
			this.version = version;
		}
		this.createTs = createTs;
		this.updateTs = updateTs;
	}

	public String getTail() {
		return tail;
	}

	public void setTail(String tail) {
		this.tail = tail;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		if (version == null || version.equals("")) {
			this.version = "unknown";
		} else {
			this.version = version;
		}
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getProcessed() {
		return processed;
	}

	public void setProcessed(int processed) {
		this.processed = processed;
	}

	public String getCreateTs() {
		DateTimeFormatter formatter =
				DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
						.withLocale( Locale.US )
						.withZone( ZoneId.systemDefault() );

		return formatter.format(this.createTs);
	}

	public void setCreateTs(Instant createTs) {
		this.createTs = createTs;
	}

    public String getUpdateTs() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
                        .withLocale( Locale.US )
                        .withZone( ZoneId.systemDefault() );

        return formatter.format(this.updateTs);
    }

    public void setUpdateTs(Instant updateTs) {
        this.updateTs = updateTs;
    }

}

