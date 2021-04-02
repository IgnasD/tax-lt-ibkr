package lt.ign.apps.tax.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lt.ign.apps.tax.model.Currency;

public class EcbXmlParser extends DefaultHandler {

	private static final Map<Currency, String> XML_URLS = Map.of(Currency.USD,
			"https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/usd.xml");

	private final String url;
	private final Currency currency;

	private Map<LocalDate, BigDecimal> rates;

	private boolean seriesFound = false;

	public EcbXmlParser(String url, Currency currency) {
		this.url = url;
		this.currency = currency;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("Series") && attributes != null && attributes.getValue("CURRENCY").equals(currency.name())) {
			seriesFound = true;
		} else if (qName.equals("Series")) {
			seriesFound = false;
		} else if (qName.equals("Obs") && seriesFound) {
			String dateStr = attributes.getValue("TIME_PERIOD");
			LocalDate date = LocalDate.parse(dateStr);
			String rateStr = attributes.getValue("OBS_VALUE");
			BigDecimal rate = new BigDecimal(rateStr);
			rates.put(date, rate);
		}
	}

	public void parse() {
		rates = new HashMap<>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(url, this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Map<LocalDate, BigDecimal> getRates() {
		return rates;
	}

	public static EcbXmlParser forCurrency(Currency currency) {
		return new EcbXmlParser(XML_URLS.get(currency), currency);
	}

}
