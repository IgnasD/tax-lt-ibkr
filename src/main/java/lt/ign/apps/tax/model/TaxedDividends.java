package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lt.ign.apps.tax.model.event.Dividends;

public class TaxedDividends extends Dividends {

	private final BigDecimal withholdingTax;

	public TaxedDividends(String symbol, LocalDateTime dateTime, Currency currency, BigDecimal amount, BigDecimal withholdingTax) {
		super(symbol, dateTime, currency, amount);
		this.withholdingTax = withholdingTax;
	}

	public BigDecimal getWithholdingTax() {
		return withholdingTax;
	}

	public static TaxedDividends create(Dividends dividends, BigDecimal withholdingTax) {
		return new TaxedDividends(dividends.getSymbol(), dividends.getDateTime(), dividends.getCurrency(), dividends.getAmount(),
			withholdingTax);
	}

}
