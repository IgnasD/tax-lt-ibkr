package lt.ign.apps.tax.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.mods.CurrencyConversion;
import lt.ign.apps.tax.mods.Modifier;

public class TradeCurrencyView {

	private final Trade trade;
	private final Trade original;
	private final Optional<CurrencyConversion> currencyConversion;
	private final List<Modifier> nonCurrencyMods;

	private Trade _inOriginalCurrency = null;
	private Trade _originalInBaseCurrency = null;

	public TradeCurrencyView(Trade trade) {
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
		this.nonCurrencyMods = List.copyOf(nonCurrencyMods);
	}

	public Optional<CurrencyConversion> getCurrencyConversion() {
		return currencyConversion;
	}

	public List<Modifier> getOtherModifications() {
		return nonCurrencyMods;
	}

	public Trade getInBaseCurrency() {
		return trade;
	}

	public Trade getInOriginalCurrency() {
		if (_inOriginalCurrency == null) {
			_inOriginalCurrency = ModifiedTrade.create(original, nonCurrencyMods);
		}
		return _inOriginalCurrency;
	}

	public Trade getOriginalInBaseCurrency() {
		if (_originalInBaseCurrency == null) {
			_originalInBaseCurrency = ModifiedTrade.create(original, currencyConversion.map(cc -> (Modifier) cc).stream().toList());
		}
		return _originalInBaseCurrency;
	}

}