import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.adventnet.afp.log.LogException;
import com.adventnet.afp.log.LoggerProperties;
import com.adventnet.snmp.beans.SnmpTarget;
import com.adventnet.utils.LogManager;

public class APSnmpTest {
	SnmpTarget target = new SnmpTarget();
	Logger logger = Logger.getLogger("APSnmpTest");
	Properties prop = new Properties();
	String snmpGetTableByMib = null;
	public APSnmpTest() {

		try {
			File dir = new File("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileHandler handler = new FileHandler("logs/ApSnmpTest_info.txt", 1024 * 1024, 1, true);
			logger.addHandler(handler);
			logger.setLevel(Level.ALL);
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
			logger.setUseParentHandlers(false); // disable write to stdout
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			prop.load(new FileInputStream("oids.properties"));
		} catch (FileNotFoundException e) {
			logger.info("oids.properties file not found.");
			logger.info(e.toString());
		} catch (IOException e) {
			logger.info("can not read oids.properties file.");
			logger.info(e.toString());
		}

		String snmpDebugMode = prop.getProperty("snmp.debug.mode");
		if (snmpDebugMode != null && snmpDebugMode.equals("true")) {
			logger.info("snmp.debug.mode is true.");
			LoggerProperties loggerProp = new LoggerProperties("SNMP", "SNMP");
			loggerProp.setClassName("com.adventnet.utils.SnmpLoggerImpl");
			loggerProp.setLogLevel(LogManager.CRITICAL);
			loggerProp.addCustomProperty("LOGTYPE", "DEBUG");
			try {
				target.addLogClient(loggerProp);
			} catch (LogException e) {
				e.printStackTrace();
			}
		}
	}
	
	public ArrayList getByColumn(String oid) {
		ArrayList values = new ArrayList();
		target.setObjectID(oid);
		String thisOid = "";
		String thisValue = "";
		while (true) {
			try {
				thisValue = target.snmpGetNext();
				thisOid = target.getObjectID();
			} catch (Exception e) {
				logger.info("Can not get next value of oid : " + oid);
				e.printStackTrace();
				break;
			}

			if (!thisOid.startsWith(oid)) {
				break;
			}
			values.add(thisValue);
		}

		return values;
	}
	


	public static void main(String[] args) {
		ArrayList currentApNames  = new ArrayList();
		Properties previousApNames = new Properties();
		try {
			previousApNames.load(new FileInputStream("apnames.properties"));
		} catch (Exception e) {
		}
		
		APSnmpTest client = new APSnmpTest();
		String host = "192.168.0.19";
		String community = "public";
		String oid = ".1.3.6.1.4.1.9.9.513.1.1.1.1.5";
		if (args.length > 1) {
			host = args[0];
			community = args[1];
			try {
				oid =  args[2];
			} catch (Exception e) {
			}
		}

		client.target.setTargetHost(host);
		client.target.setTargetPort(161);
		client.target.setCommunity(community);
		client.target.setSnmpVersion(1);
		client.logger.info("------------ host: "+host+" ----------");
		String snmpTimeout = null;
		try {
			snmpTimeout = client.prop.getProperty("snmp.target.set.timeout");
		} catch (Exception e) {
		}
		int snmpTimeoutValue = snmpTimeout != null ? Integer.parseInt(snmpTimeout) : 5;
		client.target.setTimeout(snmpTimeoutValue);
		
		String retries = null;
		try {
			retries = client.prop.getProperty("snmp.target.set.retries");
		} catch (Exception e) {
		}
		int retriesValue = retries != null ? Integer.parseInt(retries) : 0;
		if (retriesValue > 0) {client.target.setRetries(retriesValue);}
		
		
		currentApNames = client.getByColumn(oid);
		
		for(int i=0;i<currentApNames.size();i++){
			String name = (String) currentApNames.get(i);
			if(!previousApNames.containsKey(name)) {
        		previousApNames.put(name, "1");  // "1" means this app was online
        		//todo: more actions for new found ap
			}else{
				previousApNames.setProperty(name, "1"); // "1" means this app was online
			}
		}

		System.out.println("Data:");
		Enumeration<?> pnames = previousApNames.propertyNames();
		while (pnames.hasMoreElements()) {  
            String name = (String) pnames.nextElement();
            if(!currentApNames.contains(name)) {
            		previousApNames.setProperty(name, "0"); // "0" means this app was NOT online
            }
            System.out.println(name+"\t"+previousApNames.getProperty(name));
        } 
		if(previousApNames.containsValue("0")) {
            System.out.println("Message: Error AP found");
        }else {
            System.out.println("Message: No issue found");
        }
		try {
			previousApNames.store(new FileOutputStream("apnames.properties"), "store all ap names, 1=online, 0=offline");
		} catch (Exception e) {
			client.logger.info("failed to write apnames.properties");
		}
		
		System.exit(0);
	}

}
