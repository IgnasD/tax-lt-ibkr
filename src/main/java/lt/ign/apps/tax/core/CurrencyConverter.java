package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.CurrencyConversion;

public class CurrencyConverter {

	private final Map<LocalDate, BigDecimal> usdEurRates;

	public CurrencyConverter(Map<LocalDate, BigDecimal> usdEurRates) {
		this.usdEurRates = usdEurRates;
	}

	public Trade convert(Trade trade, Currency targetCurrency) {
		var currency = trade.getCurrency();
		if (currency == targetCurrency) {
			return trade;
		}

		if (currency != Currency.USD || targetCurrency != Currency.EUR) {
			throw new UnsupportedOperationException(String.format("Unsupported conversion %s -> %s", currency, targetCurrency));
		}

		var conversionDate = trade.getDateTime().toLocalDate();
		BigDecimal conversionRate = null;
		while (conversionRate == null) {
			conversionRate = usdEurRates.get(conversionDate);
			conversionDate = conversionDate.minusDays(1);
		}

		return trade.modify(new CurrencyConversion(conversionDate.atStartOfDay(), currency, targetCurrency, conversionRate));
	}

}
