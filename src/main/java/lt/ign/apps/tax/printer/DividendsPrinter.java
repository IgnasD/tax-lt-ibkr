package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.TaxedDividends;
import lt.ign.apps.tax.model.event.DividendEvent;

public class DividendsPrinter {

	private final List<TaxedDividends> taxedDividends;
	private final Currency baseCurrency;
	private final CurrencyConverter currencyConverter;

	public DividendsPrinter(List<TaxedDividends> taxedDividends, Currency baseCurrency, CurrencyConverter currencyConverter) {
		this.taxedDividends = taxedDividends;
		this.baseCurrency = baseCurrency;
		this.currencyConverter = currencyConverter;
	}

	public void print(PrintStream ps) {
		var yearly = taxedDividends.stream().collect(Collectors.groupingBy(d -> d.getDateTime().getYear()));
		for (var entry : yearly.entrySet()) {
			ps.println("====================================================================================================");
			ps.println("Dividends: " + entry.getKey());
			ps.println("----------------------------------------------------------------------------------------------------");
			entry.getValue().stream().sorted(Comparator.comparing(DividendEvent::getSymbol).thenComparing(DividendEvent::getDateTime))
				.forEach(d -> {
					ps.print(String.format(Locale.ROOT, "%s %s %.2f%s + %.2f%s = %.2f%s",
						d.getDateTime().toLocalDate(), d.getSymbol(),
						d.getAmount(), d.getCurrency(),
						d.getWithholdingTax(), d.getCurrency(),
						d.getAmount().add(d.getWithholdingTax()), d.getCurrency()));
					if (d.getCurrency() != baseCurrency) {
						var exchangeRate = currencyConverter.getExchangeRate(d, baseCurrency);
						var inBase = d.convertCurrency(exchangeRate);
						ps.print(String.format(Locale.ROOT, " | %.2f%s + %.2f%s = %.2f%s (rate: %s)",
							inBase.getAmount(), inBase.getCurrency(),
							inBase.getWithholdingTax(), inBase.getCurrency(),
							inBase.getAmount().add(d.getWithholdingTax()), inBase.getCurrency(),
							exchangeRate.toString()));
					}
					ps.println();
				});
			ps.println("====================================================================================================");
		}
	}

}
