package lt.ign.apps.tax.model;

import java.util.List;

import lt.ign.apps.tax.model.event.Trade;

public record Position(List<Cover> covers, List<Trade> uncovered) {
}
