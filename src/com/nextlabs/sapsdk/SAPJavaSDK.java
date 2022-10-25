package com.nextlabs.sapsdk;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProvider;

public class SAPJavaSDK implements IServiceProvider {

	private static final Log LOG = LogFactory.getLog(SAPJavaSDK.class);

	@Override
	public void init() throws Exception {

		LOG.info("SAPJavaSDK init() started.");
		Properties prop = PropertyLoader
				.loadProperties("/jservice/config/SAPJavaSDKService.properties");
		
		SAPJCoServerRegistration.setupDataProviders(prop);
		SAPJCoServerRegistration.setupServer(prop);
	}
	
	public static void main(String[] args) {
		SAPJavaSDK sapsdk = new SAPJavaSDK();
		try {
			sapsdk.init();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

}
