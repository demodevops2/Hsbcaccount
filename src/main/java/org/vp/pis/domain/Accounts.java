package org.vp.pis.domain;


public class Accounts {
	
	private String AccountId;
	private String Currency;
	private String Nickname;
	private Account Account;
	private Servicer Servicer;
	
	public String getAccountId() {
		return AccountId;
	}
	public void setAccountId(String accountId) {
		AccountId = accountId;
	}
	public String getCurrency() {
		return Currency;
	}
	public void setCurrency(String currency) {
		Currency = currency;
	}
	public String getNickname() {
		return Nickname;
	}
	public void setNickname(String nickname) {
		Nickname = nickname;
	}
	@Override
	public String toString() {
		return "Accounts [AccountId=" + AccountId + ", Currency=" + Currency + ", Nickname=" + Nickname + ", account="
				+ Account + ", servicer=" + Servicer + "]";
	}
	public Account getAccount() {
		return Account;
	}
	public void setAccount(Account account) {
		Account = account;
	}
	public Servicer getServicer() {
		return Servicer;
	}
	public void setServicer(Servicer servicer) {
		Servicer = servicer;
	}
}
