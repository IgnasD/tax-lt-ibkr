package lt.ign.apps.tax.model;

import java.time.LocalDateTime;

public class Split extends Event {

	private final int multiplier;

	public Split(String symbol, LocalDateTime dateTime, int multiplier) {
		super(symbol, dateTime);
		this.multiplier = multiplier;
	}

	public int getMultiplier() {
		return multiplier;
	}

}
