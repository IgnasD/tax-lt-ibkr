package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Event;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.CurrencyConversion;

public class EurConverter {

	private final Map<LocalDate, BigDecimal> usdEurRates;

	public EurConverter(Map<LocalDate, BigDecimal> usdEurRates) {
		this.usdEurRates = usdEurRates;
	}

	public Event convertToEur(Event event) {
		if (!(event instanceof Trade)) {
			return event;
		}

		var trade = (Trade) event;
		var currency = trade.getCurrency();
		if (currency == Currency.EUR) {
			return event;
		}
		if (currency != Currency.USD) {
			throw new UnsupportedOperationException(String.format("Unknown conversion rate for %s/EUR", currency));
		}

		var conversionDate = trade.getDateTime().toLocalDate();
		BigDecimal conversionRate = null;
		while (conversionRate == null) {
			conversionRate = usdEurRates.get(conversionDate);
			conversionDate = conversionDate.minusDays(1);
		}

		return trade.modify(new CurrencyConversion(conversionDate.atStartOfDay(), Currency.USD, Currency.EUR, conversionRate));
	}

}
