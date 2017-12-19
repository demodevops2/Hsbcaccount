package org.vp.pis.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountMain {
	
	@JsonProperty("Data")
	private Data Data;
	@JsonProperty("Risk")
	private Risk Risk;
	private String Status;
	private Links Links;
	private Meta Meta;

	@Override
	public String toString() {
		return "AccountMain [data=" + Data + "]";
	}

	public Links getLinks() {
		return Links;
	}

	public void setLinks(Links links) {
		Links = links;
	}

	public Meta getMeta() {
		return Meta;
	}

	public void setMeta(Meta meta) {
		Meta = meta;
	}

	public Data getData() {
		return Data;
	}

	public void setData(Data data) {
		Data = data;
	}

	public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		Status = status;
	}

	public Risk getRisk() {
		return Risk;
	}

	public void setRisk(Risk risk) {
		Risk = risk;
	}

}
