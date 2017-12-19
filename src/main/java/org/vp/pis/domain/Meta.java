package org.vp.pis.domain;

public class Meta {
	
	private int TotalPages;
	private String FirstAvailableDateTime;
	private String LastAvailableDateTime;

	public int getTotalPages() {
		return TotalPages;
	}

	public void setTotalPages(int totalPages) {
		TotalPages = totalPages;
	}

	public String getFirstAvailableDateTime() {
		return FirstAvailableDateTime;
	}

	public void setFirstAvailableDateTime(String firstAvailableDateTime) {
		FirstAvailableDateTime = firstAvailableDateTime;
	}

	public String getLastAvailableDateTime() {
		return LastAvailableDateTime;
	}

	public void setLastAvailableDateTime(String lastAvailableDateTime) {
		LastAvailableDateTime = lastAvailableDateTime;
	}

}
