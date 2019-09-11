import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

public class IpmiSnmpTest {
	SnmpTarget target = new SnmpTarget();
	Logger logger = Logger.getLogger("IpmiSnmpTest");
	Properties prop = new Properties();
	String snmpGetTableByMib = null;

	public IpmiSnmpTest() {

		try {
			File dir = new File("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileHandler handler = new FileHandler("logs/IpmiSnmpTest_info.txt", 1024 * 1024, 1, true);
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

	public static void main(String[] args) {
		IpmiSnmpTest client = new IpmiSnmpTest();
		String host = "192.168.0.19";
		String community = "yantest";
		if (args.length > 1) {
			host = args[0];
			community = args[1];
		}

		client.target.setTargetHost(host);
		client.target.setTargetPort(161);
		client.target.setCommunity(community);
		client.target.setSnmpVersion(0);
		client.logger.info("------------ host: "+host+" ----------");
		try {
			client.target.loadMibs("./RFC1213-MIB ./imm.mib");
		} catch (MibException | IOException e) {
			client.logger.info("Can not read mib file. Do add imm.mib and RFC1213.MIB file to this folder!");
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
		client.processIBMHWData();
		System.exit(0);
	}

	private void printTable(ArrayList result, String name) {
		int count = 0;
		if ("Error".equals(name)) {
			Collections.reverse(result);
			System.out.println("<--table Last 5 Errors starts-->\nID#Time#Message");
			
		} else if ("Disk".equals(name)) {
			System.out.println("<--table " + name + " starts-->\nindex#diskFruName#diskHealthStatus");
		} else if ("Power".equals(name)) {
			System.out.println("<--table " + name + " starts-->\nindex#powerFruName#powerHealthStatus");
		} else if ("CPU".equals(name)) {
			System.out.println("<--table " + name
					+ " starts-->\nindex#cpuVpdDescription#cpuVpdSpeed#cpuVpdidentifier#cpuVpdType#cpuVpdFamily#cpuVpdCores#cpuVpdThreads#cpuVpdVoltage#cpuVpdDataWidth#cpuVpdHealthStatus");
		} else if ("Memory".equals(name)) {
			System.out.println(
					"<--table " + name + " starts-->\nindex#memoryVpdDescription#memoryVpdType#memoryVpdHealthStatus");
		} else if ("TMP".equals(name)) {
			System.out.println("<--table " + name + " starts-->\nindex#tempDescr#tempReading#tempHealthStatus");
		} else if ("VOLT".equals(name)) {
			System.out.println("<--table " + name + " starts-->\nindex#voltDescr#voltReading#voltHealthStatus");
		} else if ("FAN".equals(name)) {
			System.out.println("<--table " + name + " starts-->\nindex#fanDescr#fanReading#fanHealthStatus");
		} else if ("Network Interface".equals(name)) {
			System.out.println("<--table " + name + " starts-->\nindex#ifDescr#ifSpeed#ifPhysAddress#ifAdminStatus#ifOperStatus");
		}else {
			System.out.println("<--table " + name + " starts-->\nID#Name#Value");
		}

		for (int i = 0; i < result.size(); i++) {
			if (count == 5) {
				break;
			}
			StringBuffer sb = new StringBuffer();
			ArrayList row = (ArrayList) result.get(i);
			try {
				if ("Error".equals(name)) {
					String id = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String severity = (2 < row.size()&&!"".equals(row.get(2))) ? (String) row.get(2) : "-";
					String message = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String date = (3 < row.size()&&!"".equals(row.get(3))) ? (String) row.get(3) : "-";
					String time = (4 < row.size()&&!"".equals(row.get(4))) ? (String) row.get(4) : "-";
					if (severity.indexOf("error(0)") > -1) {
						sb.append(id).append("#").append(date).append(" ").append(time).append("#").append(message);
						count += 1;
					} else {
						// maybe do some other thing later...
						continue;
					}
				} else if ("Power".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String powerFruName = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String powerHealthStatus = (5 < row.size()&&!"".equals(row.get(5))) ? (String) row.get(5) : "-";
					
					sb.append(index).append("#").append(powerFruName).append("#").append(powerHealthStatus);
				} else if ("Disk".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String diskFruName = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String diskHealthStatus = (2 < row.size()&&!"".equals(row.get(2))) ? (String) row.get(2) : "-";

					sb.append(index).append("#").append(diskFruName).append("#").append(diskHealthStatus);
				} else if ("CPU".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String cpuVpdDescription = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String cpuVpdSpeed = (2 < row.size()&&!"".equals(row.get(2))) ? (String) row.get(2) : "-";
					String cpuVpdidentifier = (3 < row.size()&&!"".equals(row.get(3))) ? (String) row.get(3) : "-";
					String cpuVpdType = (4 < row.size()&&!"".equals(row.get(4))) ? (String) row.get(4) : "-";
					String cpuVpdFamily = (5 < row.size()&&!"".equals(row.get(5))) ? (String) row.get(5) : "-";
					String cpuVpdCores = (6 < row.size()&&!"".equals(row.get(6))) ? (String) row.get(6) : "-";
					String cpuVpdThreads = (7 < row.size()&&!"".equals(row.get(7))) ? (String) row.get(7) : "-";
					String cpuVpdVoltage = (8 < row.size()&&!"".equals(row.get(8))) ? (String) row.get(8) : "-";
					String cpuVpdDataWidth = (9 < row.size()&&!"".equals(row.get(9))) ? (String) row.get(9) : "-";
					String cpuVpdHealthStatus = (10 < row.size()&&!"".equals(row.get(10))) ? (String) row.get(10) : "-";

					sb.append(index).append("#").append(cpuVpdDescription).append("#").append(cpuVpdSpeed).append("#")
							.append(cpuVpdidentifier).append("#").append(cpuVpdType).append("#").append(cpuVpdFamily)
							.append("#").append(cpuVpdCores).append("#").append(cpuVpdThreads).append("#")
							.append(cpuVpdVoltage).append("#").append(cpuVpdDataWidth).append("#")
							.append(cpuVpdHealthStatus);
				} else if ("Memory".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String memoryVpdDescription = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String memoryVpdType = (5 < row.size()&&!"".equals(row.get(5))) ? (String) row.get(5) : "-";
					String memoryVpdHealthStatus = (7 < row.size()&&!"".equals(row.get(7))) ? (String) row.get(7) : "-";
					
					sb.append(index).append("#").append(memoryVpdDescription).append("#").append(memoryVpdType)
							.append("#").append(memoryVpdHealthStatus);
				} else if ("TMP".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String tempDescr = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String tempReading = (2 < row.size()&&!"".equals(row.get(2))) ? (String) row.get(2) : "-";
					String tempHealthStatus = (10 < row.size()&&!"".equals(row.get(10))) ? (String) row.get(10) : "-";
					
					sb.append(index).append("#").append(tempDescr).append("#").append(tempReading)
							.append("#").append(tempHealthStatus);
				}  else if ("VOLT".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String voltDescr = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String voltReading = (2 < row.size()&&!"".equals(row.get(2))) ? (String) row.get(2) : "-";
					String voltHealthStatus = (10 < row.size()&&!"".equals(row.get(10))) ? (String) row.get(10) : "-";
					
					voltReading = String.valueOf(Integer.valueOf(voltReading) / 1000.0);
					sb.append(index).append("#").append(voltDescr).append("#").append(voltReading)
							.append("#").append(voltHealthStatus);
				} else if ("FAN".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0)))? (String) row.get(0) : "-";
					String fanDescr = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String fanReading = (2 < row.size()&&!"".equals(row.get(2))) ? (String) row.get(2) : "-";
					String fanHealthStatus = (9 < row.size()&&!"".equals(row.get(9))) ? (String) row.get(9) : "-";
			
					fanReading = fanReading.indexOf("%") > 0 ? fanReading.substring(0, fanReading.indexOf("%")).trim() :fanReading; 
					sb.append(index).append("#").append(fanDescr).append("#").append(fanReading)
							.append("#").append(fanHealthStatus);
				} else if("Network Interface".equals(name)) {
					String index = (0 < row.size()&&!"".equals(row.get(0)))? (String) row.get(0) : "-";
					String ifDescr = (1 < row.size()&&!"".equals(row.get(1))) ? (String) row.get(1) : "-";
					String ifSpeed = (4 < row.size()&&!"".equals(row.get(4))) ? (String) row.get(4) : "-";
					String ifPhysAddress = (5 < row.size()&&!"".equals(row.get(5))) ? (String) row.get(5) : "-";
					String ifAdminStatus = (6 < row.size()&&!"".equals(row.get(6))) ? (String) row.get(6) : "-";
					String ifOperStatus = (7 < row.size()&&!"".equals(row.get(7))) ? (String) row.get(7) : "-";
			
					sb.append(index).append("#").append(ifDescr).append("#").append(ifSpeed)
							.append("#").append(ifPhysAddress).append("#").append(ifAdminStatus).append("#").append(ifOperStatus);
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
		if ("Error".equals(name)) {
			System.out.println("<--table Last 5 Errors ends-->");
		} else {
			System.out.println("<--table " + name + " ends-->");
		}

	}

	private void processIBMHWData() {

		HashMap<String, String> tables = new HashMap<String, String>();
		tables.put("TMP", ".1.3.6.1.4.1.2.3.51.3.1.1.2");
		tables.put("VOLT", ".1.3.6.1.4.1.2.3.51.3.1.2.2");
		tables.put("FAN", ".1.3.6.1.4.1.2.3.51.3.1.3.2");
		// tables.put("Error", ".1.3.6.1.4.1.2.3.51.3.2.1.1"); move to configuration
		// file oids.properties

		// IMM2
		tables.put("Disk", ".1.3.6.1.4.1.2.3.51.3.1.12.2");
		tables.put("Power", ".1.3.6.1.4.1.2.3.51.3.1.11.2");
		tables.put("CPU", ".1.3.6.1.4.1.2.3.51.3.1.5.20");
		tables.put("Memory", ".1.3.6.1.4.1.2.3.51.3.1.5.21");

		HashMap<String, String> attributes = new HashMap<String, String>();
		attributes.put("Power Status", ".1.3.6.1.4.1.2.3.51.3.5.1.1.0");
		attributes.put("System Status", ".1.3.6.1.4.1.2.3.51.3.5.1.4.0");

		for (Entry entry : prop.entrySet()) {
			String name = (String) entry.getKey();
			String oid = (String) entry.getValue();
			if (name.startsWith("TABLE_") && !tables.containsKey(name.substring(6, name.length()))) {
				tables.put(name.substring(6, name.length()), oid);
			} else if (name.startsWith("ATTRI_") && !tables.containsKey(name.substring(6, name.length()))) {
				attributes.put(name.substring(6, name.length()), oid);
			} else {
				// logger.info("Your key does not with TABLE_ or ATTRI_, so do not consider it
				// as an oid to get: " + name);
			}
		}

		for (Entry<String, String> entry : tables.entrySet()) {
			String name = entry.getKey();
			String oid = entry.getValue();
			logger.info("going to snmpget table: " + name + " : " + oid);
			ArrayList result = null;
			if (snmpGetTableByMib.equals("true")) {
				result = getTableRowsByMib(oid);
			} else {
				if (".1.3.6.1.4.1.2.3.51.3.2.1.1".equals(oid)) {
					logger.info("!!! if you do not get data by mib, error table fetching will be disabled.");
					continue;
				}
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
		ArrayList result = new ArrayList();
		String oids[] = null;

		MibOperations mibops = target.getMibOperations();
		SnmpOID snmptableoid = mibops.getSnmpOID(oid);
		MibNode tablenode = mibops.getMibNode(oid);
		if (tablenode != null && tablenode.isTable()) {
			Vector colums = tablenode.getTableItems();
			oids = new String[colums.size()];
			for (int i = 0; i < oids.length; i++) {
				oids[i] = (String) colums.elementAt(i);
			}
			target.setObjectIDList(oids);
			try {
				String[][] data = target.snmpGetAllList();
				for (int i = 0; i < data.length; i++) {
					ArrayList<String> row = new ArrayList<String>();
					for (int j = 0; j < data[i].length; j++) {
						row.add(data[i][j]);
					}
					result.add(row);
				}
			} catch (NullPointerException e) {
				logger.info("No response from snmp agent.");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return result;
	}
}
