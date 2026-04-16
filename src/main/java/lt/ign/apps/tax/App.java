package lt.ign.apps.tax;

import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.core.FifoTradeCoverer;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.DepositWithdrawal;
import lt.ign.apps.tax.model.event.StockEvent;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;
import lt.ign.apps.tax.printer.DepositsWithdrawalsPrinter;
import lt.ign.apps.tax.printer.OpenPositionsPrinter;
import lt.ign.apps.tax.printer.TaxReportPrinter;

public class App {

	public static void main(String[] args) {
		var csvFiles = Stream.of(args).map(Paths::get).toList();

		var usdEurRates = EcbXmlParser.forCurrency(Currency.USD).parseRates();
		var currencyConverter = new CurrencyConverter(usdEurRates);

		var entries = IbkrCsvParser.parse(csvFiles);

		var covererResults = entries.stream()
			.filter(e -> e instanceof StockEvent)
			.map(e -> (StockEvent) e)
			.collect(Collectors.groupingBy(StockEvent::getSymbol)).entrySet().stream()
			.map(entry -> new FifoTradeCoverer(entry.getKey()).cover(entry.getValue()))
			.toList();

		var covers = covererResults.stream()
			.flatMap(r -> r.covers().stream())
			.toList();

		var uncovered = covererResults.stream()
			.flatMap(r -> r.uncovered().stream())
			.toList();

		var depositsWithdrawals = entries.stream()
			.filter(e -> e instanceof DepositWithdrawal)
			.map(e -> (DepositWithdrawal) e)
			.toList();

		new TaxReportPrinter(covers, Currency.EUR, currencyConverter).print(System.out);
		new OpenPositionsPrinter(uncovered, Currency.EUR, currencyConverter).print(System.out);
		new DepositsWithdrawalsPrinter(depositsWithdrawals).print(System.out);
	}

}
