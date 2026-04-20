package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lt.ign.apps.tax.model.event.Interest;
import lt.ign.apps.tax.util.MathUtils;

public class TaxedInterest extends Interest implements CurrencyConvertable<TaxedInterest> {

	private final BigDecimal withholdingTax;

	public TaxedInterest(Currency currency, LocalDate date, BigDecimal amount, BigDecimal withholdingTax) {
		super(currency, date, amount);
		this.withholdingTax = withholdingTax;
	}

	public BigDecimal getWithholdingTax() {
		return withholdingTax;
	}

	@Override
	public LocalDateTime getDateTime() {
		return getDate().atStartOfDay();
	}

	public static TaxedInterest create(Interest interest, BigDecimal withholdingTax) {
		return new TaxedInterest(interest.getCurrency(), interest.getDate(), interest.getAmount(), withholdingTax);
	}

	@Override
	public TaxedInterest convertCurrency(ExchangeRate exchangeRate) {
		if (getCurrency() != exchangeRate.sourceCurrency()) {
			throw new IllegalArgumentException(String.format("Interest currency (%s) does not match exchange rate source currency (%s)",
				getCurrency(), exchangeRate.sourceCurrency()));
		}
		return new TaxedInterest(exchangeRate.targetCurrency(), getDate(), MathUtils.divide(getAmount(), exchangeRate.rate()),
			MathUtils.divide(withholdingTax, exchangeRate.rate()));
	}

}
