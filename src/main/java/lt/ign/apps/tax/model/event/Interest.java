package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDate;

import lt.ign.apps.tax.model.Currency;

public class Interest extends ReportEntry {

	private final Currency currency;
	private final LocalDate date;
	private final BigDecimal amount;

	public Interest(Currency currency, LocalDate date, BigDecimal amount) {
		this.currency = currency;
		this.date = date;
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public LocalDate getDate() {
		return date;
	}

	public BigDecimal getAmount() {
		return amount;
	}

}
