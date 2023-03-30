package lt.ign.apps.tax.mods;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.util.MathUtils;

public class CurrencyConversion extends Modifier {

	private final Currency sourceCurrency;
	private final Currency targetCurrency;
	private final BigDecimal conversionRate;

	public CurrencyConversion(LocalDateTime dateTime, Currency sourceCurrency, Currency targetCurrency, BigDecimal conversionRate) {
		super(dateTime);
		this.sourceCurrency = sourceCurrency;
		this.targetCurrency = targetCurrency;
		this.conversionRate = conversionRate;
	}

	public Currency getSourceCurrency() {
		return sourceCurrency;
	}

	public Currency getTargetCurrency() {
		return targetCurrency;
	}

	public BigDecimal getConversionRate() {
		return conversionRate;
	}

	@Override
	public Trade apply(Trade trade) {
		if (trade.getCurrency() != sourceCurrency) {
			throw new IllegalArgumentException(
				String.format("Trade currency (%s) does not match expected source currency (%s)", trade.getCurrency(), sourceCurrency));
		}
		var proceeds = MathUtils.divide(trade.getProceeds(), conversionRate);
		var fees = MathUtils.divide(trade.getFees(), conversionRate);
		return new Trade(trade.getSymbol(), trade.getDateTime(), trade.getType(), trade.getQuantity(), proceeds, fees, targetCurrency);
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%.4f %s/%s", conversionRate, sourceCurrency, targetCurrency);
	}

}
