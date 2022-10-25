package com.nextlabs.sapsdk;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;

public class SAPJCoServerDataProvider implements ServerDataProvider {

	private static final Log LOG = LogFactory
			.getLog(SAPJCoServerDataProvider.class);
	private ServerDataEventListener eventListener;
	private Map<String, Properties> ABAP_AS_properties_map = new HashMap<String, Properties>();

	@Override
	public Properties getServerProperties(String server) {
		return ABAP_AS_properties_map.get(server);
	}

	@Override
	public void setServerDataEventListener(ServerDataEventListener eventListener) {

	}

	@Override
	public boolean supportsEvents() {
		return true;
	}

	public void changePropertiesForABAP_AS(String server, Properties properties) {
		LOG.info("SAPJavaSDK ::  ServerDataProvider changePropertiesForABAP_AS() for server "
				+ server);
		if (properties == null) {
			eventListener.deleted(server);
			ABAP_AS_properties_map.remove(server);
			LOG.info("SAPJavaSDK :: ServerDataProvider changePropertiesForABAP_AS() :: properties = null for server  "
					+ server + "-> server deleted");
		} else {
			if (ABAP_AS_properties_map.get(server) != null
					&& !ABAP_AS_properties_map.get(server).equals(properties)) {
				eventListener.updated(server);
			}
			ABAP_AS_properties_map.put(server, properties);
		}
		LOG.info("SAPJavaSDK :: ServerDataProvider changePropertiesForABAP_AS() :: Change "
				+ server + " properties completed");
	}
}
