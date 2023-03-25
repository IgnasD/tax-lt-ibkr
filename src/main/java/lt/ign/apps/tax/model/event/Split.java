package lt.ign.apps.tax.model.event;

import java.time.LocalDateTime;
import java.util.Locale;

public class Split extends Event {

	private final int multiplier;

	public Split(String symbol, LocalDateTime dateTime, int multiplier) {
		super(symbol, dateTime);
		this.multiplier = multiplier;
	}

	public int getMultiplier() {
		return multiplier;
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s SPLIT %s %dx", getDateTime(), getSymbol(), multiplier);
	}

}
