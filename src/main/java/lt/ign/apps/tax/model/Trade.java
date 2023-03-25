package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.util.Locale;

public class Trade extends Event {

	private Type type;
	private int quantity;
	private BigDecimal proceeds;
	private BigDecimal fees;
	private Currency currency;

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getProceeds() {
		return proceeds;
	}

	public void setProceeds(BigDecimal proceeds) {
		this.proceeds = proceeds;
	}

	public BigDecimal getFees() {
		return fees;
	}

	public void setFees(BigDecimal fees) {
		this.fees = fees;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
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
