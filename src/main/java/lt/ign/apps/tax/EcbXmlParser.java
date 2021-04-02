package lt.ign.apps.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EcbXmlParser extends DefaultHandler {

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

}
