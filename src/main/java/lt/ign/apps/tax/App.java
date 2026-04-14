package lt.ign.apps.tax;

import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lt.ign.apps.tax.core.EurConverter;
import lt.ign.apps.tax.core.FifoTradeCoverer;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.StockEvent;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;
import lt.ign.apps.tax.printer.OpenPositionsPrinter;
import lt.ign.apps.tax.printer.TaxReportPrinter;

public class App {

	public static void main(String[] args) {
		var csvFiles = Stream.of(args).map(Paths::get).toList();

		var usdEurRates = EcbXmlParser.forCurrency(Currency.USD).parseRates();
		var eurConverter = new EurConverter(usdEurRates);

		var entries = IbkrCsvParser.parse(csvFiles).stream()
			.map(eurConverter::convertToEur)
			.toList();

		var results = entries.stream()
			.filter(e -> e instanceof StockEvent)
			.map(e -> (StockEvent) e)
			.collect(Collectors.groupingBy(StockEvent::getSymbol)).entrySet().stream()
			.map(entry -> new FifoTradeCoverer(entry.getKey()).cover(entry.getValue()))
			.toList();

		var covers = results.stream()
			.flatMap(r -> r.covers().stream())
			.toList();

		var uncovered = results.stream()
			.flatMap(r -> r.uncovered().stream())
			.toList();

		new TaxReportPrinter(covers, Currency.EUR).print(System.out);
		new OpenPositionsPrinter(uncovered, Currency.EUR).print(System.out);
	}

}
