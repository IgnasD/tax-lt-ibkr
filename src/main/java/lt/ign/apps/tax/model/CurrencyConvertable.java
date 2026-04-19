package lt.ign.apps.tax.model;

import java.time.LocalDateTime;

public interface CurrencyConvertable<T> {

	LocalDateTime getDateTime();

	Currency getCurrency();

	T convertCurrency(ExchangeRate exchangeRate);

}
