package lt.ign.apps.tax.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lt.ign.apps.tax.model.TaxedInterest;
import lt.ign.apps.tax.model.event.Interest;
import lt.ign.apps.tax.model.event.ReportEntry;
import lt.ign.apps.tax.model.event.WithholdingTax;

public class InterestTaxPairer {

	public List<TaxedInterest> pair(List<ReportEntry> entries) {
		return entries.stream()
			.filter(e -> {
				if (e instanceof Interest) {
					return true;
				} else if (e instanceof WithholdingTax wt) {
					return wt.getType() == WithholdingTax.Type.CREDIT_INTEREST;
				} else {
					return false;
				}
			})
			.collect(Collectors.groupingBy(e -> {
				if (e instanceof Interest i) {
					return i.getDate();
				} else if (e instanceof WithholdingTax wt) {
					return wt.getDate();
				}
				throw new IllegalStateException("Should not happen");
			})).values().stream()
			.flatMap(this::pairDate)
			.toList();
	}

	private Stream<TaxedInterest> pairDate(List<ReportEntry> events) {
		var classes = events.stream().map(ReportEntry::getClass).distinct().count();

		if (classes == 1 && events.get(0) instanceof Interest) {
			// interest without withholding tax
			return events.stream().map(e -> TaxedInterest.create((Interest) e, BigDecimal.ZERO));
		}

		if (classes != 2) {
			throw new UnsupportedOperationException("To many classes for interest tax mapping: " + classes);
		}
		if (events.size() % 2 != 0) {
			throw new UnsupportedOperationException("Odd number of interest + withholding tax events");
		}

		var partitioned = events.stream().collect(Collectors.partitioningBy(e -> e instanceof Interest));
		var interest = partitioned.get(true).stream().map(e -> (Interest) e).toList();
		var withholdingTax = partitioned.get(false).stream().map(e -> (WithholdingTax) e).toList();

		if (interest.size() == withholdingTax.size()) {
			// map 1:1
			return IntStream.range(0, interest.size())
				.mapToObj(i -> TaxedInterest.create(interest.get(i), withholdingTax.get(i).getAmount()));
		}

		throw new UnsupportedOperationException(String.format("No algorithm to map %d dividends events to %d withholding tax events",
			interest.size(), withholdingTax.size()));
	}

}
