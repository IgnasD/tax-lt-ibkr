package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import lt.ign.apps.tax.core.CurrencyConverter;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.ModifiedTrade;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.Modifier;
import lt.ign.apps.tax.mods.PartialQuantity;
import lt.ign.apps.tax.mods.StockSplit;

public class TradePrinter {

	private final Trade trade;
	private final Currency baseCurrency;
	private final CurrencyConverter currencyConverter;

	public TradePrinter(Trade trade, Currency baseCurrency, CurrencyConverter currencyConverter) {
		this.trade = trade;
		this.baseCurrency = baseCurrency;
		this.currencyConverter = currencyConverter;
	}

	public void print(PrintStream ps) {
		printMultiCurrencyString(trade, ps);
		ps.println();

		List<Modifier> mods = List.of();
		var original = trade;
		if (trade instanceof ModifiedTrade modTrade) {
			mods = modTrade.getModifications();
			original = modTrade.getOriginal();
		}
		if (mods.isEmpty()) {
			return;
		}

		ps.print("^^ DERIVED FROM: ");
		printMultiCurrencyString(original, ps);
		ps.println();

		Trade modTrade = null;
		for (var mod : mods) {
			var prevTrade = modTrade != null ? modTrade : original;
			modTrade = prevTrade.modify(mod);
			if (mod instanceof PartialQuantity partial) {
				if (partial.getType() == PartialQuantity.Type.CARRY) {
					ps.println(String.format("%16s %s PARTIAL COVER, QUANTITY: %d -> %d", "",
						mod.getDateTime(), prevTrade.getQuantity(), modTrade.getQuantity()));
				}
			} else if (mod instanceof StockSplit) {
				ps.println(String.format("%16s %s STOCK SPLIT, QUANTITY: %d -> %d", "",
					mod.getDateTime(), prevTrade.getQuantity(), modTrade.getQuantity()));
			} else {
				throw new UnsupportedOperationException(String.format("Unsupported modifier: %s", mod.getClass().getSimpleName()));
			}
		}
	}

	private void printMultiCurrencyString(Trade tradeInOriginal, PrintStream ps) {
		ps.print(tradeInOriginal.toString());

		if (tradeInOriginal.getCurrency() != baseCurrency) {
			var tradeInBase = (ModifiedTrade) currencyConverter.convert(tradeInOriginal, baseCurrency);
			var currencyConversion = tradeInBase.getModifications().get(tradeInBase.getModifications().size() - 1);
			var str = String.format(Locale.ROOT, " | %.2f%s %.2f%s (rate: %s)",
				tradeInBase.getProceeds(), tradeInBase.getCurrency(),
				tradeInBase.getFees(), tradeInBase.getCurrency(),
				currencyConversion.toString());
			ps.print(str);
		}
	}

}
