package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.ReportEntry;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.CurrencyConversion;

public class EurConverter {

	private final Map<LocalDate, BigDecimal> usdEurRates;

	public EurConverter(Map<LocalDate, BigDecimal> usdEurRates) {
		this.usdEurRates = usdEurRates;
	}

	public ReportEntry convertToEur(ReportEntry entry) {
		if (!(entry instanceof Trade)) {
			return entry;
		}

		var trade = (Trade) entry;
		var currency = trade.getCurrency();
		if (currency == Currency.EUR) {
			return entry;
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
