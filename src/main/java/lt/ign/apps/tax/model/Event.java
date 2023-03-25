package lt.ign.apps.tax.model;

import java.time.LocalDateTime;

public abstract class Event {

	private String symbol;
	private LocalDateTime dateTime;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

}
