package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record ExchangeRate(LocalDate date, Currency sourceCurrency, Currency targetCurrency, BigDecimal rate) {

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%.4f %s/%s @ %s", rate, sourceCurrency, targetCurrency, date);
	}

}
