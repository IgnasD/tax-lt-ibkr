package lt.ign.apps.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class CurrencyConverter {

	private static final String USD_EUR_XML_URL = "https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/usd.xml";

	private final Map<LocalDate, BigDecimal> usdEurRates;

	public CurrencyConverter() {
		usdEurRates = fetchUsdEurRates();
	}

	private Map<LocalDate, BigDecimal> fetchUsdEurRates() {
		var parser = new EcbXmlParser(USD_EUR_XML_URL, Currency.USD);
		parser.parse();
		return parser.getRates();
	}

	public BigDecimal usdToEur(BigDecimal usd, LocalDate date) {
		return usd.divide(usdEurRates.get(date), 10 /*out of my ass*/, RoundingMode.HALF_UP);
	}

}
