package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Trade;

public class TaxReportPrinter {

	private final List<Cover> covers;
	private final Currency baseCurrency;
	private final CurrencyConverter currencyConverter;

	public TaxReportPrinter(List<Cover> covers, Currency baseCurrency, CurrencyConverter currencyConverter) {
		this.covers = covers;
		this.baseCurrency = baseCurrency;
		this.currencyConverter = currencyConverter;
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
		var totalRacInBase = new RevenueAndCost();
		var totalRacPerCurrency = new TreeMap<Currency, RevenueAndCost>();

		for (var cover : covers) {
			var tradeRacInBase = new RevenueAndCost();
			var tradeRacInOriginal = new RevenueAndCost();

			var closeInOriginal = cover.close();
			var closeInBase = tradeInBase(closeInOriginal);

			ps.println(closeInOriginal.getSymbol());

			for (var openInOriginal : cover.opens()) {
				if (openInOriginal.getCurrency() != closeInOriginal.getCurrency()) {
					throw new IllegalStateException(String.format("Open trade currency (%s) does not match close trade currency (%s)",
						openInOriginal.getCurrency(), closeInOriginal.getCurrency()));
				}

				tradeRacInOriginal.addCost(openInOriginal.getProceeds());
				tradeRacInOriginal.addCost(openInOriginal.getFees());

				var openInBase = tradeInBase(openInOriginal);
				tradeRacInBase.addCost(openInBase.getProceeds());
				tradeRacInBase.addCost(openInBase.getFees());

				new TradePrinter(openInOriginal, baseCurrency, currencyConverter).print(ps);
			}

			tradeRacInOriginal.addRevenue(closeInOriginal.getProceeds());
			tradeRacInOriginal.addCost(closeInOriginal.getFees());

			tradeRacInBase.addRevenue(closeInBase.getProceeds());
			tradeRacInBase.addCost(closeInBase.getFees());

			new TradePrinter(closeInOriginal, baseCurrency, currencyConverter).print(ps);

			var originalCurrency = closeInOriginal.getCurrency();
			ps.println(String.format(Locale.ROOT, "P&L: %.2f%s %.2f%s",
				tradeRacInOriginal.profitLoss(), originalCurrency,
				tradeRacInBase.profitLoss(), baseCurrency));
			ps.println("----------------------------------------------------------------------------------------------------");

			totalRacPerCurrency.merge(originalCurrency, tradeRacInOriginal, (a, b) -> {
				a.addRevenueAndCost(b);
				return a;
			});

			totalRacInBase.addRevenueAndCost(tradeRacInBase);
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
			totalRacInBase.cost, baseCurrency,
			totalRacInBase.revenue, baseCurrency,
			totalRacInBase.profitLoss(), baseCurrency));
	}

	private Trade tradeInBase(Trade trade) {
		if (trade.getCurrency() == baseCurrency) {
			return trade;
		}
		return currencyConverter.convert(trade, baseCurrency);
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
