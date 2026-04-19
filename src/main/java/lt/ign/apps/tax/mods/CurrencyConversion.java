package lt.ign.apps.tax.mods;

import lt.ign.apps.tax.model.ExchangeRate;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.util.MathUtils;

public class CurrencyConversion extends Modifier {

	private final ExchangeRate exchangeRate;

	public CurrencyConversion(ExchangeRate exchangeRate) {
		super(exchangeRate.date().atTime(23, 59, 59));
		this.exchangeRate = exchangeRate;
	}

	public ExchangeRate getExchangeRate() {
		return exchangeRate;
	}

	@Override
	public Trade apply(Trade trade) {
		if (trade.getCurrency() != exchangeRate.sourceCurrency()) {
			throw new IllegalArgumentException(
				String.format("Trade currency (%s) does not match expected source currency (%s)", trade.getCurrency(),
					exchangeRate.sourceCurrency()));
		}
		var proceeds = MathUtils.divide(trade.getProceeds(), exchangeRate.rate());
		var fees = MathUtils.divide(trade.getFees(), exchangeRate.rate());
		return new Trade(trade.getSymbol(), trade.getDateTime(), trade.getType(), trade.getQuantity(), proceeds, fees,
			exchangeRate.targetCurrency());
	}

}
