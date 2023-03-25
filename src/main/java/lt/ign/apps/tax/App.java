package lt.ign.apps.tax;

import java.nio.file.Paths;
import java.util.stream.Stream;

import lt.ign.apps.tax.core.FifoTradeCoverer;
import lt.ign.apps.tax.core.TaxReportPrinter;
import lt.ign.apps.tax.core.TradeDataAugmenter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;

public class App {

	public static void main(String[] args) {
		var ecbParser = EcbXmlParser.forCurrency(Currency.USD);
		ecbParser.parse();
		var usdEurRates = ecbParser.getRates();

		var csvFiles = Stream.of(args).map(Paths::get).toList();
		var tradesOrig = IbkrCsvParser.parse(csvFiles);

		var augmenter = new TradeDataAugmenter(usdEurRates);
		var tradesAug = augmenter.augmentEurData(tradesOrig);

		var coverer = new FifoTradeCoverer();
		var covers = coverer.cover(tradesAug);

		var printer = new TaxReportPrinter();
		printer.print(covers);
	}

}
