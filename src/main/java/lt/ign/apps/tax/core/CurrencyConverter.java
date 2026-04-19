package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.CurrencyConvertable;
import lt.ign.apps.tax.model.ExchangeRate;

public class CurrencyConverter {

	private final Map<LocalDate, BigDecimal> usdEurRates;

	public CurrencyConverter(Map<LocalDate, BigDecimal> usdEurRates) {
		this.usdEurRates = usdEurRates;
	}

	public ExchangeRate getExchangeRate(LocalDateTime dateTime, Currency sourceCurrency, Currency targetCurrency) {
		if (sourceCurrency != Currency.USD || targetCurrency != Currency.EUR) {
			throw new UnsupportedOperationException(String.format("Unsupported conversion %s -> %s", sourceCurrency, targetCurrency));
		}

		var conversionDate = dateTime.toLocalDate();
		BigDecimal conversionRate = null;
		for (;;) {
			conversionRate = usdEurRates.get(conversionDate);
			if (conversionRate != null) {
				break;
			}
			conversionDate = conversionDate.minusDays(1);
		}
		return new ExchangeRate(conversionDate, sourceCurrency, targetCurrency, conversionRate);
	}

	public <T extends CurrencyConvertable<T>> T convert(T convertable, Currency targetCurrency) {
		var currency = convertable.getCurrency();
		if (currency == targetCurrency) {
			return convertable;
		}

		var exchangeRate = getExchangeRate(convertable.getDateTime(), currency, targetCurrency);
		return convertable.convertCurrency(exchangeRate);
	}

}
