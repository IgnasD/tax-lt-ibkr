package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.TaxedInterest;

public class InterestPrinter {

	private final List<TaxedInterest> taxedInterest;
	private final Currency baseCurrency;
	private final CurrencyConverter currencyConverter;

	public InterestPrinter(List<TaxedInterest> taxedInterest, Currency baseCurrency, CurrencyConverter currencyConverter) {
		this.taxedInterest = taxedInterest;
		this.baseCurrency = baseCurrency;
		this.currencyConverter = currencyConverter;
	}

	public void print(PrintStream ps) {
		var yearly = taxedInterest.stream().collect(Collectors.groupingBy(d -> d.getDate().getYear()));
		for (var entry : yearly.entrySet()) {
			var totalsInBase = new Totals();
			var totalsPerCurrency = new TreeMap<Currency, Totals>();

			ps.println("====================================================================================================");
			ps.println("Interest: " + entry.getKey());
			ps.println("----------------------------------------------------------------------------------------------------");
			entry.getValue().stream().sorted(Comparator.comparing(TaxedInterest::getDate)).forEach(inOriginal -> {
				var originalCurrency = inOriginal.getCurrency();

				totalsPerCurrency.merge(originalCurrency, new Totals(inOriginal), Totals::sum);

				ps.print(String.format(Locale.ROOT, "%s %.2f%s + %.2f%s = %.2f%s",
					inOriginal.getDate(),
					inOriginal.getAmount(), originalCurrency,
					inOriginal.getWithholdingTax(), originalCurrency,
					inOriginal.getAmount().add(inOriginal.getWithholdingTax()), originalCurrency));

				if (inOriginal.getCurrency() == baseCurrency) {
					totalsInBase.add(inOriginal);
				} else {
					var exchangeRate = currencyConverter.getExchangeRate(inOriginal, baseCurrency);
					var inBase = inOriginal.convertCurrency(exchangeRate);

					totalsInBase.add(inBase);

					ps.print(String.format(Locale.ROOT, " | %.2f%s + %.2f%s = %.2f%s (rate: %s)",
						inBase.getAmount(), inBase.getCurrency(),
						inBase.getWithholdingTax(), baseCurrency,
						inBase.getAmount().add(inOriginal.getWithholdingTax()), baseCurrency,
						exchangeRate.toString()));
				}

				ps.println();
			});

			ps.println("----------------------------------------------------------------------------------------------------");

			totalsPerCurrency.entrySet().forEach(e -> {
				var currency = e.getKey();
				var totals = e.getValue();
				ps.println(String.format(Locale.ROOT, "%s. Amount: %.2f%s; Withholding tax: %.2f%s; Net: %.2f%s",
					currency,
					totals.amount, currency,
					totals.withholdingTax, currency,
					totals.net(), currency));
			});

			ps.println(String.format(Locale.ROOT, "TOTAL. Amount: %.2f%s; Withholding tax: %.2f%s; Net: %.2f%s",
				totalsInBase.amount, baseCurrency,
				totalsInBase.withholdingTax, baseCurrency,
				totalsInBase.net(), baseCurrency));

			ps.println("====================================================================================================");
		}
	}

	private static class Totals {
		private BigDecimal amount;
		private BigDecimal withholdingTax; // negatively denominated

		private Totals() {
			amount = BigDecimal.ZERO;
			withholdingTax = BigDecimal.ZERO;
		}

		private Totals(TaxedInterest interest) {
			amount = interest.getAmount();
			withholdingTax = interest.getWithholdingTax();
		}

		private void add(TaxedInterest interest) {
			amount = amount.add(interest.getAmount());
			withholdingTax = withholdingTax.add(interest.getWithholdingTax());
		}

		private BigDecimal net() {
			return amount.add(withholdingTax);
		}

		private static Totals sum(Totals a, Totals b) {
			var totals = new Totals();
			totals.amount = a.amount.add(b.amount);
			totals.withholdingTax = a.withholdingTax.add(b.withholdingTax);
			return totals;
		}
	}

}
