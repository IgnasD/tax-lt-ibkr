package lt.ign.apps.tax.model.event;

import java.time.LocalDateTime;

public abstract class StockEvent extends ReportEntry {

	private final String symbol;
	private final LocalDateTime dateTime;

	public StockEvent(String symbol, LocalDateTime dateTime) {
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
