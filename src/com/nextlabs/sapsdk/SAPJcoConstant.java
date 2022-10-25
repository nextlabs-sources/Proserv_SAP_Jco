package com.nextlabs.sapsdk;

public class SAPJcoConstant {

	// obligation attributes keys
	public static final String CE_ATTR_OBLIGATION_COUNT = "CE_ATTR_OBLIGATION_COUNT";
	public static final String CE_ATTR_OBLIGATION_NAME = "CE_ATTR_OBLIGATION_NAME";
	public static final String CE_ATTR_OBLIGATION_VALUE = "CE_ATTR_OBLIGATION_VALUE";
	public static final String CE_ATTR_OBLIGATION_NUMVALUES = "CE_ATTR_OBLIGATION_NUMVALUES";
	public static final String CE_ATTR_OBLIGATION_POLICY = "CE_ATTR_OBLIGATION_POLICY";

	// grc mitigation columns
	public static final String[] MITIGATION_COLUMNS = { "RISKID", "RISKDESC",
			"ACCONTROLID", "ACCONTROLDESC", "MITMONITOR", "MITCOMMU" };
	public static final String CE_ATTR_GRC_RISK_DETAILS = "grc_risk_details";
	public static final String CE_GRC_RISK_DETAILS_OBLIGATION_START_NUMBER="grc_risk_details_obligation_start_number";
}
