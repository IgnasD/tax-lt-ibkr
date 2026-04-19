package lt.ign.apps.tax;

import java.nio.file.Paths;
import java.util.stream.Stream;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.core.DividendTaxPairer;
import lt.ign.apps.tax.core.FifoTradeCoverer;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.DepositWithdrawal;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;
import lt.ign.apps.tax.printer.DepositsWithdrawalsPrinter;
import lt.ign.apps.tax.printer.DividendsPrinter;
import lt.ign.apps.tax.printer.OpenPositionsPrinter;
import lt.ign.apps.tax.printer.TaxReportPrinter;

public class App {

	public static void main(String[] args) {
		var humanReadableOut = System.out;

		var csvFiles = Stream.of(args).map(Paths::get).toList();

		var baseCurrency = Currency.EUR;
		var usdEurRates = EcbXmlParser.forCurrency(Currency.USD).parseRates();
		var currencyConverter = new CurrencyConverter(usdEurRates);

		var entries = IbkrCsvParser.parse(csvFiles);

		var positions = new FifoTradeCoverer().cover(entries);

		var covers = positions.stream()
			.flatMap(r -> r.covers().stream())
			.toList();

		var uncovered = positions.stream()
			.flatMap(r -> r.uncovered().stream())
			.toList();

		new TaxReportPrinter(covers, baseCurrency, currencyConverter).print(humanReadableOut);
		new OpenPositionsPrinter(uncovered, baseCurrency, currencyConverter).print(humanReadableOut);

		// -

		var depositsWithdrawals = entries.stream()
			.filter(e -> e instanceof DepositWithdrawal)
			.map(e -> (DepositWithdrawal) e)
			.toList();

		new DepositsWithdrawalsPrinter(depositsWithdrawals).print(humanReadableOut);

		// -

		var taxedDividends = new DividendTaxPairer().pair(entries);

		new DividendsPrinter(taxedDividends, baseCurrency, currencyConverter).print(humanReadableOut);
	}

}
