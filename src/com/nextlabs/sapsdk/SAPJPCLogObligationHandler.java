package com.nextlabs.sapsdk;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.sapsdk.vo.CEAttributes;
import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class SAPJPCLogObligationHandler implements JCoServerFunctionHandler {

	private static final Log LOG = LogFactory.getLog(SAPJPCLogObligationHandler.class);
	private Properties allProps;

	public SAPJPCLogObligationHandler(Properties allProps) {
		super();
		this.allProps = allProps;
	}

	@Override
	public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
			throws AbapException, AbapClassException {

		LOG.info("SAPJPCLogObligationHandler :: Log Obligation request received");

		// Get Structure data for export
		JCoStructure importStructure = function.getImportParameterList()
				.getStructure("JPC_EXPORT_PARAM");
		String Uname = importStructure.getString("U_NAME");
		String Uid = importStructure.getString("U_ID");
		String appName = importStructure.getString("APP_NAME");
		String appPath = importStructure.getString("APP_PATH");
		String appUrl = importStructure.getString("APP_URL");
		int timeOut = importStructure.getInt("TIMEOUT");
		String logId = importStructure.getString("LOG_ID");
		String logName = importStructure.getString("LOG_NAME");
		LOG.info("SAPJPCLogObligationHandler :: Export Params imported ");

		LOG.info("SAPJPCLogObligationHandler:: Values imported are-Uname :"
				+ Uname + "Uid :" + Uid + " AppName: " + appName + " AppPath: "
				+ appPath + " AppUrl: " + appUrl);
		LOG.info(" Timeout: " + timeOut);
		LOG.info("SAPJPCLogObligationHandler :: Export Params imported ");

		try {
			CEAttributes logAttributes = SAPJCoServerRegistration
					.buildAttributes(function,
							SAPJCoServerRegistration.LOG_ATTR);
			SAPCESdk sdk = new SAPCESdk();
			// Call Log Obligation
			if (sdk != null) {
				sdk.logObligationData(logId, logName, logAttributes);
				LOG.info("SAPJPCLogObligationHandler ::LogObligation logged");

			}
		} catch (Exception e) {
			LOG.error("SAPJPCLogObligationHandler ::Exception Caught ", e);
		}
	}

}
