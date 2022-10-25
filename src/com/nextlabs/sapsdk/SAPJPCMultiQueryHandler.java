package com.nextlabs.sapsdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.nextlabs.sapsdk.vo.CERequest;
import com.nextlabs.sapsdk.vo.CEResource;
import com.nextlabs.sapsdk.vo.CESdkException;
import com.nextlabs.sapsdk.vo.CESdkTimeoutException;
import com.nextlabs.sapsdk.vo.CEUser;
import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class SAPJPCMultiQueryHandler implements JCoServerFunctionHandler {

	private static final Log LOG = LogFactory
			.getLog(SAPJPCMultiQueryHandler.class);
	private Properties allProps;

	public SAPJPCMultiQueryHandler(Properties allProps) {
		super();
		this.allProps = allProps;
	}

	@Override
	public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
			throws AbapException, AbapClassException {

		LOG.info("SAPJPCMultiQueryHandler :: Query request received");

		long lCurrentTime = System.nanoTime();

		try {

			// Get Structure data for export
			JCoStructure importStructure = function.getImportParameterList()
					.getStructure("JPC_EXPORT_PARAMS");
			String Uname = importStructure.getString("U_NAME");
			String Uid = importStructure.getString("U_ID");
			String appName = importStructure.getString("APP_NAME");
			String appPath = importStructure.getString("APP_PATH");
			String appUrl = importStructure.getString("APP_URL");
			String host = importStructure.getString("HOST");
			int hostIP = importStructure.getInt("HOSTIP");
			int timeOut = importStructure.getInt("TIMEOUT");
			String obligationsIn = importStructure.getString("OBLIGATIONS_IN");
			int noiseLevel = importStructure.getInt("NOISELEVEL");
			int noOfRecords = importStructure.getInt("NOOFRECS");
			String stopOnFirstDeny = importStructure.getString("STOP1STDENY");

			JCoStructure importResourceStructure = function
					.getImportParameterList().getStructure(
							"JPC_RESOURCE_PARAMS");
			JCoTable resourceTable = importResourceStructure
					.getTable("SOURCE_ATTR");

			LOG.info("SAPJPCMultiQueryHandler:: Uname :" + Uname + "  Uid:  "
					+ Uid + "  AppName:  " + appName + "  AppPath:  " + appPath
					+ "  AppUrl:  " + appUrl);
			LOG.info(" Host: " + host + "  HostIP:  " + hostIP + " Timeout: "
					+ timeOut + " Records:" + noOfRecords + " StopOnDeny:"
					+ stopOnFirstDeny);
			LOG.info("SAPJPCMultiQueryHandler :: Export Params imported ");

			CEApplication application = new CEApplication(appName, appPath,
					appUrl);

			CEUser user = new CEUser(Uname, Uid);
			JCoTable userAttrTable = function.getTableParameterList().getTable(
					"USR_ATTR");
			CEAttributes userAttributes = buildAttributes(userAttrTable);

			List<CERequest> ceRequestList = new ArrayList<CERequest>();
			LOG.info("SAPJPCMultiQueryHandler :: Request Record Count: "
					+ resourceTable.getNumRows());
			for (int i = 0; i < resourceTable.getNumRows(); i++, resourceTable
					.nextRow()) {

				String action = resourceTable.getString("ACTION");
				String sourceId = resourceTable.getString("SOURCE_ID");
				String destId = resourceTable.getString("DEST_ID");
				String sDestinyType = resourceTable.getString("S_DESTINYTYPE");
				String dDestinyType = resourceTable.getString("D_DESTINYTYPE");
				JCoStructure secIdt = resourceTable.getStructure("SECIDT");
				JCoStructure secEnh = resourceTable.getStructure("SECENH");
				LOG.info("SAPJPCMultiQueryHandler:: Action:" + action
						+ "  SourceId: " + sourceId + "  Type:  "
						+ sDestinyType);

				CEResource source = new CEResource(sourceId, sDestinyType);
				JCoTable sourceAttrTable = resourceTable.getTable("ATTR");
				CEAttributes sourceAttributes = buildAttributes(sourceAttrTable);

				CEResource dest = new CEResource(destId, dDestinyType);
				if (destId == null || dDestinyType == null)
					dest = null;

				// No destination attributes coming in request for now.
				CEAttributes destAttributes = new CEAttributes();

				// handling for GRC enalbed/disabled
				String uniqueKey = UUID.randomUUID().toString();
				Map<String, Object> trackingAttrs = new HashMap<String, Object>();
				CERequest ceRequest = null;

				if (allProps.getProperty("grc_enabled", "false")
						.equalsIgnoreCase("true")) {
					LOG.info("SAPJPCMultiQueryHandler :: GRC is enabled. Adding GRC_KEY to user attributes");
					CEAttributes userAttributesGRC = buildAttributes(userAttrTable);
					userAttributesGRC.add("grcKey", uniqueKey);
					ceRequest = new CERequest(action, source, sourceAttributes,
							dest, destAttributes, user, userAttributesGRC,
							application, null, null, null, true, noiseLevel);
					trackingAttrs.put("GRCKEY", uniqueKey);
				} else {
					LOG.info("SAPJPCMultiQueryHandler :: GRC is disabled.");
					ceRequest = new CERequest(action, source, sourceAttributes,
							dest, destAttributes, user, userAttributes,
							application, null, null, null, true, noiseLevel);
				}

				trackingAttrs.put("SOURCE_ID", sourceId);
				trackingAttrs.put("SECIDT", secIdt);
				trackingAttrs.put("SECENH", secEnh);
				ceRequest.setTrackingAttrs(trackingAttrs);
				ceRequestList.add(ceRequest);
			}

			SAPCESdk sdk = new SAPCESdk();
			// Call Policy Check
			if (sdk != null) {
				LOG.info("SAPJPCMultiQueryHandler :: Enforcement Check");
				List<CEEnforcement> enforcementResults = null;
				boolean stopOnFirstDenyFlag = false;
				if (stopOnFirstDeny != null
						&& stopOnFirstDeny.equalsIgnoreCase("X")) {
					stopOnFirstDenyFlag = true;
				}
				try {
					enforcementResults = sdk.checkResources(ceRequestList,
							null, false, hostIP, timeOut, stopOnFirstDenyFlag);

				} catch (CESdkTimeoutException te) {
					LOG.error("SAPJPCMultiQueryHandler", te);
					throw new AbapException("TIMEOUT_ERROR", te.getMessage());
				} catch (CESdkException se) {
					LOG.error("SAPJPCMultiQueryHandler", se);
					throw new AbapException("PDP_ERROR", se.getMessage());
				}

				JCoStructure resultStructure = function
						.getExportParameterList().getStructure(
								"JPC_RETURN_PARAMS");
				JCoTable resultTable = resultStructure.getTable("RET_ATTR");
				JCoRecordMetaData recordMetaData = userAttrTable
						.getRecordMetaData();

				LOG.info("SAPJPCMultiQueryHandler :: Enforcement Result Count "
						+ enforcementResults.size());
				for (CEEnforcement ceEnforcement : enforcementResults) {
					String response = ceEnforcement.getResponseAsString();
					LOG.info("SAPJPCMultiQueryHandler :: Enforcement Result is "
							+ response);

					Map<String, Object> trackingAttrs = ceEnforcement
							.getTrackingAttrs();

					CEAttributes actualOblAttr = ceEnforcement.getObligations();

					String uniqueKey = (String) trackingAttrs.get("GRCKEY");
					if (allProps.getProperty("grc_enabled", "false")
							.equalsIgnoreCase("false")) {
						LOG.info("SAPJPCMultiQueryHandler :: GRC is disabled. Skip GRC process.");
					} else if (uniqueKey == null) {
						LOG.error("SAPJPCMultiQueryHandler :: Cannot get GRCKEY from tracking attributes for resource "
								+ trackingAttrs.get("SOURCE_ID"));
					} else {

						LOG.info("SAPJPCMultiQueryHandle :: GET GRCKEY from tracking attributes: "
								+ uniqueKey);
						List<CEAttribute> attributeList = actualOblAttr
								.getAttributes();

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
									LOG.info("SAPJPCMultiQueryHandler :: No obligation was triggered.");
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
									LOG.info("SAPJPCMultiQueryHandler :: "
											+ allProps
													.getProperty("grc_obligation_name")
											+ " obligation triggered.");
									obligationPosition = Integer
											.parseInt(attribute.getKey().split(
													":")[1]);
								}
							}

							// if GRC obligation is found
							if (obligationPosition != 0) {
								/*
								 * finished processing obligation
								 * arguments,checking conditions to proceed to
								 * reading GRC cache
								 */
								if (attribute
										.getKey()
										.equals(SAPJcoConstant.CE_ATTR_OBLIGATION_NUMVALUES
												+ ":" + obligationPosition)) {
									argumentsLengthKeyPosition = i;

									argumentsLength = Integer
											.parseInt(attribute.getValue());
									if (argumentsLength == 0) {
										LOG.info("SAPJPCMultiQueryHandler :: GRC Obligation was triggered with no argument. Cancel GRC Cache reading.");
										proceedToGetGRCCache = false;
									}

									if (proceedToGetGRCCache) {

										// get GRC data
										Cache grcCache = CacheManager
												.getInstance().getCache(
														"GRCCache");
										if (grcCache == null) {
											LOG.info("SAPJPCMultiQueryHandler :: GRCCache cannot be found.");
										} else {
											Element e = grcCache.get(uniqueKey);
											if (e == null) {
												LOG.info("SAPJPCMultiQueryHandler :: Mitigation list is null for key: "
														+ uniqueKey + ".");
											} else {
												LOG.info("SAPJPCMultiQueryHandler :: Got mitigation list for key: "
														+ uniqueKey + ".");

												List<HashMap<String, String>> mitigationList = (List<HashMap<String, String>>) e
														.getValue();

												String grcRiskDetails = "";
												for (HashMap<String, String> mitigation : mitigationList) {
													for (int j = 0; j < SAPJcoConstant.MITIGATION_COLUMNS.length; j++) {
														grcRiskDetails += mitigation
																.get(SAPJcoConstant.MITIGATION_COLUMNS[j])
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
												LOG.info("SAPJPCMultiQueryHandler :: risk key posistion: "
														+ obligationNo + ".");
												LOG.info("SAPJPCMultiQueryHandler :: risk value posistion: "
														+ (obligationNo + 1)
														+ ".");

												// add new CEAttribute
												CEAttribute grcRiskDetailsKey = new CEAttribute(
														SAPJcoConstant.CE_ATTR_OBLIGATION_VALUE
																+ ":"
																+ obligationPosition
																+ ":"
																+ obligationNo,
														SAPJcoConstant.CE_ATTR_GRC_RISK_DETAILS);

												CEAttribute grcRiskDetailsValue = new CEAttribute(
														SAPJcoConstant.CE_ATTR_OBLIGATION_VALUE
																+ ":"
																+ obligationPosition
																+ ":"
																+ (obligationNo + 1),
														grcRiskDetails);

												attributeList
														.add(argumentsLengthKeyPosition,
																grcRiskDetailsKey);

												attributeList
														.add(argumentsLengthKeyPosition + 1,
																grcRiskDetailsValue);

												attribute
														.setValue(new String(
																""
																		+ (argumentsLength + 2)));

												LOG.info("SAPJPCMultiQueryHandler :: New CE Attributes have been added");
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

						for (CEAttribute attr : attributeList) {
							LOG.info("SAPJPCMultiQueryHandler :: "
									+ attr.getKey() + " -- " + attr.getValue());
						}
					}

					JCoTable resultAttrTable = JCo.createTable(recordMetaData);
					int vOdd = 1;
					for (String s : actualOblAttr.toArray()) {
						if (vOdd % 2 == 0)
							resultAttrTable.setValue("VALUE", s);
						else {
							resultAttrTable.appendRow();
							resultAttrTable.setValue("KEY", s);
						}
						vOdd = vOdd + 1;
					}

					resultTable.appendRow();
					resultTable.setValue("RESULT", ""); // ???
					resultTable.setValue("RESPONSE", response);
					resultTable.setValue("ATTR", resultAttrTable);
					if (trackingAttrs != null) {
						resultTable.setValue("SECIDT",
								trackingAttrs.get("SECIDT"));
						resultTable.setValue("SECENH",
								trackingAttrs.get("SECENH"));
						resultTable.setValue("SOURCE_ID",
								trackingAttrs.get("SOURCE_ID"));
					}
					LOG.info("SAPJPCMultiQueryHandler :: SOURCE_ID:"
							+ trackingAttrs.get("SOURCE_ID") + " :: RESPONSE:"
							+ response);
				}

				resultStructure.setValue("RET_ATTR", resultTable);
				LOG.info("SAPJPCMultiQueryHandler :: End of SAPCESdk Query Handling");
				// end core logic
			}
		} catch (AbapException ae) {
			throw ae;
		} catch (Exception e) {
			LOG.error("SAPJPCMultiQueryHandler ::", e);
			throw new AbapException("RUNTIME_ERROR", e.getMessage());
		}

		LOG.debug("SAPJPCMultiQueryHandler :: handleRequest() completed. Time spent: "
				+ ((System.nanoTime() - lCurrentTime) / 1000000.00) + "ms");
	}

	private CEAttributes buildAttributes(JCoTable table) {
		CEAttributes attributes = new CEAttributes();
		if (table != null) {
			LOG.info("Size of table is ::" + table.getNumRows());
			table.firstRow();
			for (int i = 0; i < table.getNumRows(); i++, table.nextRow()) {
				String dataKey = table.getString("KEY");
				String dataVal = table.getString("VALUE");
				LOG.info("SAPJPCMultiQueryHandler :: KEY :" + dataKey
						+ " :: VALUE:" + dataVal);
				attributes.add(dataKey, dataVal);
			}
		}
		return attributes;
	}

}
