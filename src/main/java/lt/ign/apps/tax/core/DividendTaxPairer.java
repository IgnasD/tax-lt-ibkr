package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lt.ign.apps.tax.model.TaxedDividends;
import lt.ign.apps.tax.model.event.DividendEvent;
import lt.ign.apps.tax.model.event.Dividends;
import lt.ign.apps.tax.model.event.ReportEntry;
import lt.ign.apps.tax.model.event.WithholdingTax;

public class DividendTaxPairer {

	public List<TaxedDividends> pair(List<ReportEntry> entries) {
		return entries.stream()
			.filter(e -> e instanceof DividendEvent)
			.map(e -> (DividendEvent) e)
			.collect(Collectors.groupingBy(DividendEvent::getSymbol)).values().stream()
			.flatMap(this::pairSymbol)
			.toList();
	}

	private Stream<TaxedDividends> pairSymbol(List<DividendEvent> events) {
		return events.stream()
			.collect(Collectors.groupingBy(DividendEvent::getDateTime)).values().stream()
			.flatMap(this::pairSymbolDate);
	}

	private Stream<TaxedDividends> pairSymbolDate(List<DividendEvent> events) {
		var classes = events.stream().map(DividendEvent::getClass).distinct().count();

		if (classes == 1 && events.get(0) instanceof Dividends) {
			// dividends without withholding tax
			return events.stream().map(d -> TaxedDividends.create((Dividends) d, BigDecimal.ZERO));
		}

		if (classes != 2) {
			throw new UnsupportedOperationException("To many DividendEvent classes: " + classes);
		}
		if (events.size() % 2 != 0) {
			throw new UnsupportedOperationException("Odd number of dividend events");
		}

		var partitioned = events.stream().collect(Collectors.partitioningBy(e -> e instanceof Dividends));
		var dividends = partitioned.get(true).stream().map(e -> (Dividends) e).sorted(Comparator.comparing(Dividends::getAmount)).toList();
		var withholdingTax = partitioned.get(false).stream().map(e -> (WithholdingTax) e)
			.sorted(Comparator.comparing(WithholdingTax::getAmount).reversed()).toList();

		if (dividends.size() == 1) {
			// can be multiple withholding tax events canceling each other out due to corrective actions
			var tax = withholdingTax.stream().reduce(BigDecimal.ZERO, (a, t) -> a.add(t.getAmount()), (a1, a2) -> a1.add(a2));
			return dividends.stream().map(d -> TaxedDividends.create(d, tax));
		} else if (dividends.size() == withholdingTax.size()) {
			// map 1:1
			return IntStream.range(0, dividends.size())
				.mapToObj(i -> TaxedDividends.create(dividends.get(i), withholdingTax.get(i).getAmount()));
		}

		throw new UnsupportedOperationException(String.format("No algorithm to map %d dividends events to %d withholding tax events",
			dividends.size(), withholdingTax.size()));
	}

}
