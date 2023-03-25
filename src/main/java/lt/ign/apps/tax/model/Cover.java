package lt.ign.apps.tax.model;

import java.util.List;

import lt.ign.apps.tax.model.event.Trade;

public class Cover {

	private final List<Trade> opens;
	private final Trade close;

	public Cover(List<Trade> opens, Trade close) {
		this.opens = opens;
		this.close = close;
	}

	public List<Trade> getOpens() {
		return opens;
	}

	public Trade getClose() {
		return close;
	}

}
