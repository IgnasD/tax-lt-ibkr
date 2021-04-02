package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import lt.ign.apps.tax.MathUtils;

public class Cover<T extends Trade> {

	private List<Open> opens;
	private T close;

	public List<Open> getOpens() {
		return opens;
	}

	public void setOpens(List<Open> opens) {
		this.opens = opens;
	}

	public T getClose() {
		return close;
	}

	public void setClose(T close) {
		this.close = close;
	}

	public class Open {
		private T trade;
		private int amountCovered;

		public T getTrade() {
			return trade;
		}

		public void setTrade(T trade) {
			this.trade = trade;
		}

		public int getAmountCovered() {
			return amountCovered;
		}

		public void setAmountCovered(int amountCovered) {
			this.amountCovered = amountCovered;
		}

		public BigDecimal proportional(Function<T, BigDecimal> getter) {
			var initial = getter.apply(trade);
			if (amountCovered == trade.getQuantity()) {
				return initial;
			}
			return MathUtils.divide(initial.multiply(new BigDecimal(amountCovered)), new BigDecimal(trade.getQuantity()));
		}
	}

}
