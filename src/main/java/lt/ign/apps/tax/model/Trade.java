package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Trade {

	private Type type;
	private String symbol;
	private LocalDateTime dateTime;
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

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
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
		return String.format("%s %s %s %d %f%s %f%s", dateTime, type, symbol, quantity, proceeds, currency, fees, currency);
	}

	public enum Type {
		OPEN, CLOSE
	}

}
