package lt.ign.apps.tax;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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

			for (var open : cover.getOpens()) {
				var openPl = open.proportional(Trade::getProceeds);
				openPl = openPl.add(open.proportional(Trade::getFees));
				pl = pl.add(openPl);

				var openPlEur = open.proportional(TradeEur::getProceedsEur);
				openPlEur = openPlEur.add(open.proportional(TradeEur::getFeesEur));
				plEur = plEur.add(openPlEur);

				System.out.println(open.getTrade());
			}

			var close = cover.getClose();

			var closePl = close.getProceeds().add(close.getFees());
			pl = pl.add(closePl);

			var closePlEur = close.getProceedsEur().add(close.getFeesEur());
			plEur = plEur.add(closePlEur);

			var currency = close.getCurrency();
			totalPlPerCurrency.merge(currency, pl, (a, b) -> a.add(b));

			totalPlEur = totalPlEur.add(plEur);

			System.out.println(close);

			System.out.println(String.format("P&L: %.2f%s %.2fEUR", pl, currency, plEur));
			System.out.println("--------------------------------------------------------");
		}

		totalPlPerCurrency.entrySet().forEach(e -> System.out.println(String.format("P&L: %.2f%s", e.getValue(), e.getKey())));
		System.out.println(String.format("TOTAL P&L: %.2fEUR", totalPlEur));
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
