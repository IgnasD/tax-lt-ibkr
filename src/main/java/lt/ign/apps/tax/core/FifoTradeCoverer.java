package lt.ign.apps.tax.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Trade;

public class FifoTradeCoverer {

	public <T extends Trade> List<Cover<T>> cover(List<T> trades) {
		Map<String, List<TrackedTrade<T>>> opens = new HashMap<>();
		List<T> closes = new ArrayList<>();

		trades.stream().sorted(Comparator.comparing(Trade::getDateTime)).forEachOrdered(trade -> {
			switch (trade.getType()) {
			case OPEN:
				opens.compute(trade.getSymbol(), (k, v) -> {
					if (v == null) v = new ArrayList<>();
					TrackedTrade<T> open = new TrackedTrade<>();
					open.trade = trade;
					open.amountCovered = 0;
					v.add(open);
					return v;
				});
				break;
			case CLOSE:
				closes.add(trade);
				break;
			default:
				throw new IllegalStateException();
			}
		});

		List<Cover<T>> covers = new ArrayList<>();

		for (T tradeClose : closes) {
			int covered = 0;
			int toCover = -tradeClose.getQuantity();
			String symbol = tradeClose.getSymbol();

			var cover = new Cover<T>();
			List<Cover<T>.Open> tradeOpens = new ArrayList<>();

			while (covered < toCover) {
				var open = opens.get(symbol).stream().filter(tt -> tt.amountCovered < tt.trade.getQuantity()).findFirst().get();
				var coverAmount = Math.min(toCover - covered, open.trade.getQuantity() - open.amountCovered);

				open.amountCovered += coverAmount;
				covered += coverAmount;

				var tradeOpen = cover.new Open();
				tradeOpen.setTrade(open.trade);
				tradeOpen.setAmountCovered(coverAmount);
				tradeOpens.add(tradeOpen);
			}

			cover.setOpens(tradeOpens);
			cover.setClose(tradeClose);
			covers.add(cover);
		}

		return covers;
	}

	private static class TrackedTrade<T extends Trade> {
		private T trade;
		private int amountCovered;
	}

}
