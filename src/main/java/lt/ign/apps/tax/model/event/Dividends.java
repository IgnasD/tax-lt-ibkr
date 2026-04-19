package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lt.ign.apps.tax.model.Currency;

public class Dividends extends StockEvent {

	private final Currency currency;
	private final BigDecimal amount;

	public Dividends(String symbol, LocalDateTime dateTime, Currency currency, BigDecimal amount) {
		super(symbol, dateTime);
		this.currency = currency;
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

}
