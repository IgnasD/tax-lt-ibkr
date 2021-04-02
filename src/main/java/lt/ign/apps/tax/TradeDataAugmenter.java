package lt.ign.apps.tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.Trade;
import lt.ign.apps.tax.model.TradeEur;

public class TradeDataAugmenter {

	private final CurrencyConverter cc;

	public TradeDataAugmenter(CurrencyConverter cc) {
		this.cc = cc;
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
			var date = dateTime.toLocalDate();
			tradeEur.setProceedsEur(cc.usdToEur(proceeds, date));
			tradeEur.setFeesEur(cc.usdToEur(fees, date));
		} else {
			throw new IllegalStateException();
		}
		return tradeEur;
	}

}
