package org.vp.pis.domain;

public class DirectDebit {
	
	private String AccountId;
	private String DirectDebitId;
	private String MandateIdentification;
	private String DirectDebitStatusCode;
	private String Name;
	private String PreviousPaymentDateTime;
	private PreviousPaymentAmount PreviousPaymentAmount;
	
	public String getAccountId() {
		return AccountId;
	}
	public void setAccountId(String accountId) {
		AccountId = accountId;
	}
	public String getDirectDebitId() {
		return DirectDebitId;
	}
	public void setDirectDebitId(String directDebitId) {
		DirectDebitId = directDebitId;
	}
	public String getMandateIdentification() {
		return MandateIdentification;
	}
	public void setMandateIdentification(String mandateIdentification) {
		MandateIdentification = mandateIdentification;
	}
	public String getDirectDebitStatusCode() {
		return DirectDebitStatusCode;
	}
	public void setDirectDebitStatusCode(String directDebitStatusCode) {
		DirectDebitStatusCode = directDebitStatusCode;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getPreviousPaymentDateTime() {
		return PreviousPaymentDateTime;
	}
	public void setPreviousPaymentDateTime(String previousPaymentDateTime) {
		PreviousPaymentDateTime = previousPaymentDateTime;
	}
	public PreviousPaymentAmount getPreviousPaymentAmount() {
		return PreviousPaymentAmount;
	}
	public void setPreviousPaymentAmount(PreviousPaymentAmount previousPaymentAmount) {
		PreviousPaymentAmount = previousPaymentAmount;
	}

}
