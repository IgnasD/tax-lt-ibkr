package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import lt.ign.apps.tax.model.Currency;

public class WithholdingTax extends ReportEntry {

	private final Currency currency;
	private final LocalDate date;
	private final Type type;
	private final BigDecimal amount;
	private final Optional<String> symbol;

	public WithholdingTax(Currency currency, LocalDate date, Type type, BigDecimal amount, Optional<String> symbol) {
		this.currency = currency;
		this.date = date;
		this.type = type;
		this.amount = amount;
		this.symbol = symbol;
	}

	public Currency getCurrency() {
		return currency;
	}

	public LocalDate getDate() {
		return date;
	}

	public Type getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Optional<String> getSymbol() {
		return symbol;
	}

	public static enum Type {
		DIVIDEND, CREDIT_INTEREST
	}

}
