package lt.ign.apps.tax.printer;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

import lt.ign.apps.tax.model.event.DepositWithdrawal;

public class DepositsWithdrawalsPrinter {

	private final List<DepositWithdrawal> depositsWithdrawals;

	public DepositsWithdrawalsPrinter(List<DepositWithdrawal> depositsWithdrawals) {
		this.depositsWithdrawals = depositsWithdrawals;
	}

	public void print(PrintStream ps) {
		ps.println("====================================================================================================");
		ps.println("Deposits & Withdrawals");
		ps.println("----------------------------------------------------------------------------------------------------");
		int[] lastYear = { 0 };
		depositsWithdrawals.stream().sorted(Comparator.comparing(DepositWithdrawal::getDate)).forEach(dw -> {
			var curYear = dw.getDate().getYear();
			if (lastYear[0] != 0 && curYear != lastYear[0]) {
				ps.println("----------------------------------------------------------------------------------------------------");
			}
			ps.println(dw.toString());
			lastYear[0] = curYear;
		});
		ps.println("====================================================================================================");
	}

}
