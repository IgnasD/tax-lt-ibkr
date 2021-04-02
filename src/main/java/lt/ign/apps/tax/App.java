package lt.ign.apps.tax;

import java.util.List;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.parser.EcbXmlParser;
import lt.ign.apps.tax.parser.IbkrCsvParser;

public class App {

	public static void main(String[] args) {
		var ecbParser = EcbXmlParser.forCurrency(Currency.USD);
		ecbParser.parse();
		var usdEurRates = ecbParser.getRates();

		var ibkrParser = new IbkrCsvParser(List.of(args));
		ibkrParser.parse();
		var tradesOrig = ibkrParser.getTrades();

		var augmenter = new TradeDataAugmenter(usdEurRates);
		var tradesAug = augmenter.augmentEurData(tradesOrig);

		var coverer = new FifoTradeCoverer();
		var covers = coverer.cover(tradesAug);

		var printer = new TaxReportPrinter();
		printer.print(covers);
	}

}
