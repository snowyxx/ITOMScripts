import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.adventnet.afp.log.LogException;
import com.adventnet.afp.log.LoggerProperties;
import com.adventnet.snmp.beans.SnmpTarget;
import com.adventnet.snmp.mibs.MibException;
import com.adventnet.snmp.mibs.MibNode;
import com.adventnet.snmp.mibs.MibOperations;
import com.adventnet.snmp.snmp2.SnmpOID;
import com.adventnet.snmp.snmp2.SnmpVar;
import com.adventnet.utils.LogManager;

public class SNMPAttributeMonitor {
	SnmpTarget target = new SnmpTarget();
	Logger logger = Logger.getLogger("SNMPAttributeMonitor");
	Properties prop = new Properties();
	String snmpGetTableByMib = null;

	public SNMPAttributeMonitor() {

		try {
			File dir = new File("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileHandler handler = new FileHandler("logs/SNMPAttributeMonitor_info.txt", 1024 * 1024, 1, true);
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

//		logger.info("snmp.debug.mode is true.");
//		LoggerProperties loggerProp = new LoggerProperties("SNMP", "SNMP");
//		loggerProp.setClassName("com.adventnet.utils.SnmpLoggerImpl");
//		loggerProp.setLogLevel(LogManager.CRITICAL);
//		loggerProp.addCustomProperty("LOGTYPE", "DEBUG");
//		try {
//			target.addLogClient(loggerProp);
//		} catch (LogException e) {
//			e.printStackTrace();
//		
//		}
	}

	
	public static void main(String[] args) {
		SNMPAttributeMonitor client = new SNMPAttributeMonitor();
		String host = "192.168.0.19";
		String community = "public";
		String oid = "";
		String name = "";
		String pattern = "";
		int ver = 0;
		String verStr = "v1";
		if (args.length > 4) {
			host = args[0];
			community = args[1];
			verStr = args[2];
			ver = ("v2c".equals(verStr.trim()))?1:0;
			name = args[3];
			oid = args[4];
			pattern= args[5];
			
		}else{
			System.out.println("Usage: java SNMPAttributeMonitor <host name or ip> <community> <v1 or v2c> <monitor name> <oid> <regular expression>\n"
					+ " - regular expression: You MUST group your value to be got named as \"val\" . e.g. (?<val>\\d+)\\w");
			System.exit(0);
		}
		
		client.target.setTargetHost(host);
		client.target.setTargetPort(161);
		client.target.setCommunity(community);
		client.target.setSnmpVersion(ver);
		client.logger.info("------------ host: "+host+ " community: "+community+" version: "+ver+ "monitor name: "+name+" oid: "+ oid+" regex: "+pattern+" ----------");

		int snmpTimeoutValue = 15;
		client.target.setTimeout(snmpTimeoutValue);
		int retriesValue = 0;
		if (retriesValue > 0) {client.target.setRetries(retriesValue);}
		SnmpOID snmpoid = new SnmpOID(oid);
		client.target.setSnmpOID(snmpoid);
		String result = client.target.snmpGet();
		if ((result != null)&&(!"".equals(pattern))){
			Pattern p = Pattern.compile(pattern);
			try {
				Matcher m=p.matcher(result);
				if (m.find() && m.group("val")!=null){
					System.out.println("Message:Success\nData:\n"+name+"\t"+m.group("val"));
				}else{
					System.out.println("Message:SNMP response does not match regex, or did not caputre val group. Result is:"+result);
				}
			} catch (Exception e) {
				System.out.println("Message:"+e.getMessage());
			}
		}else{
			System.out.println("Message:Can not get data for oid:"+oid);
		}
		System.exit(0);
	}
}
