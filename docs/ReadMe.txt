##########################################################################
############   SAPJCo-EntitlementManager   ###############################
##########################################################################

SAP JCo Plugin for CE-PC or JAVA PC

1.	Objective
-----------------------------------------------------------------------------
The SAP JCo Plugin is a new product component which will enable communication between 
the SAP EM and Java Policy controller/CE-PC. This replaces the slower Web Service 
(GSOAP) calls previously made between SAP EM and PC.

2.	Scope & Design
-----------------------------------------------------------------------------
SCOPE:
This plugin is developed based on service-plugin framework of Policy Controller 
platform and uses SAP’s JCo library. For further usage and information about the SAP 
JCo plugin please refer to the section 5 of Design Specification for Java Connector 
(SAP EM) document.

ASSUMPTIONS:
NA

3.	Build From Source (only for developers to modify/enhance source code)
-----------------------------------------------------------------------------
•	Get the latest source from Perforce //depot/plugins/SAP_JCo/
•	Install Java version 1.5/above and ANT on your PC. Configure below environment variables;
	o	JAVA_HOME= {java jdk installation root folder}
	o	ANT_HOME= {ant installation root folder}
•	Append or add to PATH variable:  %JAVA_HOME% \bin; %ANT_HOME%\bin;
•	If any changes are made to the checked-out source code, build the SAP JCo binaries as below:
•	Open CMD, go to the SAP_JCo folder and run ant -f build.xml

4.	Distribution & Deployment          
-----------------------------------------------------------------------------
The SAP JCo 3.0 requires a Java Runtime Environment (JRE) version 1.5 or above. 
Policy Controller is already bundled with a JRE and hence no need to separately 
install JRE.

All binaries related to this plugin are distributed as .zip file. Extract the 
contents of this .zip file to any temporary folder and to appropriate installation steps.

Pre-requisites
•	Install VC++ 2005 SP1 distribution and patch for SP1
	o	http://www.microsoft.com/downloads/details.aspx?familyid=EB4EBE2D-33C0-4A47-9DD4-B9A6D7BD44DA&displaylang=en (VC++ 2005 SP1)
	o	http://www.microsoft.com/downloads/details.aspx?displaylang=en&FamilyID=766a6af7-ec73-40ff-b072-9112bab119c2 (Patch for SP1)
•	Add SAP Gateway Host and port to 
	o	<Windows Home>\System32\drivers\etc\services file in Windows
	o	/etc/services file in Linux/Solaris
	o	Samples
			sapgw00	3300/tcp
			sapgw01	3301/tcp
	o	sapgw00 or sapgw01 etc. should match with the gateway service names that are configured 
		in the SAPJavaSDKService.properties

Installation (Manual steps for Windows CE-Policy Controller)
•	Stop Policy Controller, if it is already running. 
•	Create below folders, if these do not exist.
	o	[NextLabs install home]/Policy Controller/jservice/config
	o	[NextLabs install home]/Policy Controller/jservice/jar/sap
	o	[NextLabs install home]/Policy Controller/jre/lib/ext
•	From the extracted contents of the .zip file, copy below files;
	o	config/SAPJavaSDKService.propertie to /Policy Controller/jservice/config
	o	jars/SAPJCo-EntitlementManager.jar to Policy Controller/jservice/jar/ sap
	o	xlib/< SAP JCo Lib Folder>/sapjco3.jar to /Policy Controller/jre/lib/ext
			Refer OS/JRE platform vs. JCo Lib Folder section below to choose the correct folder. 
	o	xlib/< SAP JCo Lib Folder>/sapjco3.dll to /Policy Controller/jre/lib/ext  

Installation (Manual steps for Windows/Linux/Solaris Java PC)
Note: Refer Java Policy Controller related documentation to set up the Java PC. It is usually deployed 
in a Tomcat server environment. [tomcat-home] refers to the Tomcat root folder under which JPC is deployed.
•	Stop Policy Controller, if it is already running. 
•	Create below folders, if these do not exist.
	o	[tomcat-home]/nextlabs/dpc/jservice/config
	o	[tomcat-home]/nextlabs/dpc/jservice/jar/sap
	o	[tomcat-home]/nextlabs/shared_lib/
•	From the extracted contents of the .zip file, copy below files;
	o	config/SAPJavaSDKService.propertie to [tomcat-home]/nextlabs/dpc/jservice/config
	o	jars/SAPJCo-EntitlementManager.jar to [tomcat-home]/nextlabs/dpc/jservice/jar/ sap
	o	xlib/<SAP JCo Lib Folder>/sapjco3.jar to [tomcat-home]/nextlabs/shared_lib/
			Refer OS/JRE platform vs. JCo Lib Folder section below to choose the correct folder.
	o	xlib/< SAP JCo Lib Folder>/sapjco3.dll or sapjco3.so to [tomcat-home]/nextlabs/shared_lib/

Installation (.bat script for Windows CE-Policy Controller)
•	Stop Policy Controller, if it is already running. 
•	Open CMD as Administrator and go to the folder where SAP JCo zip file is extracted.
•	Run deployManager.bat and follow the on-screen instructions.
•	Few points to note during the installation are;
	o	Specify the Policy Controller root folder in the first screen, before choosing install or 
		uninstall option.
	o	In ‘install’ options, specify the password in plain text form. Password encryption (refer 
		‘Password Encryption’ section below) is required only during manual installation process.
	o	‘Uninstall’ option deletes all the jars, dlls, properties etc. Remember to take back up of 
		the SAPJavaSDKService.properties, if many destinations are configured in it.

Un-Installation (Manual steps for CE or Java PC on Win/Linux/Solaris)
•	Stop Policy Controller, if it is already running. 
•	Delete /jservice/config/SAPJavaSDKService.properties (take a backup if needed)
•	Delete /service/jar/sap/ SAPJCo-EntitlementManager.jar
•	Delete sapjco.jar and sapjco3.dll/so files from jre/lib/ext/ or nextlabs/shared_lib/
•	Start Policy Controller.

Password Encryption
Password encryption tool is distributed with Policy Server. To encrypt a plain text password,
•	Open CMD and go to <Policy Server Home>/tools/crypt folder.
•	Run mkpasswd.bat  -password <plain text password>
•	Encrypted password will be displayed in the same console.
•	Use this encrypted password in SAPJavaSDKService.properties, while doing manual installation.

OS/JRE platform vs. JCo Lib Folder
Choose appropriate JCo Lib folder based on OS/JRE platform;
•	Windows 32-bit – NTia32 folder
•	Windows 64-bit Intel-Arch  – NTia64 folder
•	Windows 64-bit AMD – NTamd64 folder
•	Solaris on Intel 32/64-bit – sunx86_64 folder
•	Linux on 32/64-bit – linuxx86_64 folder

SAP Destination settings in SAPJavaSDKService.properties file
To setup the connection between a SAP system and the plugin, certain destination parameters have 
to be configured for each server instance.
•	.bat script installation will automatically add one destination configuration block using ‘SERV1_’ 
	prefix. Manually, many blocks can be added by duplicating the existing SERV1 settings and change appropriate settings.
•	To add a new set of destination parameters, append prefix values to the ‘server_name’ property in the 
	following format: “SERV#_;”.  For example, if there are already 2 servers configured, to add a  third server, 
	you may  append or add  DEST3_;  and SERV3_; respectively.
•	Enter all the configuration parameters prefixed with the same prefix appended to the server_name property for 
	the new server instance.
•	Client password property must be encrypted. 
•	Save the changes made.
•	Start Policy Controller
•	To verify whether this service is registered correctly, look for “SAPJavaSDK init() started“ lines in the agent logs. 
•	May need to add com.nextlabs.level=FINEST in the logging configuration before starting Policy Controller. FINEST 
	should be set only for debugging purposes. In Production environment, always revert to WARN after any debugging activities.
GRC Integration
In order for the GRC User Attribute Plugin to work, two properties needs to be updated in SAPJavaSDKService.properties
•	grc_enabled -  By default this is set to “false”. Set to “true” to enable the GRC plugin
•	grc_obligation_name – Specify the name of the GRC obligation

5.	Contact
-----------------------------------------------------------------------------
For any further changes/enhancements or any queries, contact Srikanth.Karanam@nextlabs.com
