package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.util.Locale;

import lt.ign.apps.tax.model.ModifiedTrade;
import lt.ign.apps.tax.model.TradeCurrencyView;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.PartialQuantity;
import lt.ign.apps.tax.mods.StockSplit;

public class TradePrinter {

	private final TradeCurrencyView view;

	public TradePrinter(TradeCurrencyView view) {
		this.view = view;
	}

	public TradePrinter(Trade trade) {
		this.view = new TradeCurrencyView(trade);
	}

	public void print(PrintStream ps) {
		printMultiCurrencyString(ps);
		ps.println();

		var mods = view.getOtherModifications();
		if (mods.isEmpty()) {
			return;
		}

		ps.print("^^ DERIVED FROM: ");
		var trade = view.getOriginalInBaseCurrency();
		printMultiCurrencyString(trade, ps);
		ps.println();

		ModifiedTrade modTrade = null;
		for (var mod : mods) {
			modTrade = (ModifiedTrade) (modTrade != null ? modTrade : trade).modify(mod);
			if (mod instanceof PartialQuantity partial) {
				if (partial.getType() == PartialQuantity.Type.CARRY) {
					ps.println(String.format("%16s %s PARTIAL COVER, QUANTITY: %d -> %d", "", mod.getDateTime(),
						trade.getQuantity(), modTrade.getQuantity()));
				}
			} else if (mod instanceof StockSplit) {
				ps.println(String.format("%16s %s STOCK SPLIT, QUANTITY: %d -> %d", "", mod.getDateTime(), trade.getQuantity(),
					modTrade.getQuantity()));
			} else {
				throw new UnsupportedOperationException("Unrecognized modifier: " + mod.getClass().getSimpleName());
			}
		}
	}

	private void printMultiCurrencyString(PrintStream ps) {
		printMultiCurrencyString(view, ps);
	}

	private static void printMultiCurrencyString(TradeCurrencyView view, PrintStream ps) {
		var inOriginalCurrency = view.getInOriginalCurrency();
		ps.print(inOriginalCurrency.toString());
		view.getCurrencyConversion().ifPresent(cc -> {
			var inBaseCurrency = view.getInBaseCurrency();
			var str = String.format(Locale.ROOT, " | %.2f%s %.2f%s (rate: %s)",
				inBaseCurrency.getProceeds(), inBaseCurrency.getCurrency(),
				inBaseCurrency.getFees(), inBaseCurrency.getCurrency(),
				cc.toString());
			ps.print(str);
		});
	}

	private static void printMultiCurrencyString(Trade trade, PrintStream ps) {
		var view = new TradeCurrencyView(trade);
		printMultiCurrencyString(view, ps);
	}

}
