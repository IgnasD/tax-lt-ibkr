package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lt.ign.apps.tax.model.event.Dividends;
import lt.ign.apps.tax.util.MathUtils;

public class TaxedDividends extends Dividends implements CurrencyConvertable<TaxedDividends> {

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

	@Override
	public TaxedDividends convertCurrency(ExchangeRate exchangeRate) {
		if (getCurrency() != exchangeRate.sourceCurrency()) {
			throw new IllegalArgumentException(String.format("Dividends currency (%s) does not match exchange rate source currency (%s)",
				getCurrency(), exchangeRate.sourceCurrency()));
		}
		return new TaxedDividends(getSymbol(), getDateTime(), exchangeRate.targetCurrency(),
			MathUtils.divide(getAmount(), exchangeRate.rate()), MathUtils.divide(withholdingTax, exchangeRate.rate()));
	}

}
