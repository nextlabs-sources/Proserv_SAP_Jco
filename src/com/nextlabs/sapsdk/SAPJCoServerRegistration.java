package com.nextlabs.sapsdk;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.crypt.IDecryptor;
import com.bluejungle.framework.crypt.ReversibleEncryptor;
import com.nextlabs.sapsdk.vo.CEAttributes;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class SAPJCoServerRegistration {

	public static final String SRC_ATTR = "SRC_ATTR";
	public static final String DST_ATTR = "DST_ATTR";
	public static final String RST_ATTR = "RST_ATTR";
	public static final String LOG_ATTR = "LOG_ATTR";
	public static final String USR_ATTR = "USR_ATTR";
	public static final String KEY = "KEY";
	public static final String VALUE = "VALUE";

	private static String DEST = "DEST";
	private static String SERV = "SERV";

	private static IDecryptor decryptor = new ReversibleEncryptor();

	private static final Log LOG = LogFactory.getLog(SAPJCoServerRegistration.class);

	static void setupServer(Properties allProps) {

		LOG.info("SAPJCoServerRegistration :: setupServer() started");
		String[] serv_prefixes = allProps.getProperty("server_prefix").split(";");
		int noOfServers = serv_prefixes.length;
		LOG.info("No of Server instances to be created : " + noOfServers);
		JCoServer[] servers = new JCoServer[noOfServers];

		for (int i = 0; i < noOfServers; i++) {
			String serverName = serv_prefixes[i];
			try {
				LOG.info("SAPJCoServerRegistration :: Setting up server - " + serverName);

				servers[i] = JCoServerFactory.getServer(serverName);

				// For Query
				LOG.info("SAPJCoServerRegistration :: Creating Query Handler object for " + serverName);
				JCoServerFunctionHandler queryHandler = new SAPJPCQueryHandler(allProps);

				// For Multi-Query
				LOG.info("SAPJCoServerRegistration :: Creating Multi Query Handler object for " + serverName);
				JCoServerFunctionHandler multiQueryHandler = new SAPJPCMultiQueryHandler(allProps);

				// For LogObligation
				LOG.info("SAPJCoServerRegistration :: Creating Log Handler object for " + serverName);
				JCoServerFunctionHandler logobglHandler = new SAPJPCLogObligationHandler(allProps);

				DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();

				factory.registerHandler(allProps.getProperty("jpc_query_handler"), queryHandler);
				factory.registerHandler(allProps.getProperty("jpc_query_mval_handler"), multiQueryHandler);
				factory.registerHandler(allProps.getProperty("jpc_logoblg_handler"), logobglHandler);
				servers[i].setCallHandlerFactory(factory);

				servers[i].setRepository(JCoDestinationManager.getDestination(serverName).getRepository());
				servers[i].start();

				LOG.info("SAPJCoServerRegistration :: Server - " + serverName + " started ");

			} catch (JCoException e) {
				LOG.error("Unable to create the server " + serverName + " because of error", e);
			} catch (Exception ee) {
				LOG.error("Unable to create the server " + serverName + " because of error", ee);
			}
		}
	}

	static void setupDataProviders(Properties allProps) {
		LOG.info("SAPJCoServerRegistration :: setupDataProviders() started");
		SAPJCoDestinationDataProvider destinationProvider = new SAPJCoDestinationDataProvider();
		SAPJCoServerDataProvider serverProvider = new SAPJCoServerDataProvider();

		String[] serv_prefixes = allProps.getProperty("server_prefix").split(";");
		LOG.info("SAPJCoServerRegistration :: setupDataProviders() Server Count :" + serv_prefixes.length);

		for (int i = 0; i < serv_prefixes.length; i++) {
			String serverName = serv_prefixes[i];
			LOG.info("SAPJcoServerRegistration :: setupDataProviders() :: processing server " + serverName);
			destinationProvider.changePropertiesForABAP_AS(serverName, getPropertySet(serverName, allProps, DEST));
			serverProvider.changePropertiesForABAP_AS(serverName, getPropertySet(serverName, allProps, SERV));
		}

		Environment.registerDestinationDataProvider(destinationProvider);
		Environment.registerServerDataProvider(serverProvider);
		LOG.info("SAPJCoServerRegistration :: setupDataProviders() completed");
	}

	private static Properties getPropertySet(String server, Properties allProps, String type) {
		Properties dataProps = new Properties();
		for (String key : allProps.stringPropertyNames()) {
			if (key.startsWith(server)) {
				String actualKey = key.substring(server.length());
				if (!actualKey.startsWith("jco.server.") && type.equals(DEST)) {
					if (actualKey.indexOf("passwd") == -1) {
						//Move to inner to avoid printout password
						dataProps.setProperty(actualKey, allProps.getProperty(key));
						LOG.info("SAPJcoServerRegistration :: getPropertySet() for server + " + server
								+ " : Destination Provider -> put Key = " + actualKey + " - Value = "
								+ dataProps.getProperty(actualKey));
					} else {
						dataProps.setProperty(actualKey, decryptor.decrypt(allProps.getProperty(key)));
					}
				}
				if (actualKey.startsWith("jco.server.") && type.equals(SERV)) {
					if (actualKey.indexOf("passwd") == -1) {
						dataProps.setProperty(actualKey, allProps.getProperty(key));
						//Move to inner to avoid printout password
						LOG.info("SAPJcoServerRegistration :: getPropertySet() for server + " + server
								+ " : Server Provider -> put Key = " + actualKey + " - Value = "
								+ dataProps.getProperty(actualKey));
					} else {
						dataProps.setProperty(actualKey, decryptor.decrypt(allProps.getProperty(key)));
					}
					
				}

			}
		}
		return dataProps;
	}

	public static CEAttributes buildAttributes(JCoFunction function, String type) {

		LOG.info("SAPJPCQueryHandler :: Fetching attributes for " + type);
		CEAttributes attributes = new CEAttributes();
		JCoTable attr_table = function.getTableParameterList().getTable(type);
		LOG.info("Size of table" + type + " is " + attr_table.getNumRows());
		attr_table.firstRow();
		for (int i = 0; i < attr_table.getNumRows(); i++, attr_table.nextRow()) {
			String dataKey = attr_table.getString(KEY);
			LOG.info("SAPJPCQueryHandler :: " + KEY + ":" + dataKey);
			String dataVal = attr_table.getString(VALUE);
			LOG.info("SAPJPCQueryHandler :: " + VALUE + ":" + dataVal);
			attributes.add(dataKey, dataVal);
		}
		return attributes;
	}

}
