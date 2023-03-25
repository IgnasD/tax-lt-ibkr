package lt.ign.apps.tax.mods;

import lt.ign.apps.tax.model.event.Trade;

public class StockSplit implements Modifier {

	private final int multiplier;

	public StockSplit(int multiplier) {
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
