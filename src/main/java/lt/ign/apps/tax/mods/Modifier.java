package lt.ign.apps.tax.mods;

import java.util.function.UnaryOperator;

import lt.ign.apps.tax.model.event.Trade;

public interface Modifier extends UnaryOperator<Trade> {

}
