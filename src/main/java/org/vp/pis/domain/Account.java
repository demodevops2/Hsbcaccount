package org.vp.pis.domain;

public class Account {
	
	private String SchemeName;
	private String Identification;
	private String Name;
	private String SecondaryIdentification;
	
	public String getSchemeName() {
		return SchemeName;
	}
	public void setSchemeName(String schemeName) {
		SchemeName = schemeName;
	}
	public String getIdentification() {
		return Identification;
	}
	public void setIdentification(String identification) {
		Identification = identification;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getSecondaryIdentification() {
		return SecondaryIdentification;
	}
	public void setSecondaryIdentification(String secondaryIdentification) {
		SecondaryIdentification = secondaryIdentification;
	}
	@Override
	public String toString() {
		return "Account [SchemeName=" + SchemeName + ", Identification=" + Identification + ", Name=" + Name
				+ ", SecondaryIdentification=" + SecondaryIdentification + "]";
	}

}
