package org.vp.pis.domain;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Data {
	
	private ArrayList<Accounts> Account;
	
	private ArrayList<Balance> Balance;
	
	private ArrayList<Beneficiary> Beneficiary;
	
	private ArrayList<DirectDebit> DirectDebit;
	
	private ArrayList<Product> Product;
	
	private ArrayList<StandingOrder> StandingOrder;
	
	private ArrayList<Transaction> Transaction;
	
	private String AccountRequestId;
	
	private String Status;
	
	private String CreationDateTime;
	
	@JsonProperty("Permissions")
	private String[] Permissions;
	
	@JsonProperty("ExpirationDateTime")
	private String ExpirationDateTime;

	@JsonProperty("TransactionFromDateTime")
    private String TransactionFromDateTime;

	@JsonProperty("TransactionToDateTime")
    private String TransactionToDateTime;

	public ArrayList<Beneficiary> getBeneficiary() {
		return Beneficiary;
	}
	
	public void setBeneficiary(ArrayList<Beneficiary> beneficiary) {
		Beneficiary = beneficiary;
	}

	public ArrayList<DirectDebit> getDirectDebit() {
		return DirectDebit;
	}

	public void setDirectDebit(ArrayList<DirectDebit> directDebit) {
		DirectDebit = directDebit;
	}

	public ArrayList<Product> getProduct() {
		return Product;
	}

	public void setProduct(ArrayList<Product> product) {
		Product = product;
	}

	public ArrayList<StandingOrder> getStandingOrder() {
		return StandingOrder;
	}

	public void setStandingOrder(ArrayList<StandingOrder> standingOrder) {
		StandingOrder = standingOrder;
	}

	public ArrayList<Transaction> getTransaction() {
		return Transaction;
	}

	public void setTransaction(ArrayList<Transaction> transaction) {
		Transaction = transaction;
	}

	public ArrayList<Accounts> getAccount() {
		return Account;
	}

	public void setAccount(ArrayList<Accounts> account) {
		Account = account;
	}

	public ArrayList<Balance> getBalance() {
		return Balance;
	}

	public void setBalance(ArrayList<Balance> balance) {
		Balance = balance;
	}

	public String[] getPermissions() {
		return Permissions;
	}

	public void setPermissions(String[] permissions) {
		Permissions = permissions;
	}

	public String getExpirationDateTime() {
		return ExpirationDateTime;
	}

	public void setExpirationDateTime(String expirationDateTime) {
		ExpirationDateTime = expirationDateTime;
	}

	public String getTransactionFromDateTime() {
		return TransactionFromDateTime;
	}

	public void setTransactionFromDateTime(String transactionFromDateTime) {
		TransactionFromDateTime = transactionFromDateTime;
	}

	public String getTransactionToDateTime() {
		return TransactionToDateTime;
	}

	public void setTransactionToDateTime(String transactionToDateTime) {
		TransactionToDateTime = transactionToDateTime;
	}

	public String getAccountRequestId() {
		return AccountRequestId;
	}

	public void setAccountRequestId(String accountRequestId) {
		AccountRequestId = accountRequestId;
	}

	public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		Status = status;
	}

	public String getCreationDateTime() {
		return CreationDateTime;
	}

	public void setCreationDateTime(String creationDateTime) {
		CreationDateTime = creationDateTime;
	}


}
