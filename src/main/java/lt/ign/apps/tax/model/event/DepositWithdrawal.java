package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import lt.ign.apps.tax.model.Currency;

public class DepositWithdrawal extends ReportEntry {

	private final Currency currency;
	private final LocalDate date;
	private final BigDecimal amount;

	public DepositWithdrawal(Currency currency, LocalDate date, BigDecimal amount) {
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

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s %.2f%s", date, amount, currency);
	}

}
