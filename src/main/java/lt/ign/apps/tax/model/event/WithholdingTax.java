package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lt.ign.apps.tax.model.Currency;

public class WithholdingTax extends DividendEvent {

	public WithholdingTax(String symbol, LocalDateTime dateTime, Currency currency, BigDecimal amount) {
		super(symbol, dateTime, currency, amount);
	}

}
