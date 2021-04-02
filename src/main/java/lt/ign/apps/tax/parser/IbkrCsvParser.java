package lt.ign.apps.tax.parser;

import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;

import lt.ign.apps.tax.model.Currency;
import lt.ign.apps.tax.model.Trade;

public class IbkrCsvParser {

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

	private final List<String> paths;

	private List<Trade> trades;

	public IbkrCsvParser(List<String> paths) {
		this.paths = paths;
	}

	private Trade.Type parseTradeType(String str) {
		switch (str) {
		case "O":
			return Trade.Type.OPEN;
		case "C":
			return Trade.Type.CLOSE;
		default:
			throw new IllegalArgumentException();
		}
	}

	public void parse() {
		trades = new ArrayList<>();
		try {
			for (String path : paths) {
				try (CSVReader reader = new CSVReader(new FileReader(path))) {
					String[] line;
					while ((line = reader.readNext()) != null) {
						if (line.length != 17) continue;
						if (!line[0].equals("Trades")) continue;
						if (!line[1].equals("Data")) continue;
						if (!line[2].equals("Order")) continue;
						if (!line[3].equals("Stocks")) continue;

						var trade = new Trade();
						trade.setCurrency(Currency.valueOf(line[4]));
						trade.setSymbol(line[5]);
						trade.setDateTime(LocalDateTime.parse(line[6], dateTimeFormatter));
						trade.setQuantity(Integer.parseInt(line[7]));
						trade.setProceeds(new BigDecimal(line[10]));
						trade.setFees(new BigDecimal(line[11]));
						trade.setType(parseTradeType(line[16]));
						trades.add(trade);

						// TODO adjust for stock splits
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<Trade> getTrades() {
		return trades;
	}

}
