package lt.ign.apps.tax.mods;

import java.time.LocalDateTime;

import lt.ign.apps.tax.model.event.Trade;

public class StockSplit extends Modifier {

	private final int multiplier;

	public StockSplit(LocalDateTime dateTime, int multiplier) {
		super(dateTime);
		this.multiplier = multiplier;
	}

	public int getMultiplier() {
		return multiplier;
	}

	@Override
	public Trade apply(Trade trade) {
		var quantity = trade.getQuantity() * multiplier;
		return new Trade(trade.getSymbol(), trade.getDateTime(), trade.getType(), quantity, trade.getProceeds(), trade.getFees(),
			trade.getCurrency());
	}

}
