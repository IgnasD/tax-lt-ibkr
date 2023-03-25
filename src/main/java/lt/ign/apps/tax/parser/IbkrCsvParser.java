package lt.ign.apps.tax.parser;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.Trade;

public class IbkrCsvParser {

	private static final String SECTION_TRADES = "Trades";

	private static final String LINE_HEADER = "Header";
	private static final String LINE_DATA = "Data";

	private static final String HEADER_DATA_DISCRIMINATOR = "DataDiscriminator";
	private static final String HEADER_ASSET_CATEGORY = "Asset Category";
	private static final String HEADER_CURRENCY = "Currency";
	private static final String HEADER_SYMBOL = "Symbol";
	private static final String HEADER_DATE_TIME = "Date/Time";
	private static final String HEADER_QUANTITY = "Quantity";
	private static final String HEADER_PROCEEDS = "Proceeds";
	private static final String HEADER_COMM_FEE = "Comm/Fee";
	private static final String HEADER_CODE = "Code";

	private static final String DATA_ORDER = "Order";

	private static final String ASSET_STOCKS = "Stocks";

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

	private static Map<String, Integer> genFieldMap(String[] fields) {
		var fieldMap = new HashMap<String, Integer>();
		for (int i = 0; i < fields.length; i++) {
			fieldMap.put(fields[i], i);
		}
		return fieldMap;
	}

	private static Trade.Type parseTradeType(String str) {
		switch (str) {
		case "O":
		case "O;P": // hacky hacky
			return Trade.Type.OPEN;
		case "C":
		case "C;P":
			return Trade.Type.CLOSE;
		default:
			throw new IllegalArgumentException("Unknown code: " + str);
		}
	}

	private static List<Trade> parseFile(Path csvFile) {
		Map<String, Integer> fieldMap = null;
		List<Trade> trades = new ArrayList<>();

		try (CSVReader reader = new CSVReader(Files.newBufferedReader(csvFile))) {
			String[] line;
			while ((line = reader.readNext()) != null) {
				if (!line[0].equals(SECTION_TRADES)) continue;
				if (line[1].equals(LINE_HEADER)) {
					fieldMap = genFieldMap(line);
					continue;
				}
				if (!line[1].equals(LINE_DATA)) continue;
				if (!line[fieldMap.get(HEADER_DATA_DISCRIMINATOR)].equals(DATA_ORDER)) continue;
				if (!line[fieldMap.get(HEADER_ASSET_CATEGORY)].equals(ASSET_STOCKS)) continue;

				var trade = new Trade();
				trade.setCurrency(Currency.valueOf(line[fieldMap.get(HEADER_CURRENCY)]));
				trade.setSymbol(line[fieldMap.get(HEADER_SYMBOL)]);
				trade.setDateTime(LocalDateTime.parse(line[fieldMap.get(HEADER_DATE_TIME)], dateTimeFormatter));
				trade.setQuantity(Integer.parseInt(line[fieldMap.get(HEADER_QUANTITY)]));
				trade.setProceeds(new BigDecimal(line[fieldMap.get(HEADER_PROCEEDS)]));
				trade.setFees(new BigDecimal(line[fieldMap.get(HEADER_COMM_FEE)]));
				trade.setType(parseTradeType(line[fieldMap.get(HEADER_CODE)]));
				trades.add(trade);

				// TODO adjust for stock splits
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return trades;
	}

	public static List<Trade> parse(List<Path> csvFiles) {
		return csvFiles.stream().flatMap(path -> parseFile(path).stream()).toList();
	}

}
