package com.nextlabs.sapsdk;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertyLoader {

	private static final Log LOG = LogFactory.getLog(PropertyLoader.class);

	public static Properties loadProperties(String name) {
	
		// In DPC environment, this SDK plugin need to resolve paths based on DPC Install Home directory.
		String dpcInstallHome = System.getProperty("dpc.install.home");
		if(dpcInstallHome == null || dpcInstallHome.trim().length() < 1){
			dpcInstallHome = ".";
		}
		LOG.info("DPC Install Home :" + dpcInstallHome);	
	
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("Invalid file name");

		name = dpcInstallHome + name;
		Properties result = null;

		try {
			File file = new File(name);
			LOG.info("Properties File Path:: " + file.getAbsolutePath());
			System.out.println("Properties File Path:: "
					+ file.getAbsolutePath());
			if (file != null) {
				FileInputStream fis = new FileInputStream(file);
				result = new Properties();
				result.load(fis); // Can throw IOException
			}
		} catch (Exception e) {
			LOG.error("Error parsing properties file ", e);
			result = null;
		}
		return result;
	}

}
