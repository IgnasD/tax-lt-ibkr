package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.ArrayList;
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

				var openView = new TradeCurrencyView(open);
				tradeRacOriginal.addCost(openView.original.getProceeds());
				tradeRacOriginal.addCost(openView.original.getFees());

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

			var closeView = new TradeCurrencyView(close);
			tradeRacOriginal.addRevenue(closeView.original.getProceeds());
			tradeRacOriginal.addCost(closeView.original.getFees());

			print(closeView);

			var originalCurrency = closeView.original.getCurrency();
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

	private static void print(TradeCurrencyView tradeView) {
		if (tradeView.original == tradeView.base) {
			System.out.print(tradeView.base);
		} else {
			System.out.println(String.format(Locale.ROOT, "%s | %.2f%s %.2f%s (rate: %.4f %s/%s)", tradeView.original,
				tradeView.base.getProceeds(), tradeView.base.getCurrency(), tradeView.base.getFees(), tradeView.base.getCurrency(),
				tradeView.rate, tradeView.original.getCurrency(), tradeView.base.getCurrency()));
		}
	}

	private static class TradeCurrencyView {
		private final Trade original;
		private final Trade base;
		private final BigDecimal rate;

		private TradeCurrencyView(Trade base) {
			var original = base;
			BigDecimal rate = null;
			if (base instanceof ModifiedTrade modded) {
				var originalCurrency = modded.getOriginal().getCurrency();
				var baseCurrency = modded.getCurrency();
				if (originalCurrency != baseCurrency) {
					var mods = new ArrayList<>(modded.getModifications());
					CurrencyConversion conversion = null;
					for (var it = mods.iterator(); it.hasNext();) {
						var mod = it.next();
						if (mod instanceof CurrencyConversion cc) {
							if (cc.getSourceCurrency() == originalCurrency && cc.getTargetCurrency() == baseCurrency) {
								conversion = cc;
								it.remove();
								break;
							}
						}
					}
					if (conversion == null) {
						throw new IllegalStateException(
							String.format("Could not find %s to %s currency conversion", originalCurrency, baseCurrency));
					}
					original = ModifiedTrade.create(modded.getOriginal(), mods);
					rate = conversion.getConversionRate();
				}
			}

			this.base = base;
			this.original = original;
			this.rate = rate;
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
