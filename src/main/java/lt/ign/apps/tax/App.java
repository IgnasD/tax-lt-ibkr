package lt.ign.apps.tax;

import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lt.ign.apps.tax.core.EurConverter;
import lt.ign.apps.tax.core.FifoTradeCoverer;
import lt.ign.apps.tax.core.TaxReportPrinter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.Event;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;

public class App {

	public static void main(String[] args) {
		var csvFiles = Stream.of(args).map(Paths::get).toList();

		var usdEurRates = EcbXmlParser.forCurrency(Currency.USD).parseRates();
		var eurConverter = new EurConverter(usdEurRates);

		var covers = IbkrCsvParser.parse(csvFiles).stream()
			.map(eurConverter::convertToEur)
			.collect(Collectors.groupingBy(Event::getSymbol)).entrySet().stream()
			.flatMap(entry -> new FifoTradeCoverer(entry.getKey()).cover(entry.getValue()).stream())
			.toList();

		new TaxReportPrinter(Currency.EUR).print(covers);
	}

}
