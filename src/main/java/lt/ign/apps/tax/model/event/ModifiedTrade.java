package lt.ign.apps.tax.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.mods.Modifier;

public class ModifiedTrade extends Trade {

	private final Trade original;
	private final List<Modifier> modifications;

	private ModifiedTrade(String symbol, LocalDateTime dateTime, Type type, int quantity, BigDecimal proceeds, BigDecimal fees,
		Currency currency, Trade original, List<Modifier> modifications) {
		super(symbol, dateTime, type, quantity, proceeds, fees, currency);
		this.original = original;
		this.modifications = List.copyOf(modifications);
	}

	public Trade getOriginal() {
		return original;
	}

	public List<Modifier> getModifications() {
		return modifications;
	}

	@Override
	public Trade modify(Modifier modifier) {
		var mods = new ArrayList<>(modifications);
		mods.add(modifier);
		return create(original, mods);
	}

	public static ModifiedTrade create(Trade original, List<Modifier> mods) {
		var modified = mods.stream().reduce(original, (trade, mod) -> mod.apply(trade), (oldTrade, newTrade) -> newTrade);
		return new ModifiedTrade(modified.getSymbol(), modified.getDateTime(), modified.getType(), modified.getQuantity(),
			modified.getProceeds(), modified.getFees(), modified.getCurrency(), original, mods);
	}

	/*@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s | %.2fEUR %.2fEUR (rate: %.4f USD/EUR)", super.toString(), proceedsEur, feesEur,
				conversionRate);
	}*/

}
