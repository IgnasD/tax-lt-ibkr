package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.mods.Modifier;

public class Trade extends Event {

	private final Type type;
	private final int quantity;
	private final BigDecimal proceeds;
	private final BigDecimal fees;
	private final Currency currency;

	public Trade(String symbol, LocalDateTime dateTime, Type type, int quantity, BigDecimal proceeds, BigDecimal fees, Currency currency) {
		super(symbol, dateTime);
		this.type = type;
		this.quantity = quantity;
		this.proceeds = proceeds;
		this.fees = fees;
		this.currency = currency;
	}

	public Type getType() {
		return type;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getProceeds() {
		return proceeds;
	}

	public BigDecimal getFees() {
		return fees;
	}

	public Currency getCurrency() {
		return currency;
	}
	
	public Trade modify(Modifier modifier) {
		return ModifiedTrade.create(this, List.of(modifier));
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s %s %s %d %.2f%s %.2f%s", getDateTime(), type, getSymbol(), quantity, proceeds, currency, fees,
			currency);
	}

	public enum Type {
		OPEN, CLOSE
	}

}
