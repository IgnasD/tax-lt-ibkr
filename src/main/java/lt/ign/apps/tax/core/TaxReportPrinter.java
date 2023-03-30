package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.Cover;
import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.ModifiedTrade;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.CurrencyConversion;
import lt.ign.apps.tax.mods.Modifier;
import lt.ign.apps.tax.mods.PartialQuantity;
import lt.ign.apps.tax.mods.StockSplit;

public class TaxReportPrinter {

	private final Currency baseCurrency;

	public TaxReportPrinter(Currency baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public void print(List<Cover> covers) {
		Map<Integer, List<Cover>> yearlyCovers = covers.stream()
			.collect(Collectors.groupingBy(c -> c.getClose().getDateTime().getYear()));
		for (var entry : yearlyCovers.entrySet()) {
			System.out.println("====================================================================================================");
			System.out.println("Tax year: " + entry.getKey());
			System.out.println("----------------------------------------------------------------------------------------------------");
			printInternal(entry.getValue().stream().sorted(Comparator.comparing(c -> c.getClose().getDateTime())).toList());
			System.out.println("====================================================================================================");
		}
	}

	private void printInternal(List<Cover> covers) {
		var totalRacBase = new RevenueAndCost();
		var totalRacPerCurrency = new HashMap<Currency, RevenueAndCost>();

		for (var cover : covers) {
			var tradeRacBase = new RevenueAndCost();
			var tradeRacOriginal = new RevenueAndCost();

			var close = cover.getClose();
			if (close.getCurrency() != baseCurrency) {
				throw new UnsupportedOperationException(String
					.format("Expected base currency (%s) does not match close trade currency (%s)", baseCurrency, close.getCurrency()));
			}

			System.out.println(close.getSymbol());

			for (var open : cover.getOpens()) {
				if (open.getCurrency() != baseCurrency) {
					throw new UnsupportedOperationException(String
						.format("Expected base currency (%s) does not match open trade currency (%s)", baseCurrency, open.getCurrency()));
				}

				tradeRacBase.addCost(open.getProceeds());
				tradeRacBase.addCost(open.getFees());

				var openView = new TradeView(open);
				tradeRacOriginal.addCost(openView.getInOriginalCurrency().getProceeds());
				tradeRacOriginal.addCost(openView.getInOriginalCurrency().getFees());

				print(openView);
			}

			tradeRacBase.addRevenue(close.getProceeds());
			tradeRacBase.addCost(close.getFees());

			var closeView = new TradeView(close);
			tradeRacOriginal.addRevenue(closeView.getInOriginalCurrency().getProceeds());
			tradeRacOriginal.addCost(closeView.getInOriginalCurrency().getFees());

			print(closeView);

			var originalCurrency = closeView.getInOriginalCurrency().getCurrency();
			System.out.println(String.format(Locale.ROOT, "P&L: %.2f%s %.2f%s", tradeRacOriginal.profitLoss(), originalCurrency,
				tradeRacBase.profitLoss(), baseCurrency));
			System.out.println("----------------------------------------------------------------------------------------------------");

			totalRacPerCurrency.merge(originalCurrency, tradeRacOriginal, (a, b) -> {
				a.addRevenueAndCost(b);
				return a;
			});

			totalRacBase.addRevenueAndCost(tradeRacBase);
		}

		totalRacPerCurrency.entrySet().forEach(e -> {
			var currency = e.getKey();
			var rac = e.getValue();
			System.out.println(String.format(Locale.ROOT, "%s. Cost: %.2f%s; Revenue: %.2f%s; P&L: %.2f%s", currency, rac.cost, currency,
				rac.revenue, currency, rac.profitLoss(), currency));
		});

		System.out.println(String.format(Locale.ROOT, "TOTAL. Cost: %.2fEUR; Revenue: %.2fEUR; P&L: %.2fEUR", totalRacBase.cost,
			totalRacBase.revenue, totalRacBase.profitLoss()));
	}

	private static void print(TradeView tradeView) {
		System.out.println(toMultiCurrencyString(tradeView));
		if (!tradeView.nonCurrencyMods.isEmpty()) {
			var trade = tradeView.getOriginalInBase();
			System.out.println("^^ DERIVED FROM: " + toMultiCurrencyString(trade));
			for (var mod : tradeView.nonCurrencyMods) {
				var moddedTrade = trade.modify(mod);
				if (mod instanceof PartialQuantity partial) {
					if (partial.getType() == PartialQuantity.Type.CARRY) {
						System.out.println(String.format("%16s %s PARTIAL COVER, QUANTITY: %d -> %d", "", mod.getDateTime(),
							trade.getQuantity(), moddedTrade.getQuantity()));
					}
				} else if (mod instanceof StockSplit split) {
					System.out.println(String.format("%16s %s STOCK SPLIT, QUANTITY: %d -> %d", "", mod.getDateTime(), trade.getQuantity(),
						moddedTrade.getQuantity()));
				} else {
					throw new UnsupportedOperationException("Unrecognized modifier: " + mod.getClass().getSimpleName());
				}
			}
		}
	}

	private static String toMultiCurrencyString(TradeView tradeView) {
		if (tradeView.currencyConversion.isEmpty()) {
			return tradeView.trade.toString();
		} else {
			return String.format(Locale.ROOT, "%s | %.2f%s %.2f%s (rate: %s)", tradeView.getInOriginalCurrency(),
				tradeView.trade.getProceeds(), tradeView.trade.getCurrency(), tradeView.trade.getFees(), tradeView.trade.getCurrency(),
				tradeView.currencyConversion.get());
		}
	}

	private static String toMultiCurrencyString(Trade trade) {
		return toMultiCurrencyString(new TradeView(trade));
	}

	private static class TradeView {
		private final Trade trade;
		private final Trade original;
		private final Optional<CurrencyConversion> currencyConversion;
		private final List<Modifier> nonCurrencyMods;

		private TradeView(Trade trade) {
			this.trade = trade;

			Trade original = trade;
			CurrencyConversion currencyConversion = null;
			List<Modifier> nonCurrencyMods = Collections.emptyList();
			if (trade instanceof ModifiedTrade modded) {
				original = modded.getOriginal();
				var currencyConversionPartitioned = modded.getModifications().stream()
					.collect(Collectors.partitioningBy(mod -> mod instanceof CurrencyConversion));

				var currencyConversions = currencyConversionPartitioned.get(true);
				if (currencyConversions.size() > 1) {
					throw new UnsupportedOperationException(
						"Expected single currency conversion per position, got: " + currencyConversions.size());
				}
				if (currencyConversions.size() == 1) {
					currencyConversion = (CurrencyConversion) currencyConversions.get(0);
					nonCurrencyMods = currencyConversionPartitioned.get(false);
				}
			}

			this.original = original;
			this.currencyConversion = Optional.ofNullable(currencyConversion);
			this.nonCurrencyMods = nonCurrencyMods;
		}

		private Trade _inOriginalCurrency = null;

		private Trade getInOriginalCurrency() {
			if (_inOriginalCurrency == null) {
				_inOriginalCurrency = ModifiedTrade.create(original, nonCurrencyMods);
			}
			return _inOriginalCurrency;
		}

		private Trade _originalInBase = null;

		private Trade getOriginalInBase() {
			if (_originalInBase == null) {
				_originalInBase = ModifiedTrade.create(original, currencyConversion.map(cc -> (Modifier) cc).stream().toList());
			}
			return _originalInBase;
		}
	}

	private static class RevenueAndCost {
		private BigDecimal revenue = BigDecimal.ZERO;
		private BigDecimal cost = BigDecimal.ZERO; // negatively denominated

		private void addRevenue(BigDecimal revenue) {
			this.revenue = this.revenue.add(revenue);
		}

		private void addCost(BigDecimal cost) {
			this.cost = this.cost.add(cost);
		}

		private void addRevenueAndCost(RevenueAndCost rac) {
			addRevenue(rac.revenue);
			addCost(rac.cost);
		}

		private BigDecimal profitLoss() {
			return revenue.add(cost);
		}
	}

}
