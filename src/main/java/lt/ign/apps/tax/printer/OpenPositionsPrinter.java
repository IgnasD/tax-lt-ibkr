package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.TradeCurrencyView;
import lt.ign.apps.tax.model.event.Trade;

public class OpenPositionsPrinter {

	private final List<Trade> uncovered;
	private final Currency baseCurrency;

	public OpenPositionsPrinter(List<Trade> uncovered, Currency baseCurrency) {
		this.uncovered = uncovered;
		this.baseCurrency = baseCurrency;
	}

	public void print(PrintStream ps) {
		var totalInBaseCurrency = new ProceedsAndFees();
		var totalPerCurrency = new TreeMap<Currency, ProceedsAndFees>();

		var opensPerSymbol = uncovered.stream().collect(Collectors.groupingBy(t -> t.getSymbol()));
		ps.println("====================================================================================================");
		ps.println("Open positions");
		ps.println("----------------------------------------------------------------------------------------------------");

		for (var symbolOpens : opensPerSymbol.entrySet()) {
			var symbol = symbolOpens.getKey();

			ps.println(symbol);

			var positionInBaseCurrency = new ProceedsAndFees();
			var positionInOriginalCurrency = new ProceedsAndFees();

			Currency originalCurrency = null;

			for (var open : symbolOpens.getValue()) {
				if (open.getCurrency() != baseCurrency) {
					throw new IllegalStateException(String.format("Expected base currency (%s) does not match open trade currency (%s)",
						baseCurrency, open.getCurrency()));
				}

				positionInBaseCurrency.addProceeds(open.getProceeds());
				positionInBaseCurrency.addFees(open.getFees());

				var openInOriginalCurrency = new TradeCurrencyView(open).getInOriginalCurrency();
				positionInOriginalCurrency.addProceeds(openInOriginalCurrency.getProceeds());
				positionInOriginalCurrency.addFees(openInOriginalCurrency.getFees());

				if (originalCurrency == null) {
					originalCurrency = openInOriginalCurrency.getCurrency();
				} else if (originalCurrency != openInOriginalCurrency.getCurrency()) {
					throw new IllegalStateException(String.format("Position currency changed (%s -> %s)", originalCurrency,
						openInOriginalCurrency.getCurrency()));
				}

				new TradePrinter(open).print(ps);
			}

			ps.print(String.format(Locale.ROOT, "%s POSITION: %.2f%s + %.2f%s = %.2f%s",
				symbol,
				positionInOriginalCurrency.proceeds, originalCurrency,
				positionInOriginalCurrency.fees, originalCurrency,
				positionInOriginalCurrency.total(), originalCurrency));
			if (baseCurrency != originalCurrency) {
				ps.print(String.format(Locale.ROOT, " | %.2f%s + %.2f%s = %.2f%s",
					positionInBaseCurrency.proceeds, baseCurrency,
					positionInBaseCurrency.fees, baseCurrency,
					positionInBaseCurrency.total(), baseCurrency));
			}
			ps.println();
			ps.println("----------------------------------------------------------------------------------------------------");

			totalInBaseCurrency.add(positionInBaseCurrency);
			totalPerCurrency.merge(originalCurrency, positionInOriginalCurrency, (a, b) -> {
				a.add(b);
				return a;
			});
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
			totalInBaseCurrency.proceeds, baseCurrency,
			totalInBaseCurrency.fees, baseCurrency,
			totalInBaseCurrency.total(), baseCurrency));

		ps.println("====================================================================================================");
	}

	private static class ProceedsAndFees {
		private BigDecimal proceeds = BigDecimal.ZERO;
		private BigDecimal fees = BigDecimal.ZERO;

		private void addProceeds(BigDecimal revenue) {
			this.proceeds = this.proceeds.add(revenue);
		}

		private void addFees(BigDecimal cost) {
			this.fees = this.fees.add(cost);
		}

		private void add(ProceedsAndFees other) {
			addProceeds(other.proceeds);
			addFees(other.fees);
		}

		private BigDecimal total() {
			return this.proceeds.add(fees);
		}
	}

}
