package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.Trade;
import lt.ign.apps.tax.model.TradeEur;
import lt.ign.apps.tax.util.MathUtils;

public class TradeDataAugmenter {

	private final Map<LocalDate, BigDecimal> usdEurRates;

	public TradeDataAugmenter(Map<LocalDate, BigDecimal> usdEurRates) {
		this.usdEurRates = usdEurRates;
	}

	public List<TradeEur> augmentEurData(List<Trade> trades) {
		return trades.stream().map(this::augmentEurData).collect(Collectors.toList());
	}

	private TradeEur augmentEurData(Trade trade) {
		var tradeEur = new TradeEur();
		tradeEur.setType(trade.getType());
		tradeEur.setSymbol(trade.getSymbol());
		var dateTime = trade.getDateTime();
		tradeEur.setDateTime(dateTime);
		tradeEur.setQuantity(trade.getQuantity());
		var proceeds = trade.getProceeds();
		tradeEur.setProceeds(proceeds);
		var fees = trade.getFees();
		tradeEur.setFees(fees);
		var currency = trade.getCurrency();
		tradeEur.setCurrency(currency);
		if (currency == Currency.EUR) {
			tradeEur.setProceedsEur(proceeds);
			tradeEur.setFeesEur(fees);
			tradeEur.setConversionRate(BigDecimal.ONE);
		} else if (currency == Currency.USD) {
			var conversionDate = dateTime.toLocalDate();
			BigDecimal conversionRate = null;
			while (conversionRate == null) {
				conversionRate = usdEurRates.get(conversionDate);
				conversionDate = conversionDate.minusDays(1);
			}
			tradeEur.setProceedsEur(MathUtils.divide(proceeds, conversionRate));
			tradeEur.setFeesEur(MathUtils.divide(fees, conversionRate));
			tradeEur.setConversionRate(conversionRate);
		} else {
			throw new IllegalStateException();
		}
		return tradeEur;
	}

}
