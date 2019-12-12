import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.Map.Entry;
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
import com.adventnet.utils.LogManager;

public class APMonitor_AppManager {
	static String VER = "19.112201";
	SnmpTarget target = new SnmpTarget();
	Logger logger = Logger.getLogger("APMonitor_AppManager");
	Properties prop = new Properties();
	String snmpGetTableByMib = null;

	public APMonitor_AppManager() {

		try {
			File dir = new File("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileHandler handler = new FileHandler("logs/APMonitor_AppManager.txt", 1024 * 1024, 1, true);
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

	private void printTable(ArrayList result, String name) {
		if ("hwWlanApTable".equals(name)) {
			System.out.println(
					"<--table hwWlanApTable starts-->\nApMac#ApName#RunState#ApIpAddress#OnlineTime#OnlineUserNum#MemoryUseRate#CpuUseRate");

		} else {
			System.out.println("<--table " + name + " starts-->\nID#Name#Value");
		}

		for (int i = 0; i < result.size(); i++) {
			StringBuffer sb = new StringBuffer();
			ArrayList row = (ArrayList) result.get(i);
			try {
				if ("hwWlanApTable".equals(name)) {
					String ApMac = (0 < row.size() && !"".equals(row.get(0))) ? (String) row.get(0) : "-";
					String ApName = (3 < row.size() && !"".equals(row.get(3))) ? (String) row.get(3) : "-";
//					RunState values: INTEGER  { idle ( 1 ) , autofind ( 2 ) , typeNotMatch ( 3 ) , fault ( 4 ) , config ( 5 ) , configFailed ( 6 ) , download ( 7 ) , normal ( 8 ) , committing ( 9 ) , commitFailed ( 10 ) , standby ( 11 ) , verMismatch ( 12 ) , nameConflicted ( 13 ) , invalid ( 14 ) , countryCodeMismatch ( 15 ) } 
					String RunState = (5 < row.size() && !"".equals(row.get(5))) ? (String) row.get(5) : "-";
					String ApIpAddress = (12 < row.size() && !"".equals(row.get(12))) ? (String) row.get(12) : "-";
					String OnlineTime = (20 < row.size() && !"".equals(row.get(20))) ? (String) row.get(20) : "-";
					String OnlineUserNum = (43 < row.size() && !"".equals(row.get(43))) ? (String) row.get(43) : "-";
					String MemoryUseRate = (39 < row.size() && !"".equals(row.get(39))) ? (String) row.get(39) : "-";
					String CpuUseRate = (40 < row.size() && !"".equals(row.get(40))) ? (String) row.get(40) : "-";

					sb.append(ApMac).append("#").append(ApName).append("#").append(RunState).append("#")
							.append(ApIpAddress).append("#").append(OnlineTime).append("#").append(OnlineUserNum)
							.append("#").append(MemoryUseRate).append("#").append(CpuUseRate);
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

	private void processWLANData(String vendor) {
		HashMap<String, String> cols = new HashMap<String, String>();
		HashMap<String, String> attributes = new HashMap<String, String>();

		if ("cisco".equals(vendor)) {
			cols.put("cLApName", ".1.3.6.1.4.1.9.9.513.1.1.1.1.5");
			cols.put("cLApActiveClientCount", ".1.3.6.1.4.1.9.9.513.1.1.1.1.72");
			cols.put("cLApAssocFailCountByRate", "..1.3.6.1.4.1.9.9.513.1.1.1.1.70");
			cols.put("cLApMemoryAverageUsage", ".1.3.6.1.4.1.9.9.513.1.1.1.1.56");
			cols.put("cLApCpuAverageUsage", ".1.3.6.1.4.1.9.9.513.1.1.1.1.58");
			getAndPrintSelectedCol("cLApTable", cols);
		} else if ("huawei".equalsIgnoreCase(vendor)) {
			cols.put("ApName", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.4");
//	      RunState values: INTEGER  { idle ( 1 ) , autofind ( 2 ) , typeNotMatch ( 3 ) , fault ( 4 ) , config ( 5 ) , configFailed ( 6 ) , download ( 7 ) , normal ( 8 ) , committing ( 9 ) , commitFailed ( 10 ) , standby ( 11 ) , verMismatch ( 12 ) , nameConflicted ( 13 ) , invalid ( 14 ) , countryCodeMismatch ( 15 ) } 
			cols.put("RunState", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.6");
			cols.put("ApIpAddress", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.13");
			cols.put("OnlineTime", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.21");
			cols.put("OnlineUserNum", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.44");
			cols.put("MemoryUseRate", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.40");
			cols.put("CpuUseRate", ".1.3.6.1.4.1.2011.6.139.13.3.3.1.41");
			getAndPrintSelectedCol("hwWlanApTable", cols);
		} else if ("h3c".equalsIgnoreCase(vendor)) {
			cols.put("APIPAddress", ".1.3.6.1.4.1.25506.2.75.2.1.1.1.2");
			cols.put("MacAddress", ".1.3.6.1.4.1.25506.2.75.2.1.1.1.3");
//            "Represents operational status of AP. The following values are supported: join(1) - AP is joining to AC. joinConfirm(2) - AC confirm whether permit AP to join. download(3) - AP is downloading software from AC. config(4) - AP and AC exchange configuration before AP - provide WLAN service. run(5) - The WLAN service is ready. "
			cols.put("APOperationStatus", ".1.3.6.1.4.1.25506.2.75.2.1.1.1.4");
			cols.put("APTemplateNameOfAP", ".1.3.6.1.4.1.25506.2.75.2.1.1.1.5");
			getAndPrintSelectedCol("APObjectStatusTable", cols);
		} else if ("ruijie".equalsIgnoreCase(vendor)) {
			cols.put("ApName", ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.2");
			cols.put("ApIp", ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.33");
			cols.put("StaNum", ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.34"); // "This object represents Sta Num attach to the AP." 
			cols.put("ApState", ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.48");
			//"This object represents the AP's state.1 -- on line;2 -- off line"
			// But it only list online ap(value is 1). tested in ws6008
			cols.put("IfLinkSpeed", ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.24");
			cols.put("ApUptime", ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.47");
			getAndPrintSelectedCol("ruijieApCfgTable", cols);
			attributes.put("AcName", ".1.3.6.1.4.1.4881.1.1.10.2.56.1.1.8.0");
			attributes.put("AcLocation", ".1.3.6.1.4.1.4881.1.1.10.2.56.1.1.9.0");
			attributes.put("AcType", ".1.3.6.1.4.1.4881.1.1.10.2.56.1.1.31.0");
			attributes.put("AcOfflineApNum", ".1.3.6.1.4.1.4881.1.1.10.2.56.1.1.80.0");
			attributes.put("AcClientNum", ".1.3.6.1.4.1.4881.1.1.10.2.56.1.1.15.0");
			for (Entry<String, String> entry : attributes.entrySet()) {
				String name = entry.getKey();
				String oid = entry.getValue();
				logger.info("going to snmpget attribute: " + name + " : " + oid);
				SnmpOID snmpoid = new SnmpOID(oid);
				target.setSnmpOID(snmpoid);
				String result = target.snmpGet();
				result = (result != null) ? result : "NA";
				if("AcType".equals(name)) {
					String typeNames[] = {"UNKOWN", "ws5302", "ws5708" , "m8600ws" , "ws3302" , "m12000ws", "ws5504", "ws6108", "ws6816", "m18000-WS-ED" , "m8600E-WS-ED", "eg2000" , "ws6008", "ws6812", "aw608", "ws6024", "eg350"};
					int result_N = Integer.parseInt(result);
					if(result_N <= typeNames.length ) {
						result = typeNames[result_N];
					}
				}
				System.out.println(name + "=" + result);
			}
		} else if ("test".equalsIgnoreCase(vendor)) {
			cols.put("ifDescr", ".1.3.6.1.2.1.2.2.1.2");
			cols.put("ifOperStatus", ".1.3.6.1.2.1.2.2.1.8");
			cols.put("ifMtu", ".1.3.6.1.2.1.2.2.1.4");
			getAndPrintSelectedCol("ifTable", cols);
		}else if ("HillstoneIpsec".equalsIgnoreCase(vendor)) {
			cols.put("TunnelID", ".1.3.6.1.4.1.28557.2.1.1.1.1.3");
			cols.put("TunnelName", ".1.3.6.1.4.1.28557.2.1.1.1.1.2");
			cols.put("TunnelType", ".1.3.6.1.4.1.28557.2.1.1.1.1.4");
			cols.put("TunnelPeerIp", ".1.3.6.1.4.1.28557.2.1.1.1.1.5");
			cols.put("TunnelExIfIndex", ".1.3.6.1.4.1.28557.2.1.1.1.1.6");
			cols.put("TunnelLocalID", ".1.3.6.1.4.1.28557.2.1.1.1.1.7");
			cols.put("TunnelRemoteID", ".1.3.6.1.4.1.28557.2.1.1.1.1.8");
			cols.put("TunnelCryptAlgorithms", ".1.3.6.1.4.1.28557.2.1.1.1.1.9");
			cols.put("TunnelAuthAlgorithms", ".1.3.6.1.4.1.28557.2.1.1.1.1.10");
			cols.put("TunnelLifeTime", ".1.3.6.1.4.1.28557.2.1.1.1.1.11");
			cols.put("TunnelStatus", ".1.3.6.1.4.1.28557.2.1.1.1.1.12");
			getAndPrintSelectedCol("HillstoneIpsec", cols);
		}

//		HashMap<String, String> tables = new HashMap<String, String>();
//		if ("huawei".equals(vendor)) {
//			tables.put("hwWlanApTable", ".1.3.6.1.4.1.2011.6.139.13.3.3");
//			
//		}
//
//		for (Entry<String, String> entry : tables.entrySet()) {
//			String name = entry.getKey();
//			String oid = entry.getValue();
//			logger.info("going to snmpget table: " + name + " : " + oid);
//			ArrayList result = null;
//			result = getTableRowsByMib(oid);
//			printTable(result, name);
//		}

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
			logger.info("target timeout is :" + target.getTimeout());
			logger.info("target.getMaxNumRows() :" + target.getMaxNumRows());
			logger.info("target.getMaxRepetitions() :" + target.getMaxRepetitions());
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
				logger.info("No response from snmp agent." + oids);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return result;
	}

	public void getAndPrintSelectedCol(String tableName, HashMap<String, String> cols) {
		String startsLine = "<--table " + tableName + " starts-->\n";
		ArrayList<ArrayList> colList = new ArrayList<ArrayList>();
		for (Entry<String, String> entry : cols.entrySet()) {
			String name = entry.getKey();
			String oid = entry.getValue();
			startsLine += name + "#";
			logger.info("going to snmpget column: " + name + " : " + oid);
			ArrayList result = null;
			result = getByColumn(oid);
			if("HillstoneIpsec".equals(tableName) && "TunnelStatus".equalsIgnoreCase(name)) {
				for (int i=0; i< result.size(); i++) {
					String typeNames[] = {"UNKOWN", "active", "inactive"};
					int result_N = Integer.parseInt((String)result.get(i));
					if(result_N <= typeNames.length ) {
						result.set(i, typeNames[result_N]);
					}
				}
			}
			colList.add(result);
			if (result.size() == 0) {
				System.out.println("!!!! Can not got AP table oid response");
				return;
			}
		}
		System.out.println(startsLine.substring(0, startsLine.length() - 1));
		int colcount = colList.size();
		if (colcount == 0)
			return;
		int rowCount = colList.get(0).size();
		for (int i = 0; i < rowCount; i++) {
			StringBuffer sb = new StringBuffer();
			for (int j = 0; j < colcount; j++) {
				sb.append(colList.get(j).get(i)).append("#");
			}
			System.out.println(sb.toString().substring(0, sb.toString().length() - 1));
		}

		System.out.println("<--table " + tableName + " ends-->");
		System.out.println("APCount=" + rowCount);

	}

	public ArrayList getByColumn(String oid) {
		ArrayList values = new ArrayList();
		target.setObjectID(oid);
		String thisOid = ".9.9.9";
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

			if (!thisOid.startsWith(oid) || thisOid.equals(oid)) {
				break;
			}
			values.add(thisValue);
		}

		return values;
	}

	public static void main(String[] args) {
		int ver = 0;
		String verStr = "v1";
		String vendor = "huawei";
		APMonitor_AppManager client = new APMonitor_AppManager();
		client.logger.info("\n------------ Version: " + VER + " ----------");
		String host = "192.168.0.19";
		String community = "yantest";
		if (args.length > 1) {
			host = args[0];
			community = args[1];
			try {
				verStr = args[2];
				ver = ("v2c".equals(verStr.trim())) ? 1 : 0; // 0 = v1 1=v2c
			} catch (Exception e) {
			}
			try {
				vendor = args[3];
			} catch (Exception e) {
			}
		}

		client.target.setTargetHost(host);
		client.target.setTargetPort(161);
		client.target.setCommunity(community);
		client.target.setSnmpVersion(ver);
		client.logger.info("------------ host: " + host + " ----------");
//		String mibs = client.prop.getProperty("mibs");
//		if (mibs != null) {
//			client.logger.info("mib files to load: " + mibs);
//		}
//		try {
//			client.target.loadMibs(mibs);
//		} catch (MibException | IOException e) {
//			client.logger.info("Can not read mib file.");
//			client.logger.info(e.toString());
//		}
		String snmpTimeout = null;
		try {
			snmpTimeout = client.prop.getProperty("snmp.target.set.timeout");
		} catch (Exception e) {
		}
		int snmpTimeoutValue = snmpTimeout != null ? Integer.parseInt(snmpTimeout) : 5;
		client.logger.info("snmp timeout is:" + snmpTimeoutValue);
		client.target.setTimeout(snmpTimeoutValue);

		String retries = null;
		try {
			retries = client.prop.getProperty("snmp.target.set.retries");
		} catch (Exception e) {
		}
		int retriesValue = retries != null ? Integer.parseInt(retries) : 0;
		if (retriesValue > 0) {
			client.logger.info("snmp retry is:" + retriesValue);
			client.target.setRetries(retriesValue);
		}
		SnmpOID sysoid = new SnmpOID(".1.3.6.1.2.1.1.2.0");
		client.target.setSnmpOID(sysoid);
		long t1 = System.currentTimeMillis();
		String result = client.target.snmpGet();
		int t = (int) (System.currentTimeMillis() - t1);
		if (result != null) {
			System.out.println("script_availability=0");
			System.out.println("script_responsetime=" + t);
			client.logger.info("vendor is:" + vendor);
			client.processWLANData(vendor);
		} else {
			System.out.println("script_availability=1");
			System.out.println("script_message=Can not connect to snmp agent");
		}

		System.exit(0);
	}
}
