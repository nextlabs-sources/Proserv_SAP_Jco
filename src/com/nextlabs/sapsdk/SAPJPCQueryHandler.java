package com.nextlabs.sapsdk;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.sapsdk.vo.CEApplication;
import com.nextlabs.sapsdk.vo.CEAttribute;
import com.nextlabs.sapsdk.vo.CEAttributes;
import com.nextlabs.sapsdk.vo.CEEnforcement;
import com.nextlabs.sapsdk.vo.CEResource;
import com.nextlabs.sapsdk.vo.CESdkException;
import com.nextlabs.sapsdk.vo.CESdkTimeoutException;
import com.nextlabs.sapsdk.vo.CEUser;
import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class SAPJPCQueryHandler implements JCoServerFunctionHandler {

	private static final Log LOG = LogFactory.getLog(SAPJPCQueryHandler.class);
	private Properties allProps;

	public SAPJPCQueryHandler(Properties allProps) {
		super();
		this.allProps = allProps;
	}

	@Override
	public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
			throws AbapException, AbapClassException {

		String response = "";
		String result = "";
		LOG.info("SAPJPCQueryHandler :: Query request received");

		// Get Structure data for export
		JCoStructure importStructure = function.getImportParameterList()
				.getStructure("JPC_EXPORT_PARAM");
		String Uname = importStructure.getString("U_NAME");
		String Uid = importStructure.getString("U_ID");
		String appName = importStructure.getString("APP_NAME");
		String appPath = importStructure.getString("APP_PATH");
		String appUrl = importStructure.getString("APP_URL");
		String host = importStructure.getString("HOST"); // What is this used
															// for?
		int hostIP = importStructure.getInt("HOSTIP");
		int timeOut = importStructure.getInt("TIMEOUT");
		String action = importStructure.getString("ACTION");
		String sourceId = importStructure.getString("SOURCE_ID");
		String destId = importStructure.getString("DEST_ID");
		String sDestinyType = importStructure.getString("S_DESTINYTYPE");
		String dDestinyType = importStructure.getString("D_DESTINYTYPE");
		// What is this used for?
		String obligationsIn = importStructure.getString("OBLIGATIONS_IN");
		int noiseLevel = importStructure.getInt("NOISELEVEL");

		JCoTable outTable = function.getTableParameterList().getTable(
				SAPJCoServerRegistration.RST_ATTR);

		long h = System.currentTimeMillis();
		LOG.info("SAPJPCQueryHandler :: Enforcement handle :" + h);
		LOG.info("SAPJPCQueryHandler:: Values imported are- Uname :" + Uname
				+ "  Uid:  " + Uid + "  AppName:  " + appName + "  AppPath:  "
				+ appPath + "  AppUrl:  " + appUrl);
		LOG.info(" Host: " + host + "  HostIP:  " + hostIP + " Timeout: "
				+ timeOut + " Source id: " + sourceId + " DestId: " + destId
				+ " sDestType:  " + sDestinyType);
		LOG.info("SAPJPCQueryHandler :: Export Params imported ");

		if (destId == null || destId.trim().length() <= 0)
			destId = null;

		if (dDestinyType == null || dDestinyType.trim().length() <= 0)
			dDestinyType = null;

		try {
			// Declare Variable for CESDK or PEP API
			CEApplication application = new CEApplication(appName, appPath,
					appUrl);

			CEUser user = new CEUser(Uname, Uid);
			CEAttributes userAttributes = SAPJCoServerRegistration
					.buildAttributes(function,
							SAPJCoServerRegistration.USR_ATTR);
			String uniqueKey = UUID.randomUUID().toString();

			if (allProps.getProperty("grc_enabled", "false").equalsIgnoreCase(
					"true")) {
				LOG.info("SAPJPCQueryHandler :: GRC is enabled. Adding GRC_KEY to user attributes");
				userAttributes.add("grcKey", uniqueKey);
			} else {
				LOG.info("SAPJPCQueryHandler :: GRC is disabled.");
			}

			CEResource source = new CEResource(sourceId, sDestinyType);
			CEResource dest = new CEResource(destId, dDestinyType);
			if (destId == null || dDestinyType == null)
				dest = null;
			CEAttributes source_attributes = SAPJCoServerRegistration
					.buildAttributes(function,
							SAPJCoServerRegistration.SRC_ATTR);
			CEAttributes dest_attributes = SAPJCoServerRegistration
					.buildAttributes(function,
							SAPJCoServerRegistration.DST_ATTR);

			SAPCESdk sdk = new SAPCESdk();

			// Call Policy Check
			if (sdk != null) {
				LOG.info("SAPJPCQueryHandler :: Enforcement Check, with handle :"
						+ h);
				CEEnforcement enforcementResult = null;
				try {
					enforcementResult = sdk.checkResources(action, source,
							source_attributes, dest, dest_attributes, user,
							userAttributes, application, null, null, null,
							hostIP,// 0,//i_hostip, // ip address,
							true, // perform obligations
							noiseLevel, timeOut);
				} catch (CESdkTimeoutException te) {
					LOG.error("SAPJPCQueryHandler", te);
					throw new AbapException("TIMEOUT_ERROR", te.getMessage());
				} catch (CESdkException se) {
					LOG.error("SAPJPCQueryHandler", se);
					throw new AbapException("PDP_ERROR", se.getMessage());
				}

				response = enforcementResult.getResponseAsString();
				LOG.info("SAPJPCQueryHandler :: Enforcement Result is "
						+ response);

				CEAttributes actualOblAttr = enforcementResult.getObligations();
				List<CEAttribute> attributeList = actualOblAttr.getAttributes();

				if (allProps.getProperty("grc_enabled", "false")
						.equalsIgnoreCase("true")) {

					int obligationPosition = 0;
					int argumentsLength = 0;
					int argumentsLengthKeyPosition = 0;
					boolean proceedToGetGRCCache = true;

					for (int i = 0; i < attributeList.size(); i++) {
						CEAttribute attribute = attributeList.get(i);

						// if no obligation was triggered
						if (attribute.getKey().equals(
								SAPJcoConstant.CE_ATTR_OBLIGATION_COUNT)) {
							if (Integer.parseInt(attribute.getValue()) < 1) {
								LOG.info("SAPJPCQueryHandler :: No obligation was triggered.");
								break;
							}
						}

						/* find the GRC obligation and record its position */
						if (attribute.getKey().contains(
								(SAPJcoConstant.CE_ATTR_OBLIGATION_NAME))) {
							if (attribute
									.getValue()
									.equals(allProps
											.getProperty("grc_obligation_name"))) {
								LOG.info("SAPJPCQueryHandler :: "
										+ allProps
												.getProperty("grc_obligation_name")
										+ " obligation triggered.");
								obligationPosition = Integer.parseInt(attribute
										.getKey().split(":")[1]);
							}
						}

						// if GRC obligation is found
						if (obligationPosition != 0) {
							/*
							 * finished processing obligation arguments,checking
							 * conditions to proceed to reading GRC cache
							 */
							if (attribute.getKey().equals(
									SAPJcoConstant.CE_ATTR_OBLIGATION_NUMVALUES
											+ ":" + obligationPosition)) {
								argumentsLengthKeyPosition = i;

								argumentsLength = Integer.parseInt(attribute
										.getValue());
								if (argumentsLength == 0) {
									LOG.info("SAPJPCQueryHandler :: GRC Obligation was triggered with no argument. Cancel GRC Cache reading.");
									proceedToGetGRCCache = false;
								}

								if (proceedToGetGRCCache) {

									// get GRC data
									Cache grcCache = CacheManager.getInstance()
											.getCache("GRCCache");
									if (grcCache == null) {
										LOG.info("SAPJPCQueryHandler :: GRCCache cannot be found.");
									} else {
										Element e = grcCache.get(uniqueKey);
										if (e == null) {
											LOG.info("SAPJPCQueryHandler :: Mitigation list is null for key: "
													+ uniqueKey + ".");
										} else {
											LOG.info("SAPJPCQueryHandler :: Got mitigation list for key: "
													+ uniqueKey + ".");

											List<HashMap<String, String>> mitigationList = (List<HashMap<String, String>>) e
													.getValue();

											String grcRiskDetails = "";
											for (HashMap<String, String> mitigation : mitigationList) {
												int grcObligationListCounter = 0;
												for ( grcObligationListCounter = 0; grcObligationListCounter < SAPJcoConstant.MITIGATION_COLUMNS.length; grcObligationListCounter++) {
													grcRiskDetails += mitigation
															.get(SAPJcoConstant.MITIGATION_COLUMNS[grcObligationListCounter])
															+ "||";

												}
												grcRiskDetails = grcRiskDetails
														.substring(
																0,
																grcRiskDetails
																		.length() - 2);
												grcRiskDetails += "##";
											}

											grcRiskDetails = grcRiskDetails
													.substring(
															0,
															grcRiskDetails
																	.length() - 2);
											String obligationNumber = allProps
													.getProperty(SAPJcoConstant.CE_GRC_RISK_DETAILS_OBLIGATION_START_NUMBER);
											int obligationNo = 7;
											if (obligationNumber != null) {
												obligationNo = Integer
														.parseInt(obligationNumber);
											}
											LOG.info("SAPJPCQueryHandler :: risk key posistion: "
													+ obligationNo + ".");
											LOG.info("SAPJPCQueryHandler :: risk value posistion: "
													+ (obligationNo + 1)
													+ ".");
										
											// add new CEAttribute
											CEAttribute grcRiskDetailsKey = new CEAttribute(
													SAPJcoConstant.CE_ATTR_OBLIGATION_VALUE
															+ ":"
															+ obligationPosition
															+ ":" + obligationNo,
													SAPJcoConstant.CE_ATTR_GRC_RISK_DETAILS);

											CEAttribute grcRiskDetailsValue = new CEAttribute(
													SAPJcoConstant.CE_ATTR_OBLIGATION_VALUE
															+ ":"
															+ obligationPosition
															+ ":" + (obligationNo+1),
													grcRiskDetails);

											attributeList.add(
													argumentsLengthKeyPosition,
													grcRiskDetailsKey);

											attributeList
													.add(argumentsLengthKeyPosition + 1,
															grcRiskDetailsValue);

											attribute.setValue(new String(""
													+ (argumentsLength + 2)));
										}
									}
								}

								// reset obligation records to proceed

								obligationPosition = 0;
								argumentsLength = 0;
								argumentsLengthKeyPosition = 0;
								proceedToGetGRCCache = true;
							}
						}
					}

					actualOblAttr.setAttributes(attributeList);
				}

				int vOdd = 1;
				for (String s : actualOblAttr.toArray()) {
					LOG.info("Get from obligations: " + s);
					if (vOdd % 2 == 0)
						outTable.setValue(SAPJCoServerRegistration.VALUE, s);
					else {
						outTable.appendRow();
						outTable.setValue(SAPJCoServerRegistration.KEY, s);
					}
					vOdd = vOdd + 1;
				}
				LOG.info("SAPJPCQueryHandler :: End of SAPCESdk Query Handling");
				// end core logic
			}
		} catch (AbapException ae) {
			throw ae;
		} catch (Exception e) {
			LOG.error("SAPJPCQueryHandler", e);
			throw new AbapException("RUNTIME_ERROR", e.getMessage());
		}
		// Get Export Data
		JCoStructure exportStructure = function.getExportParameterList()
				.getStructure("JPC_IMPORT_PARAM");
		exportStructure.setValue("RESULT", result);// TODO what to set here
		exportStructure.setValue("RESPONSE", response);
		LOG.info("SAPJPCQueryHandler :: Result" + result + "Response"
				+ response);
		// end send output

	}
}
