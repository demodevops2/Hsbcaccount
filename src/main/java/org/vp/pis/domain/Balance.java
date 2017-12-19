package org.vp.pis.domain;

import java.util.ArrayList;

public class Balance {
	private String AccountId;
	private Amount Amount;
	private String CreditDebitIndicator;
	private String Type;
	private String DateTime;
	private ArrayList<CreditLine> CreditLine;
	private String Currency;
	
	public String getAccountId() {
		return AccountId;
	}
	public void setAccountId(String accountId) {
		AccountId = accountId;
	}
	public Amount getAmount() {
		return Amount;
	}
	public void setAmount(Amount amount) {
		Amount = amount;
	}
	public String getCreditDebitIndicator() {
		return CreditDebitIndicator;
	}
	public void setCreditDebitIndicator(String creditDebitIndicator) {
		CreditDebitIndicator = creditDebitIndicator;
	}
	public String getType() {
		return Type;
	}
	public void setType(String type) {
		Type = type;
	}
	
	public String getDateTime() {
		return DateTime;
	}
	public void setDateTime(String dateTime) {
		DateTime = dateTime;
	}
	public String getCurrency() {
		return Currency;
	}
	public void setCurrency(String currency) {
		Currency = currency;
	}
	public ArrayList<CreditLine> getCreditLine() {
		return CreditLine;
	}
	public void setCreditLine(ArrayList<CreditLine> creditLine) {
		CreditLine = creditLine;
	}

}
