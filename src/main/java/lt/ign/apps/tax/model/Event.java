package lt.ign.apps.tax.model;

import java.time.LocalDateTime;

public abstract class Event {

	private final String symbol;
	private final LocalDateTime dateTime;

	public Event(String symbol, LocalDateTime dateTime) {
		this.symbol = symbol;
		this.dateTime = dateTime;
	}

	public String getSymbol() {
		return symbol;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

}
