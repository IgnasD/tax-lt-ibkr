package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lt.ign.apps.tax.model.Currency;

public class Dividends extends StockEvent {

	private final Currency currency;
	private final BigDecimal tax;
	private final BigDecimal gross;
	private final BigDecimal net;

	public Dividends(String symbol, LocalDateTime dateTime, Currency currency, BigDecimal tax, BigDecimal gross, BigDecimal net) {
		super(symbol, dateTime);
		this.currency = currency;
		this.tax = tax;
		this.gross = gross;
		this.net = net;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getTax() {
		return tax;
	}

	public BigDecimal getGross() {
		return gross;
	}

	public BigDecimal getNet() {
		return net;
	}

}
