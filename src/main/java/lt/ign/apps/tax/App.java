package lt.ign.apps.tax;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lt.ign.apps.tax.core.FifoTradeCoverer;
import lt.ign.apps.tax.core.TaxReportPrinter;
import lt.ign.apps.tax.core.EurConverter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Event;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;

public class App {

	public static void main(String[] args) {
		var ecbParser = EcbXmlParser.forCurrency(Currency.USD);
		ecbParser.parse();
		var usdEurRates = ecbParser.getRates();

		var csvFiles = Stream.of(args).map(Paths::get).toList();
		var eventsCsv = IbkrCsvParser.parse(csvFiles);

		var eurConverter = new EurConverter(usdEurRates);

		eventsCsv.stream().map(eurConverter::convertToEur).collect(Collectors.groupingBy(Event::getSymbol)).forEach((symbol, events) -> {
			System.out.println(symbol);
			events.stream().sorted(Comparator.comparing(Event::getDateTime)).forEachOrdered(System.out::println);
		});

//		var coverer = new FifoTradeCoverer();
//		var covers = coverer.cover(tradesAug);
//
//		var printer = new TaxReportPrinter();
//		printer.print(covers);
	}

}
