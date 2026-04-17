package lt.ign.apps.tax.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
import com.opencsv.exceptions.CsvException;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.event.DepositWithdrawal;
import lt.ign.apps.tax.model.event.DividendEvent;
import lt.ign.apps.tax.model.event.Dividends;
import lt.ign.apps.tax.model.event.ReportEntry;
import lt.ign.apps.tax.model.event.Split;
import lt.ign.apps.tax.model.event.Trade;
import lt.ign.apps.tax.model.event.WithholdingTax;

public class IbkrCsvParser {

	private static final String SECTION_TRADES = "Trades";
	private static final String SECTION_CORPORATE_ACTIONS = "Corporate Actions";
	private static final String SECTION_DEPOSITS_WITHDRAWALS = "Deposits & Withdrawals";
	private static final String SECTION_DIVIDENDS = "Dividends";
	private static final String SECTION_WITHHOLDING_TAX = "Withholding Tax";

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
	private static final String HEADER_SETTLE_DATE = "Settle Date";
	private static final String HEADER_AMOUNT = "Amount";
	private static final String HEADER_DATE = "Date";

	private static final String DESCRIPTION_ADJUSTMENT = "Adjustment:";
	private static final String DESCRIPTION_INTERNAL = "Internal ";
	private static final String DESCRIPTION_CASH_DIVIDEND = "Cash Dividend";

	private static final String DATA_DISCRIMINATOR_ORDER = "Order";
	private static final String ASSET_CATEGORY_STOCKS = "Stocks";
	private static final String CURRENCY_TOTAL = "Total";

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");
	private static final Pattern splitPattern = Pattern.compile("^([a-zA-Z]+?)\\([A-Za-z0-9]+?\\) Split ([0-9]+?) for ([0-9]+?) ");
	private static final Pattern dividendsPattern = Pattern.compile("^([a-zA-Z]+?) ?\\([A-Za-z0-9]+?\\) (Cash Dividend|Payment in Lieu) ");

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
		if (!line[fieldMap.get(HEADER_DATA_DISCRIMINATOR)].equals(DATA_DISCRIMINATOR_ORDER)
			|| !line[fieldMap.get(HEADER_ASSET_CATEGORY)].equals(ASSET_CATEGORY_STOCKS)) {
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

	private static Optional<Split> parseCorporateAction(String[] line, Map<String, Integer> fieldMap) {
		if (!line[fieldMap.get(HEADER_ASSET_CATEGORY)].equals(ASSET_CATEGORY_STOCKS)) {
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

	private static Optional<DepositWithdrawal> parseDepositsWithdrawals(String[] line, Map<String, Integer> fieldMap) {
		if (line[fieldMap.get(HEADER_CURRENCY)].startsWith(CURRENCY_TOTAL)
			|| line[fieldMap.get(HEADER_DESCRIPTION)].startsWith(DESCRIPTION_ADJUSTMENT)
			|| line[fieldMap.get(HEADER_DESCRIPTION)].startsWith(DESCRIPTION_INTERNAL)) {
			return Optional.empty();
		}

		var currency = Currency.valueOf(line[fieldMap.get(HEADER_CURRENCY)]);
		var date = LocalDate.parse(line[fieldMap.get(HEADER_SETTLE_DATE)], dateFormatter);
		var amount = new BigDecimal(line[fieldMap.get(HEADER_AMOUNT)]);

		return Optional.of(new DepositWithdrawal(currency, date, amount));
	}

	@FunctionalInterface
	private interface DividendEventConstructor {
		DividendEvent construct(String symbol, LocalDateTime dateTime, Currency currency, BigDecimal amount);
	}

	private static Optional<DividendEvent> parseDividendEvent(String[] line, Map<String, Integer> fieldMap,
		DividendEventConstructor constructor) {
		if (line[fieldMap.get(HEADER_CURRENCY)].startsWith(CURRENCY_TOTAL)) {
			return Optional.empty();
		}

		var matcher = dividendsPattern.matcher(line[fieldMap.get(HEADER_DESCRIPTION)]);
		if (!matcher.find()) {
			throw new UnsupportedOperationException("Unknown dividends pattern: " + Arrays.toString(line));
		}
		if (!matcher.group(2).equals(DESCRIPTION_CASH_DIVIDEND)) {
			return Optional.empty();
		}

		var currency = Currency.valueOf(line[fieldMap.get(HEADER_CURRENCY)]);
		var date = LocalDate.parse(line[fieldMap.get(HEADER_DATE)], dateFormatter);
		var amount = new BigDecimal(line[fieldMap.get(HEADER_AMOUNT)]);

		var symbol = matcher.group(1);

		return Optional.of(constructor.construct(symbol, date.atStartOfDay(), currency, amount));
	}

	private static List<ReportEntry> parseFile(Path csvFile) {
		var entries = new ArrayList<ReportEntry>();

		var fieldMaps = new HashMap<String, Map<String, Integer>>();
		try (var reader = new CSVReader(Files.newBufferedReader(csvFile))) {
			for (String[] line; (line = reader.readNext()) != null;) {
				Optional<? extends ReportEntry> entry = Optional.empty();
				switch (line[1]) {
				case LINE_HEADER -> {
					switch (line[0]) {
					case SECTION_TRADES:
					case SECTION_CORPORATE_ACTIONS:
					case SECTION_DEPOSITS_WITHDRAWALS:
					case SECTION_DIVIDENDS:
					case SECTION_WITHHOLDING_TAX:
						fieldMaps.put(line[0], genFieldMap(line));
						break;
					}
				}
				case LINE_DATA -> {
					entry = switch (line[0]) {
					case SECTION_TRADES -> parseTrade(line, fieldMaps.get(line[0]));
					case SECTION_CORPORATE_ACTIONS -> parseCorporateAction(line, fieldMaps.get(line[0]));
					case SECTION_DEPOSITS_WITHDRAWALS -> parseDepositsWithdrawals(line, fieldMaps.get(line[0]));
					case SECTION_DIVIDENDS -> parseDividendEvent(line, fieldMaps.get(line[0]), Dividends::new);
					case SECTION_WITHHOLDING_TAX -> parseDividendEvent(line, fieldMaps.get(line[0]), WithholdingTax::new);
					default -> Optional.empty();
					};
				}
				}
				entry.ifPresent(entries::add);
			}
		} catch (IOException | CsvException e) {
			throw new RuntimeException(e);
		}

		return entries;
	}

	public static List<ReportEntry> parse(List<Path> csvFiles) {
		return csvFiles.stream().flatMap(path -> parseFile(path).stream()).toList();
	}

}
