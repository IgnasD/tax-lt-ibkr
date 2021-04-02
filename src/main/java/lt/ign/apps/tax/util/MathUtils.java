package lt.ign.apps.tax.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

	public static BigDecimal divide(BigDecimal left, BigDecimal right) {
		return left.divide(right, 10 /*out of my ass*/, RoundingMode.HALF_UP);
	}
}
