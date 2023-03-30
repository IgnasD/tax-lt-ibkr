package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.ModifiedTrade;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.CurrencyConversion;

public class TaxReportPrinter {

	private final Currency baseCurrency;

	public TaxReportPrinter(Currency baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public void print(List<Cover> covers) {
		Map<Integer, List<Cover>> yearlyCovers = covers.stream()
			.collect(Collectors.groupingBy(c -> c.getClose().getDateTime().getYear()));
		for (var entry : yearlyCovers.entrySet()) {
			System.out.println("====================================================================================================");
			System.out.println("Tax year: " + entry.getKey());
			System.out.println("----------------------------------------------------------------------------------------------------");
			printInternal(entry.getValue().stream().sorted(Comparator.comparing(c -> c.getClose().getDateTime())).toList());
			System.out.println("====================================================================================================");
		}
	}

	private void printInternal(List<Cover> covers) {
		var totalRacBase = new RevenueAndCost();
		var totalRacPerCurrency = new HashMap<Currency, RevenueAndCost>();

		for (var cover : covers) {
			var tradeRacBase = new RevenueAndCost();
			var tradeRacOriginal = new RevenueAndCost();

			var close = cover.getClose();
			if (close.getCurrency() != baseCurrency) {
				throw new UnsupportedOperationException(String
					.format("Expected base currency (%s) does not match close trade currency (%s)", baseCurrency, close.getCurrency()));
			}

			System.out.println(close.getSymbol());

			for (var open : cover.getOpens()) {
				if (open.getCurrency() != baseCurrency) {
					throw new UnsupportedOperationException(String
						.format("Expected base currency (%s) does not match open trade currency (%s)", baseCurrency, open.getCurrency()));
				}

				tradeRacBase.addCost(open.getProceeds());
				tradeRacBase.addCost(open.getFees());

				var openView = new TradeView(open);
				tradeRacOriginal.addCost(openView.inOriginalCurrency.getProceeds());
				tradeRacOriginal.addCost(openView.inOriginalCurrency.getFees());

				//var trade = open.getTrade();
				//if (open.isFullyCovered()) {
				print(openView);
				//} else {
				//	System.out.println(String.format("[%s]", trade));
				//	var covered = open.getAmountCovered();
				//	var total = trade.getQuantity();
				//	var currency = trade.getCurrency();
				//	System.out.println(String.format(Locale.ROOT, ">>> %d/%d %.2f%s %.2f%s | %.2fEUR %.2fEUR", covered, total, proceeds,
				//			currency, fees, currency, propProceedsEur, propFeesEur));
				//}
			}

			tradeRacBase.addRevenue(close.getProceeds());
			tradeRacBase.addCost(close.getFees());

			var closeView = new TradeView(close);
			tradeRacOriginal.addRevenue(closeView.inOriginalCurrency.getProceeds());
			tradeRacOriginal.addCost(closeView.inOriginalCurrency.getFees());

			print(closeView);

			var originalCurrency = closeView.inOriginalCurrency.getCurrency();
			System.out.println(String.format(Locale.ROOT, "P&L: %.2f%s %.2f%s", tradeRacOriginal.profitLoss(), originalCurrency,
				tradeRacBase.profitLoss(), baseCurrency));
			System.out.println("----------------------------------------------------------------------------------------------------");

			totalRacPerCurrency.merge(originalCurrency, tradeRacOriginal, (a, b) -> {
				a.addRevenueAndCost(b);
				return a;
			});

			totalRacBase.addRevenueAndCost(tradeRacBase);
		}

		totalRacPerCurrency.entrySet().forEach(e -> {
			var currency = e.getKey();
			var rac = e.getValue();
			System.out.println(String.format(Locale.ROOT, "%s. Cost: %.2f%s; Revenue: %.2f%s; P&L: %.2f%s", currency, rac.cost, currency,
				rac.revenue, currency, rac.profitLoss(), currency));
		});

		System.out.println(String.format(Locale.ROOT, "TOTAL. Cost: %.2fEUR; Revenue: %.2fEUR; P&L: %.2fEUR", totalRacBase.cost,
			totalRacBase.revenue, totalRacBase.profitLoss()));
	}

	private static void print(TradeView tradeView) {
		if (tradeView.inOriginalCurrency == tradeView.trade) {
			System.out.print(tradeView.trade);
		} else {
			System.out.println(String.format(Locale.ROOT, "%s | %.2f%s %.2f%s (rate: %s)", tradeView.inOriginalCurrency,
				tradeView.trade.getProceeds(), tradeView.trade.getCurrency(), tradeView.trade.getFees(), tradeView.trade.getCurrency(),
				tradeView.currencyConversion));
		}
	}

	private static class TradeView {
		private final Trade trade;
		private final Trade inOriginalCurrency;
		private final CurrencyConversion currencyConversion;

		private TradeView(Trade trade) {
			CurrencyConversion currencyConversion = null;

			var original = trade;
			if (trade instanceof ModifiedTrade modded) {
				var currencyConversionPartitioned = modded.getModifications().stream()
					.collect(Collectors.partitioningBy(mod -> mod instanceof CurrencyConversion));

				var currencyConversions = currencyConversionPartitioned.get(true);
				if (currencyConversions.size() > 1) {
					throw new UnsupportedOperationException(
						"Expected single currency conversion per position, got: " + currencyConversions.size());
				}
				if (currencyConversions.size() == 1) {
					currencyConversion = (CurrencyConversion) currencyConversions.get(0);
					var otherMods = currencyConversionPartitioned.get(false);
					original = ModifiedTrade.create(modded.getOriginal(), otherMods);
				}
			}

			this.trade = trade;
			this.inOriginalCurrency = original;
			this.currencyConversion = currencyConversion;
		}
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
	}

}
