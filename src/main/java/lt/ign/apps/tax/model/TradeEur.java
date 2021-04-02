package lt.ign.apps.tax.model;

import java.math.BigDecimal;
import java.util.Locale;

public class TradeEur extends Trade {

	private BigDecimal proceedsEur;
	private BigDecimal feesEur;
	private BigDecimal conversionRate;

	public BigDecimal getProceedsEur() {
		return proceedsEur;
	}

	public void setProceedsEur(BigDecimal proceedsEur) {
		this.proceedsEur = proceedsEur;
	}

	public BigDecimal getFeesEur() {
		return feesEur;
	}

	public void setFeesEur(BigDecimal feesEur) {
		this.feesEur = feesEur;
	}

	public BigDecimal getConversionRate() {
		return conversionRate;
	}

	public void setConversionRate(BigDecimal conversionRate) {
		this.conversionRate = conversionRate;
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s | %.2fEUR %.2fEUR (rate: %.4f USD/EUR)", super.toString(), proceedsEur, feesEur,
				conversionRate);
	}

}
