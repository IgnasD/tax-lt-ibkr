package lt.ign.apps.tax;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Trade;

public class FifoTradeCoverer {

	public List<Cover> cover(List<Trade> trades) {
		Map<String, List<Cover.Open>> opens = new HashMap<>();
		List<Trade> closes = new ArrayList<>();

		trades.stream().sorted(Comparator.comparing(Trade::getDateTime)).forEachOrdered(trade -> {
			switch (trade.getType()) {
			case OPEN:
				opens.compute(trade.getSymbol(), (k, v) -> {
					if (v == null) v = new ArrayList<>();
					var open = new Cover.Open();
					open.setTrade(trade);
					open.setAmountCovered(0);
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

		List<Cover> covers = new ArrayList<>();

		for (Trade tradeClose : closes) {
			int covered = 0;
			int toCover = -tradeClose.getQuantity();
			String symbol = tradeClose.getSymbol();

			List<Cover.Open> tradeOpens = new ArrayList<>();

			while (covered < toCover) {
				var open = opens.get(symbol).stream().filter(o -> o.getAmountCovered() < o.getTrade().getQuantity()).findFirst().get();
				var coverAmount = Math.min(toCover - covered, open.getTrade().getQuantity() - open.getAmountCovered());

				open.setAmountCovered(open.getAmountCovered() + coverAmount);
				covered += coverAmount;

				var tradeOpen = new Cover.Open();
				tradeOpen.setTrade(open.getTrade());
				tradeOpen.setAmountCovered(coverAmount);
				tradeOpens.add(tradeOpen);
			}

			var cover = new Cover();
			cover.setOpens(tradeOpens);
			cover.setClose(tradeClose);
			covers.add(cover);
		}

		return covers;
	}

}
