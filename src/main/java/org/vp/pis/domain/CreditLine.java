package org.vp.pis.domain;

public class CreditLine {
	private String Included;
	private Amount Amount;
	private String Type;
	public String getIncluded() {
		return Included;
	}
	public void setIncluded(String included) {
		Included = included;
	}
	public Amount getAmount() {
		return Amount;
	}
	public void setAmount(Amount amount) {
		Amount = amount;
	}
	public String getType() {
		return Type;
	}
	public void setType(String type) {
		Type = type;
	}

}
