package lt.ign.apps.tax.parser;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.Event;
import lt.ign.apps.tax.model.Split;
import lt.ign.apps.tax.model.Trade;

public class IbkrCsvParser {

	private static final String SECTION_TRADES = "Trades";
	private static final String SECTION_CORPORATE_ACTIONS = "Corporate Actions";

	private static final String LINE_HEADER = "Header";
	private static final String LINE_DATA = "Data";

	private static final String HEADER_DATA_DISCRIMINATOR = "DataDiscriminator";
	private static final String HEADER_ASSET_CATEGORY = "Asset Category";
	private static final String HEADER_CURRENCY = "Currency";
	private static final String HEADER_SYMBOL = "Symbol";
	private static final String HEADER_DATE_TIME = "Date/Time";
	private static final String HEADER_DESCRIPTION = "Description";
	private static final String HEADER_QUANTITY = "Quantity";
	private static final String HEADER_PROCEEDS = "Proceeds";
	private static final String HEADER_VALUE = "Value";
	private static final String HEADER_COMM_FEE = "Comm/Fee";
	private static final String HEADER_REALIZED_PL = "Realized P/L";
	private static final String HEADER_CODE = "Code";

	private static final String DATA_ORDER = "Order";

	private static final String ASSET_STOCKS = "Stocks";

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");
	private static final Pattern splitPattern = Pattern.compile("^([a-zA-Z]+?)\\([A-Za-z0-9]+?\\) Split ([0-9]+?) for ([0-9]+?) ");

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
		case "O;P":
			return Trade.Type.OPEN;
		case "C":
		case "C;P":
			return Trade.Type.CLOSE;
		default:
			throw new UnsupportedOperationException("Unknown code: " + str);
		}
	}

	private static Optional<Trade> parseTrade(String[] line, Map<String, Integer> fieldMap) {
		if (!line[fieldMap.get(HEADER_DATA_DISCRIMINATOR)].equals(DATA_ORDER)
			|| !line[fieldMap.get(HEADER_ASSET_CATEGORY)].equals(ASSET_STOCKS)) {
			return Optional.empty();
		}

		var currency = Currency.valueOf(line[fieldMap.get(HEADER_CURRENCY)]);
		var symbol = line[fieldMap.get(HEADER_SYMBOL)];
		var dateTime = LocalDateTime.parse(line[fieldMap.get(HEADER_DATE_TIME)], dateTimeFormatter);
		var quantity = Integer.parseInt(line[fieldMap.get(HEADER_QUANTITY)]);
		var proceeds = new BigDecimal(line[fieldMap.get(HEADER_PROCEEDS)]);
		var fees = new BigDecimal(line[fieldMap.get(HEADER_COMM_FEE)]);
		var type = parseTradeType(line[fieldMap.get(HEADER_CODE)]);
		return Optional.of(new Trade(symbol, dateTime, type, quantity, proceeds, fees, currency));
	}

	private static Optional<Split> parseSplit(String[] line, Map<String, Integer> fieldMap) {
		if (!line[fieldMap.get(HEADER_ASSET_CATEGORY)].equals(ASSET_STOCKS)) {
			return Optional.empty();
		}

		var matcher = splitPattern.matcher(line[fieldMap.get(HEADER_DESCRIPTION)]);

		if (!matcher.find() || !line[fieldMap.get(HEADER_PROCEEDS)].equals("0") || !line[fieldMap.get(HEADER_VALUE)].equals("0")
			|| !line[fieldMap.get(HEADER_REALIZED_PL)].equals("0") || !line[fieldMap.get(HEADER_CODE)].isEmpty()) {
			throw new UnsupportedOperationException("Unknown split detected: " + Arrays.toString(line));
		}

		var dateTime = LocalDateTime.parse(line[fieldMap.get(HEADER_DATE_TIME)], dateTimeFormatter);
		var symbol = matcher.group(1);

		var dividend = Integer.parseInt(matcher.group(2));
		var divisor = Integer.parseInt(matcher.group(3));
		if (dividend % divisor != 0) {
			throw new UnsupportedOperationException("Fractional split detected");
		}
		var multiplier = dividend / divisor;

		return Optional.of(new Split(symbol, dateTime, multiplier));
	}

	private static List<Event> parseFile(Path csvFile) {
		Map<String, Integer> tradesFieldMap = null;
		Map<String, Integer> corporateActionsFieldMap = null;
		var events = new ArrayList<Event>();

		try (var reader = new CSVReader(Files.newBufferedReader(csvFile))) {
			String[] line;
			while ((line = reader.readNext()) != null) {
				if (line[0].equals(SECTION_TRADES)) {
					if (line[1].equals(LINE_HEADER)) {
						tradesFieldMap = genFieldMap(line);
						continue;
					}
					if (line[1].equals(LINE_DATA)) {
						var trade = parseTrade(line, tradesFieldMap);
						trade.ifPresent(events::add);
						continue;
					}
				}

				if (line[0].equals(SECTION_CORPORATE_ACTIONS)) {
					if (line[1].equals(LINE_HEADER)) {
						corporateActionsFieldMap = genFieldMap(line);
						continue;
					}
					if (line[1].equals(LINE_DATA)) {
						var split = parseSplit(line, corporateActionsFieldMap);
						split.ifPresent(events::add);
						continue;
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return events;
	}

	public static List<Event> parse(List<Path> csvFiles) {
		return csvFiles.stream().flatMap(path -> parseFile(path).stream()).toList();
	}

}
