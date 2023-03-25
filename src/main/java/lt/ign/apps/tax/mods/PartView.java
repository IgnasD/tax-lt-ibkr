package lt.ign.apps.tax.mods;

import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.util.MathUtils;

public class PartView implements Modifier {

	private final int targetQuantity;

	public PartView(int targetQuantity) {
		this.targetQuantity = targetQuantity;
	}

	public int getTargetQuantity() {
		return targetQuantity;
	}

	@Override
	public Trade apply(Trade trade) {
		var sourceQuantity = trade.getQuantity();
		if (sourceQuantity < targetQuantity) {
			throw new IllegalArgumentException(
				String.format("Trade quantity (%d) is less than target quantity (%d)", sourceQuantity, targetQuantity));
		}
		var multiplicand = MathUtils.divide(targetQuantity, sourceQuantity);
		var proceeds = trade.getProceeds().multiply(multiplicand);
		var fees = trade.getFees().multiply(multiplicand);
		return new Trade(trade.getSymbol(), trade.getDateTime(), trade.getType(), targetQuantity, proceeds, fees, trade.getCurrency());
	}

}
