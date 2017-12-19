package org.vp.pis.domain;

public class Beneficiary {
	
	private String AccountId;
	private String BeneficiaryId;
	private String Reference;
	private Servicer Servicer;
	private CreditorAccount CreditorAccount;
	
	
	public String getAccountId() {
		return AccountId;
	}
	public void setAccountId(String accountId) {
		AccountId = accountId;
	}
	public String getBeneficiaryId() {
		return BeneficiaryId;
	}
	public void setBeneficiaryId(String beneficiaryId) {
		BeneficiaryId = beneficiaryId;
	}
	public String getReference() {
		return Reference;
	}
	public void setReference(String reference) {
		Reference = reference;
	}
	public CreditorAccount getCreditorAccount() {
		return CreditorAccount;
	}
	public void setCreditorAccount(CreditorAccount creditorAccount) {
		CreditorAccount = creditorAccount;
	}
	public Servicer getServicer() {
		return Servicer;
	}
	public void setServicer(Servicer servicer) {
		Servicer = servicer;
	}
}
