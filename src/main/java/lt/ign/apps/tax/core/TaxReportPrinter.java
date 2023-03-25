package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Trade;
//import lt.ign.apps.tax.model.event.TradeEur;

public class TaxReportPrinter {

	/*public void print(List<Cover<TradeEur>> covers) {
		Map<Integer, List<Cover<TradeEur>>> yearlyCovers = covers.stream()
				.collect(Collectors.groupingBy(c -> c.getClose().getDateTime().getYear()));
		for (var entry : yearlyCovers.entrySet()) {
			System.out.println("====================================================================================================");
			System.out.println("Tax year: " + entry.getKey());
			System.out.println("----------------------------------------------------------------------------------------------------");
			printInternal(entry.getValue());
			System.out.println("====================================================================================================");
		}
	}

	private void printInternal(List<Cover<TradeEur>> covers) {
		var totalRacPerCurrency = new HashMap<Currency, RevenueAndCost>();
		var totalRacEur = new RevenueAndCost();

		for (var cover : covers) {
			var tradeRacNative = new RevenueAndCost();
			var tradeRacEur = new RevenueAndCost();

			var close = cover.getClose();

			System.out.println(close.getSymbol());

			for (var open : cover.getOpens()) {
				var propProceeds = open.proportional(Trade::getProceeds);
				tradeRacNative.addCost(propProceeds);
				var propFees = open.proportional(Trade::getFees);
				tradeRacNative.addCost(propFees);

				var propProceedsEur = open.proportional(TradeEur::getProceedsEur);
				tradeRacEur.addCost(propProceedsEur);
				var propFeesEur = open.proportional(TradeEur::getFeesEur);
				tradeRacEur.addCost(propFeesEur);

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

			tradeRacNative.addRevenue(close.getProceeds());
			tradeRacNative.addCost(close.getFees());

			tradeRacEur.addRevenue(close.getProceedsEur());
			tradeRacEur.addCost(close.getFeesEur());

			var currency = close.getCurrency();
			totalRacPerCurrency.merge(currency, tradeRacNative, (a, b) -> {
				a.addRevenueAndCost(b);
				return a;
			});

			totalRacEur.addRevenueAndCost(tradeRacEur);

			System.out.println(close);

			System.out.println(String.format(Locale.ROOT, "P&L: %.2f%s %.2fEUR",
					tradeRacNative.profitLoss(), currency, tradeRacEur.profitLoss()));
			System.out.println("----------------------------------------------------------------------------------------------------");
		}

		totalRacPerCurrency.entrySet().forEach(e -> {
			var currency = e.getKey();
			var rac = e.getValue();
			System.out.println(String.format(Locale.ROOT, "%s. Cost: %.2f%s; Revenue: %.2f%s; P&L: %.2f%s", currency, rac.cost, currency,
					rac.revenue, currency, rac.profitLoss(), currency));
		});
		System.out.println(String.format(Locale.ROOT, "TOTAL. Cost: %.2fEUR; Revenue: %.2fEUR; P&L: %.2fEUR", totalRacEur.cost,
				totalRacEur.revenue, totalRacEur.profitLoss()));
	}

	private static class RevenueAndCost {
		private BigDecimal revenue = BigDecimal.ZERO;
		private BigDecimal cost = BigDecimal.ZERO; // negatively denominated

		private void addRevenue(BigDecimal revenue) {
			this.revenue = this.revenue.add(revenue);
		}

		private void addCost(BigDecimal cost) {
			this.cost = this.cost.add(cost);
		}

		private void addRevenueAndCost(RevenueAndCost rac) {
			addRevenue(rac.revenue);
			addCost(rac.cost);
		}

		private BigDecimal profitLoss() {
			return revenue.add(cost);
		}
	}*/

}
