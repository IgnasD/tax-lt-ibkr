package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.TradeCurrencyView;

public class TaxReportPrinter {

	private final List<Cover> covers;
	private final Currency baseCurrency;

	public TaxReportPrinter(List<Cover> covers, Currency baseCurrency) {
		this.covers = covers;
		this.baseCurrency = baseCurrency;
	}

	public void print(PrintStream ps) {
		var yearlyCovers = covers.stream().collect(Collectors.groupingBy(c -> c.close().getDateTime().getYear()));
		for (var entry : yearlyCovers.entrySet()) {
			var yearCovers = entry.getValue().stream().sorted(Comparator.comparing(c -> c.close().getDateTime())).toList();
			ps.println("====================================================================================================");
			ps.println("Tax year: " + entry.getKey());
			ps.println("----------------------------------------------------------------------------------------------------");
			printInternal(yearCovers, ps);
			ps.println("====================================================================================================");
		}
	}

	private void printInternal(List<Cover> covers, PrintStream ps) {
		var totalRacBase = new RevenueAndCost();
		var totalRacPerCurrency = new TreeMap<Currency, RevenueAndCost>();

		for (var cover : covers) {
			var tradeRacBase = new RevenueAndCost();
			var tradeRacOriginal = new RevenueAndCost();

			var close = cover.close();
			if (close.getCurrency() != baseCurrency) {
				throw new UnsupportedOperationException(String
					.format("Expected base currency (%s) does not match close trade currency (%s)", baseCurrency, close.getCurrency()));
			}

			ps.println(close.getSymbol());

			for (var open : cover.opens()) {
				if (open.getCurrency() != baseCurrency) {
					throw new UnsupportedOperationException(String
						.format("Expected base currency (%s) does not match open trade currency (%s)", baseCurrency, open.getCurrency()));
				}

				tradeRacBase.addCost(open.getProceeds());
				tradeRacBase.addCost(open.getFees());

				var openInOriginalCurrency = new TradeCurrencyView(open).getInOriginalCurrency();
				tradeRacOriginal.addCost(openInOriginalCurrency.getProceeds());
				tradeRacOriginal.addCost(openInOriginalCurrency.getFees());

				new TradePrinter(open).print(ps);
			}

			tradeRacBase.addRevenue(close.getProceeds());
			tradeRacBase.addCost(close.getFees());

			var closeInOriginalCurrency = new TradeCurrencyView(close).getInOriginalCurrency();
			tradeRacOriginal.addRevenue(closeInOriginalCurrency.getProceeds());
			tradeRacOriginal.addCost(closeInOriginalCurrency.getFees());

			new TradePrinter(close).print(ps);

			var originalCurrency = closeInOriginalCurrency.getCurrency();
			ps.println(String.format(Locale.ROOT, "P&L: %.2f%s %.2f%s", tradeRacOriginal.profitLoss(), originalCurrency,
				tradeRacBase.profitLoss(), baseCurrency));
			ps.println("----------------------------------------------------------------------------------------------------");

			totalRacPerCurrency.merge(originalCurrency, tradeRacOriginal, (a, b) -> {
				a.addRevenueAndCost(b);
				return a;
			});

			totalRacBase.addRevenueAndCost(tradeRacBase);
		}

		totalRacPerCurrency.entrySet().forEach(e -> {
			var currency = e.getKey();
			var rac = e.getValue();
			ps.println(String.format(Locale.ROOT, "%s. Cost: %.2f%s; Revenue: %.2f%s; P&L: %.2f%s",
				currency,
				rac.cost, currency,
				rac.revenue, currency,
				rac.profitLoss(), currency));
		});

		ps.println(String.format(Locale.ROOT, "TOTAL. Cost: %.2f%s; Revenue: %.2f%s; P&L: %.2f%s",
			totalRacBase.cost, baseCurrency,
			totalRacBase.revenue, baseCurrency,
			totalRacBase.profitLoss(), baseCurrency));
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
