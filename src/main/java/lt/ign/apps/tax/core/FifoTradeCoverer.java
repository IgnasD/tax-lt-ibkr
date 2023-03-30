package lt.ign.apps.tax.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.event.Event;
import lt.ign.apps.tax.model.event.Split;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.PartialQuantity;
import lt.ign.apps.tax.mods.StockSplit;

public class FifoTradeCoverer {

	private final String symbol;

	public FifoTradeCoverer(String symbol) {
		this.symbol = symbol;
	}

	public List<Cover> cover(List<Event> events) {
		var covers = new ArrayList<Cover>();

		var eventQueue = events.stream().sorted(Comparator.comparing(Event::getDateTime)).collect(Collectors.toCollection(ArrayDeque::new));
		var uncoveredOpens = new ArrayDeque<Trade>();

		while (!eventQueue.isEmpty()) {
			var event = eventQueue.poll();
			if (!event.getSymbol().equals(symbol)) {
				throw new IllegalArgumentException("Found unexpected symbol: " + event.getSymbol());
			}
			if (event instanceof Split split) {
				var splitMod = new StockSplit(split.getDateTime(), split.getMultiplier());
				uncoveredOpens = uncoveredOpens.stream().map(trade -> trade.modify(splitMod))
					.collect(Collectors.toCollection(ArrayDeque::new));
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
			} else {
				throw new UnsupportedOperationException("Unknown event type " + event.getClass().getSimpleName());
			}
		}

		return covers;
	}

}
