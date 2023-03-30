package lt.ign.apps.tax.mods;

import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

import lt.ign.apps.tax.model.event.Trade;

public abstract class Modifier implements UnaryOperator<Trade> {

	private final LocalDateTime dateTime;

	protected Modifier(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

}
