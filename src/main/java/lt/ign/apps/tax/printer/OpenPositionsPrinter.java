package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Trade;

public class OpenPositionsPrinter {

	private final List<Trade> uncovered;
	private final Currency baseCurrency;
	private final CurrencyConverter currencyConverter;

	public OpenPositionsPrinter(List<Trade> uncovered, Currency baseCurrency, CurrencyConverter currencyConverter) {
		this.uncovered = uncovered;
		this.baseCurrency = baseCurrency;
		this.currencyConverter = currencyConverter;
	}

	public void print(PrintStream ps) {
		var totalInBase = new Totals();
		var totalPerCurrency = new TreeMap<Currency, Totals>();

		var opensPerSymbol = uncovered.stream().collect(Collectors.groupingBy(Trade::getSymbol));
		ps.println("====================================================================================================");
		ps.println("Open positions");
		ps.println("----------------------------------------------------------------------------------------------------");

		for (var symbolOpens : opensPerSymbol.entrySet()) {
			var symbol = symbolOpens.getKey();

			ps.println(symbol);

			var positionInBase = new Totals();
			var positionInOriginal = new Totals();

			Currency originalCurrency = null;

			for (var openInOriginal : symbolOpens.getValue()) {
				if (originalCurrency == null) {
					originalCurrency = openInOriginal.getCurrency();
				} else if (originalCurrency != openInOriginal.getCurrency()) {
					throw new IllegalStateException(String.format("Position currency changed (%s -> %s)", originalCurrency,
						openInOriginal.getCurrency()));
				}

				positionInOriginal.add(openInOriginal);

				var openInBase = currencyConverter.convert(openInOriginal, baseCurrency);
				positionInBase.add(openInBase);

				new TradePrinter(openInOriginal, baseCurrency, currencyConverter).print(ps);
			}

			ps.print(String.format(Locale.ROOT, "%s POSITION: %.2f%s + %.2f%s = %.2f%s",
				symbol,
				positionInOriginal.proceeds, originalCurrency,
				positionInOriginal.fees, originalCurrency,
				positionInOriginal.total(), originalCurrency));
			if (baseCurrency != originalCurrency) {
				ps.print(String.format(Locale.ROOT, " | %.2f%s + %.2f%s = %.2f%s",
					positionInBase.proceeds, baseCurrency,
					positionInBase.fees, baseCurrency,
					positionInBase.total(), baseCurrency));
			}
			ps.println();
			ps.println("----------------------------------------------------------------------------------------------------");

			totalInBase.add(positionInBase);
			totalPerCurrency.merge(originalCurrency, positionInOriginal, Totals::sum);
		}

		totalPerCurrency.entrySet().forEach(e -> {
			var currency = e.getKey();
			var pf = e.getValue();
			ps.println(String.format(Locale.ROOT, "%s. Proceeds: %.2f%s; Fees: %.2f%s; Total: %.2f%s",
				currency,
				pf.proceeds, currency,
				pf.fees, currency,
				pf.total(), currency));
		});

		ps.println(String.format(Locale.ROOT, "TOTAL. Proceeds: %.2f%s; Fees: %.2f%s; Total: %.2f%s",
			totalInBase.proceeds, baseCurrency,
			totalInBase.fees, baseCurrency,
			totalInBase.total(), baseCurrency));

		ps.println("====================================================================================================");
	}

	private static class Totals {
		private BigDecimal proceeds;
		private BigDecimal fees; // negatively denominated

		private Totals() {
			proceeds = BigDecimal.ZERO;
			fees = BigDecimal.ZERO;
		}

		private void add(Totals totals) {
			proceeds = proceeds.add(totals.proceeds);
			fees = fees.add(totals.fees);
		}

		private void add(Trade trade) {
			proceeds = proceeds.add(trade.getProceeds());
			fees = fees.add(trade.getFees());
		}

		private BigDecimal total() {
			return proceeds.add(fees);
		}

		private static Totals sum(Totals a, Totals b) {
			var totals = new Totals();
			totals.proceeds = a.proceeds.add(b.proceeds);
			totals.fees = a.fees.add(b.fees);
			return totals;
		}
	}

}
