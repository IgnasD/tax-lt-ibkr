package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.Trade;
import lt.ign.apps.tax.model.TradeEur;

public class TaxReportPrinter {

	private void printInternal(List<Cover<TradeEur>> covers) {
		var totalPlPerCurrency = new HashMap<Currency, BigDecimal>();
		var totalPlEur = BigDecimal.ZERO;

		for (var cover : covers) {
			var pl = BigDecimal.ZERO;
			var plEur = BigDecimal.ZERO;

			var close = cover.getClose();

			System.out.println(close.getSymbol());

			for (var open : cover.getOpens()) {
				var propProceeds = open.proportional(Trade::getProceeds);
				var propFees = open.proportional(Trade::getFees);
				pl = pl.add(propProceeds.add(propFees));

				var propProceedsEur = open.proportional(TradeEur::getProceedsEur);
				var propFeesEur = open.proportional(TradeEur::getFeesEur);
				plEur = plEur.add(propProceedsEur.add(propFeesEur));

				var trade = open.getTrade();
				if (open.isFullyCovered()) {
					System.out.println(trade);
				} else {
					System.out.println(String.format("[%s]", trade));
					var covered = open.getAmountCovered();
					var total = trade.getQuantity();
					var currency = trade.getCurrency();
					System.out.println(String.format(Locale.ROOT, ">>> %d/%d %.2f%s %.2f%s | %.2fEUR %.2fEUR", covered, total, propProceeds,
							currency, propFees, currency, propProceedsEur, propFeesEur));
				}
			}

			var closePl = close.getProceeds().add(close.getFees());
			pl = pl.add(closePl);

			var closePlEur = close.getProceedsEur().add(close.getFeesEur());
			plEur = plEur.add(closePlEur);

			var currency = close.getCurrency();
			totalPlPerCurrency.merge(currency, pl, (a, b) -> a.add(b));

			totalPlEur = totalPlEur.add(plEur);

			System.out.println(close);

			System.out.println(String.format(Locale.ROOT, "P&L: %.2f%s %.2fEUR", pl, currency, plEur));
			System.out.println("--------------------------------------------------------");
		}

		totalPlPerCurrency.entrySet().forEach(e -> System.out.println(String.format(Locale.ROOT,
				"%s P&L: %.2f%s", e.getKey(), e.getValue(), e.getKey())));
		System.out.println(String.format(Locale.ROOT, "TOTAL P&L: %.2fEUR", totalPlEur));
	}

	public void print(List<Cover<TradeEur>> covers) {
		Map<Integer, List<Cover<TradeEur>>> yearlyCovers = covers.stream()
				.collect(Collectors.groupingBy(c -> c.getClose().getDateTime().getYear()));
		for (var entry : yearlyCovers.entrySet()) {
			System.out.println("========================================================");
			System.out.println("Tax year: " + entry.getKey());
			System.out.println("--------------------------------------------------------");
			printInternal(entry.getValue());
			System.out.println("========================================================");
		}
	}

}
