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

import com.adventnet.afp.log.LogException;
import com.adventnet.afp.log.LoggerProperties;
import com.adventnet.snmp.beans.SnmpTarget;
import com.adventnet.snmp.mibs.MibException;
import com.adventnet.snmp.mibs.MibNode;
import com.adventnet.snmp.mibs.MibOperations;
import com.adventnet.snmp.snmp2.SnmpOID;
import com.adventnet.snmp.snmp2.SnmpVar;
import com.adventnet.utils.LogManager;

public class WLSnmpTest {
	SnmpTarget target = new SnmpTarget();
	Logger logger = Logger.getLogger("WLSnmpTest");
	Properties prop = new Properties();
	String snmpGetTableByMib = null;

	public WLSnmpTest() {

		try {
			File dir = new File("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileHandler handler = new FileHandler("logs/WLSnmpTest_info.txt", 1024 * 1024, 1, true);
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
		try {
			snmpGetTableByMib = prop.getProperty("snmpGetTableByMib");
		} catch (Exception e) {
		}
		snmpGetTableByMib = snmpGetTableByMib == null ? "true" : snmpGetTableByMib;
		logger.info("table data fethch by mib or not: " + snmpGetTableByMib);
	}

	

	private void printTable(ArrayList result, String name) {
		int count = 0;
		if ("WappAp".equals(name)) {
			System.out.println("<--table " + name + " starts-->\ncLApName#cLLwappUpTime#cLApPrimaryControllerAddress#cLApPowerStatus#cLApPortNumber#cLApActiveClientCount");
			
		} else {
			System.out.println("<--table " + name + " starts-->\nID#Name#Value");
		}

		for (int i = 0; i < result.size(); i++) {
			StringBuffer sb = new StringBuffer();
			ArrayList row = (ArrayList) result.get(i);
			System.out.println(row);
			try {
				if("WappAp".equals(name)) {
					String cLApName = (4 < row.size()&&!"".equals(row.get(4)))? (String) row.get(4) : "-";
					String cLLwappUpTime = (6 < row.size()&&!"".equals(row.get(6))) ? (String) row.get(6) : "-";
					String cLApPrimaryControllerAddress = (10 < row.size()&&!"".equals(row.get(10))) ? (String) row.get(10) : "-";
					String cLApPowerStatus = (19 < row.size()&&!"".equals(row.get(19))) ? (String) row.get(19) : "-";
					String cLApPortNumber = (38 < row.size()&&!"".equals(row.get(38))) ? (String) row.get(38) : "-";
					String cLApActiveClientCount = (71 < row.size()&&!"".equals(row.get(71))) ? (String) row.get(71) : "-";
			
					sb.append(cLApName).append("#").append(cLLwappUpTime).append("#").append(cLApPrimaryControllerAddress)
							.append("#").append(cLApPowerStatus).append("#").append(cLApPortNumber).append(cLApActiveClientCount);
				} else {

					// TO IMPROVE: print first 3 columns of a table by default....
					// it is not a good idea. Should make it flexible using a configuration file,
					// e.g. oids.properties or another json/xml file
					String id = (0 < row.size()) ? (String) row.get(0) : "-";
					String cname = (1 < row.size()) ? (String) row.get(1) : "-";
					String value = (2 < row.size()) ? (String) row.get(2) : "-";
					sb.append(id).append("#").append(cname).append("#").append(value);
				}
				if (sb.length() > 0) {
					System.out.println(sb.toString());
				}
			} catch (Exception e) {
				logger.info("Error when try to print a line of data");
				e.printStackTrace();
				logger.info(e.toString());
			}

		}
		
		System.out.println("<--table " + name + " ends-->");
		

	}

	private void processCiscoWLData() {

		HashMap<String, String> tables = new HashMap<String, String>();
//		tables.put("WappAp", ".1.3.6.1.4.1.9.9.513.1.1.1");
		tables.put("TMP", ".1.3.6.1.4.1.2.3.51.3.1.1.2");
		HashMap<String, String> attributes = new HashMap<String, String>();
//		attributes.put("Power Status", ".1.3.6.1.4.1.2.3.51.3.5.1.1.0");

		for (Entry<String, String> entry : tables.entrySet()) {
			String name = entry.getKey();
			String oid = entry.getValue();
			logger.info("going to snmpget table: " + name + " : " + oid);
			ArrayList result = null;
			if (snmpGetTableByMib.equals("true")) {
				result = getTableRowsByMib(oid);
			} else {
				result = getTableRows(oid);
			}
			printTable(result, name);
		}

		for (Entry<String, String> entry : attributes.entrySet()) {
			String name = entry.getKey();
			String oid = entry.getValue();
			logger.info("going to snmpget attribute: " + name + " : " + oid);
			getAndPrintAttribute(name, oid);
		}

	}

	private ArrayList getTableRows(String oid) {
		target.setObjectID(oid);
		String thisColumnIndex = null;
		String thisOid = "";
		String thisValue = "";
		int rowIndex = 0;
		ArrayList result = new ArrayList();
		String maxRowsNumber = prop.getProperty("maxRowsNumber");
		int maxNumber = maxRowsNumber != null ? Integer.parseInt(maxRowsNumber) : 100;
		while (true) {
			try {
				thisValue = target.snmpGetNext();
				thisOid = target.getObjectID();
			} catch (Exception e) {
				logger.info("Can not get next value of oid : " + oid);
				break;
			}

			if (!thisOid.startsWith(oid)) {
				break;
			}
			String thisColumnOid = thisOid.substring(0, thisOid.lastIndexOf("."));
			String preOid = thisColumnOid.substring(0, thisColumnOid.lastIndexOf("."));
			String columnIndex = thisColumnOid.substring(thisColumnOid.lastIndexOf(".") + 1);

			if (thisColumnIndex == null) {
				thisColumnIndex = columnIndex;
			} else if (!thisColumnIndex.equals(columnIndex)) {
				thisColumnIndex = columnIndex;
				rowIndex = 0;
			} else {
				rowIndex++;
				if (rowIndex == maxNumber - 1) {
					int nextColumnIndexValue = Integer.parseInt(columnIndex) + 1;
					String nextColumnOid = preOid + "." + String.valueOf(nextColumnIndexValue);
					target.setObjectID(nextColumnOid);
				}
			}
			ArrayList thisRow = new ArrayList();
			try {
				thisRow = (ArrayList) result.get(rowIndex);
			} catch (Exception e) {
			}
			thisRow.add(thisValue);
			try {
				result.remove(rowIndex);
			} catch (Exception e) {
			}
			result.add(rowIndex, thisRow);
		}

		return result;
	}

	private void getAndPrintAttribute(String name, String oid) {
		SnmpOID snmpoid = new SnmpOID(oid);
		target.setSnmpOID(snmpoid);
		String result = target.snmpGet();
		result = (result != null) ? result : "NA";
		System.out.println(name + "=" + result);
	}

	private ArrayList getTableRowsByMib(String oid) {
		System.out.println("yanxxxxxx getTableRowsByMib");
		ArrayList result = new ArrayList();
		String oids[] = null;

		MibOperations mibops = target.getMibOperations();
		
		SnmpOID snmptableoid = mibops.getSnmpOID(oid);
		MibNode tablenode = mibops.getMibNode(oid);
		Enumeration mibnames = mibops.getMibModuleNames();
		while(mibnames.hasMoreElements()){
			System.out.println("yanxxxxxx"+ mibnames.nextElement().toString());
		}
		System.out.println("yanxxxxxx oid " + oid);
		System.out.println("yanxxxxxx tablenode.isTable()"+tablenode.isTable());
		if (tablenode != null && tablenode.isTable()) {
			Vector colums = tablenode.getTableItems();
			oids = new String[colums.size()];
			for (int i = 0; i < oids.length; i++) {
				oids[i] = (String) colums.elementAt(i);
			}
			target.setObjectIDList(oids);
			try {
				target.setMaxNumRows(3000);
				String[][] data = target.snmpGetAllList();
				System.out.println("yanxxxxxx snmpGetAllList" + data.toString());
				for (int i = 0; i < data.length; i++) {
					ArrayList<String> row = new ArrayList<String>();
					for (int j = 0; j < data[i].length; j++) {
						row.add(data[i][j]);
					}
					result.add(row);
				}
			} catch (NullPointerException e) {
				logger.info("No response from snmp agent.");
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return result;
	}
	public static void main(String[] args) {
		WLSnmpTest client = new WLSnmpTest();
		String host = "192.168.0.19";
		String community = "public";
		int ver = 0;
		String verStr = "v1";
		if (args.length > 1) {
			host = args[0];
			community = args[1];
			try {
				verStr = args[2];
				ver = ("v2c".equals(verStr.trim()))?1:0;
			} catch (Exception e) {
			}
		}
		
		client.target.setTargetHost(host);
		client.target.setTargetPort(161);
		client.target.setCommunity(community);
		client.target.setSnmpVersion(ver);
		client.logger.info("------------ host: "+host+" ----------");
		try {
			client.target.loadMibs("./CISCO-SMI.my ./CISCO-LWAPP-TC-MIB.my ./CISCO-LWAPP-AP-MIB.my");
		} catch (Exception e) {
			client.logger.info("Can not read mib files. Do add MIB files to home folder!");
		}
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
		client.processCiscoWLData();
		System.exit(0);
	}
}
