package lt.ign.apps.tax.model;

import java.util.List;

public class Cover {

	private List<Open> opens;
	private Trade close;

	public List<Open> getOpens() {
		return opens;
	}

	public void setOpens(List<Open> opens) {
		this.opens = opens;
	}

	public Trade getClose() {
		return close;
	}

	public void setClose(Trade close) {
		this.close = close;
	}

	public static class Open {
		private Trade trade;
		private int amountCovered;

		public Trade getTrade() {
			return trade;
		}

		public void setTrade(Trade trade) {
			this.trade = trade;
		}

		public int getAmountCovered() {
			return amountCovered;
		}

		public void setAmountCovered(int amountCovered) {
			this.amountCovered = amountCovered;
		}
	}

}
