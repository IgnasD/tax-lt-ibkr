package lt.ign.apps.tax.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Position;
import lt.ign.apps.tax.model.event.DividendEvent;
import lt.ign.apps.tax.model.event.ReportEntry;
import lt.ign.apps.tax.model.event.Split;
import lt.ign.apps.tax.model.event.StockEvent;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.PartialQuantity;
import lt.ign.apps.tax.mods.StockSplit;

public class FifoTradeCoverer {

	public List<Position> cover(List<ReportEntry> entries) {
		return entries.stream()
			.filter(e -> e instanceof StockEvent)
			.map(e -> (StockEvent) e)
			.collect(Collectors.groupingBy(StockEvent::getSymbol)).entrySet().stream()
			.map(entry -> coverSymbol(entry.getKey(), entry.getValue()))
			.toList();
	}

	private Position coverSymbol(String symbol, List<StockEvent> events) {
		var covers = new ArrayList<Cover>();
		var uncoveredOpens = new ArrayDeque<Trade>();

		events.stream().sorted(Comparator.comparing(StockEvent::getDateTime)).forEach(event -> {
			if (!event.getSymbol().equals(symbol)) {
				throw new IllegalArgumentException("Found unexpected symbol: " + event.getSymbol());
			}
			if (event instanceof Split split) {
				var splitMod = new StockSplit(split.getDateTime(), split.getMultiplier());
				for (int i = 0, l = uncoveredOpens.size(); i < l; i++) {
					var trade = uncoveredOpens.remove();
					trade = trade.modify(splitMod);
					uncoveredOpens.add(trade);
				}
			} else if (event instanceof Trade trade) {
				switch (trade.getType()) {
				case OPEN -> {
					uncoveredOpens.add(trade);
				}
				case CLOSE -> {
					var opens = new ArrayList<Trade>();
					int covered = 0;
					int toCover = -trade.getQuantity();

					while (covered < toCover) {
						var open = uncoveredOpens.remove();
						int openQuantity = open.getQuantity();

						int coverAmount = Math.min(toCover - covered, openQuantity);
						covered += coverAmount;

						if (coverAmount < openQuantity) {
							var tradeDateTime = trade.getDateTime();
							uncoveredOpens.addFirst(open.modify(new PartialQuantity(tradeDateTime, PartialQuantity.Type.CARRY,
								openQuantity - coverAmount)));
							opens.add(open.modify(new PartialQuantity(tradeDateTime, PartialQuantity.Type.COVER, coverAmount)));
						} else {
							opens.add(open);
						}
					}

					covers.add(new Cover(opens, trade));
				}
				default -> throw new UnsupportedOperationException("Unknown trade type " + trade.getType());
				}
			} else if (event instanceof DividendEvent) {
				// does not change position
			} else {
				throw new UnsupportedOperationException("Unknown event type " + event.getClass().getSimpleName());
			}
		});

		return new Position(covers, new ArrayList<>(uncoveredOpens));
	}

}
