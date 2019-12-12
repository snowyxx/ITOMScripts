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

public class APMonitor_DelliDRAC {
	static String VER = "19.112201";
	SnmpTarget target = new SnmpTarget();
	Logger logger = Logger.getLogger("APMonitor_DelliDRAC");
	Properties prop = new Properties();
	String snmpGetTableByMib = null;

	public APMonitor_DelliDRAC() {

		try {
			File dir = new File("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileHandler handler = new FileHandler("logs/APMonitor_DelliDRAC.txt", 1024 * 1024, 1, true);
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

	private void processiDRACData(String vendor) {
		// This class only handle dell iDRAC
		if ("dell".equalsIgnoreCase(vendor)) {
			HashMap<String, String> attributes = new HashMap<String, String>();
			attributes.put("racShortName", ".1.3.6.1.4.1.674.10892.5.1.1.2.0");
			attributes.put("racVersion", ".1.3.6.1.4.1.674.10892.5.1.1.5.0");
			attributes.put("racURL", ".1.3.6.1.4.1.674.10892.5.1.1.6.0");
			attributes.put("systemServiceTag", ".1.3.6.1.4.1.674.10892.5.1.3.2.0");
			attributes.put("systemExpressServiceCode", ".1.3.6.1.4.1.674.10892.5.1.3.3.0");
			attributes.put("systemModelName", ".1.3.6.1.4.1.674.10892.5.1.3.12.0");
			attributes.put("globalSystemStatus", ".1.3.6.1.4.1.674.10892.5.2.1.0");
			attributes.put("systemLCDStatus", ".1.3.6.1.4.1.674.10892.5.2.2.0");
			attributes.put("globalStorageStatus", ".1.3.6.1.4.1.674.10892.5.2.3.0");
			attributes.put("systemPowerState", ".1.3.6.1.4.1.674.10892.5.2.4.0");
			attributes.put("systemPowerUpTime", ".1.3.6.1.4.1.674.10892.5.2.5.0");
			attributes.put("PowerSupplyStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.9.1");
			attributes.put("VoltageStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.12.1");
			attributes.put("AmperageStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.15.1");
			attributes.put("CoolingDeviceStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.21.1");
			attributes.put("TemperatureStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.24.1");
			attributes.put("MemoryDeviceStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.27.1");
			attributes.put("ProcessorDeviceStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.50.1");
			attributes.put("BatteryStatusCombined", ".1.3.6.1.4.1.674.10892.5.4.200.10.1.52.1");

			for (Entry<String, String> entry : attributes.entrySet()) {
				String name = entry.getKey();
				String oid = entry.getValue();
				logger.info("going to snmpget attribute: " + name + " : " + oid);
				SnmpOID snmpoid = new SnmpOID(oid);
				target.setSnmpOID(snmpoid);
				String result = target.snmpGet();
				result = (result != null) ? result : "NA";
				ArrayList objectStatusEnum = new ArrayList();
				objectStatusEnum.add("globalSystemStatus");
				objectStatusEnum.add("systemLCDStatus");
				objectStatusEnum.add("globalStorageStatus");
				objectStatusEnum.add("PowerSupplyStatusCombined");
				objectStatusEnum.add("VoltageStatusCombined");
				objectStatusEnum.add("AmperageStatusCombined");
				objectStatusEnum.add("CoolingDeviceStatusCombined");
				objectStatusEnum.add("TemperatureStatusCombined");
				objectStatusEnum.add("MemoryDeviceStatusCombined");
				objectStatusEnum.add("ProcessorDeviceStatusCombined");
				objectStatusEnum.add("BatteryStatusCombined");
				ArrayList powerStateStatusEnum = new ArrayList() {
					{
						add("systemPowerState");
					}
				};
				if(objectStatusEnum.contains(name)) {
					result = number2Value(result, "ObjectStatusEnum");
				}else if (powerStateStatusEnum.contains(name)) {
					result = number2Value(result, "PowerStateStatusEnum");
				}
				System.out.println(name + "=" + result);
			}
			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.powerGroup.voltageProbeTable
			HashMap<String, String> volCols = new HashMap<String, String>();
			volCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.600.20.1.2");
			volCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.600.20.1.5");
			volCols.put("LocationName", ".1.3.6.1.4.1.674.10892.5.4.600.20.1.8");
			getAndPrintSelectedCol("voltageProbeTable", volCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.powerGroup.amperageProbeTable
			HashMap<String, String> ampCols = new HashMap<String, String>();
			ampCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.600.30.1.2");
			ampCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.600.30.1.5");
			ampCols.put("Reading", ".1.3.6.1.4.1.674.10892.5.4.600.30.1.6");
			ampCols.put("LocationName", ".1.3.6.1.4.1.674.10892.5.4.600.30.1.8");
			getAndPrintSelectedCol("amperageProbeTable", ampCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.thermalGroup.coolingDeviceTable
			HashMap<String, String> coolCols = new HashMap<String, String>();
			coolCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.700.12.1.2");
			coolCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.700.12.1.5");
			coolCols.put("Reading", ".1.3.6.1.4.1.674.10892.5.4.700.12.1.6");
			coolCols.put("LocationName", ".1.3.6.1.4.1.674.10892.5.4.700.12.1.8");
			getAndPrintSelectedCol("coolingDeviceTable", coolCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.thermalGroup.temperatureProbeTable
			HashMap<String, String> tempCols = new HashMap<String, String>();
			tempCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.700.20.1.2");
			tempCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.700.20.1.5");
			tempCols.put("Reading", ".1.3.6.1.4.1.674.10892.5.4.700.20.1.6");
			tempCols.put("LocationName", ".1.3.6.1.4.1.674.10892.5.4.700.20.1.8");
			getAndPrintSelectedCol("temperatureProbeTable", tempCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.deviceGroup.processorDeviceTable
			HashMap<String, String> processorCols = new HashMap<String, String>();
			processorCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.2");
			processorCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.5");
			processorCols.put("MaximumSpeed", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.11");
			processorCols.put("CurrentSpeed", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.12");
			processorCols.put("Voltage", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.14");
			processorCols.put("CoreCount", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.17");
			processorCols.put("BrandName", ".1.3.6.1.4.1.674.10892.5.4.1100.30.1.23");
			getAndPrintSelectedCol("processorDeviceTable", processorCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.deviceGroup.memoryDeviceTable
			HashMap<String, String> memCols = new HashMap<String, String>();
			memCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.1100.50.1.2");
			memCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.1100.50.1.5");
			memCols.put("Size", ".1.3.6.1.4.1.674.10892.5.4.1100.50.1.14");
			memCols.put("Speed", ".1.3.6.1.4.1.674.10892.5.4.1100.50.1.15");
			memCols.put("CurrentOperatingSpeed", ".1.3.6.1.4.1.674.10892.5.4.1100.50.1.27");
			memCols.put("ManufacturerName", ".1.3.6.1.4.1.674.10892.5.4.1100.50.1.21");
			getAndPrintSelectedCol("memoryDeviceTable", memCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.deviceGroup.pCIDeviceTable
			HashMap<String, String> pciCols = new HashMap<String, String>();
			pciCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.1100.80.1.2");
			pciCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.1100.80.1.5");
			pciCols.put("DescriptionName", ".1.3.6.1.4.1.674.10892.5.4.1100.80.1.9");
			getAndPrintSelectedCol("pCIDeviceTable", pciCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.systemDetailsGroup.deviceGroup.networkDeviceTable
			HashMap<String, String> netCols = new HashMap<String, String>();
			netCols.put("Index", ".1.3.6.1.4.1.674.10892.5.4.1100.90.1.2");
			netCols.put("Status", ".1.3.6.1.4.1.674.10892.5.4.1100.90.1.3");
			netCols.put("ConnectionStatus", ".1.3.6.1.4.1.674.10892.5.4.1100.90.1.4");
			netCols.put("ProductName", ".1.3.6.1.4.1.674.10892.5.4.1100.90.1.6");
			getAndPrintSelectedCol("networkDeviceTable", netCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.storageDetailsGroup.software.storageManagement.physicalDevices.physicalDiskTable
			// not support for iDRAC8
			HashMap<String, String> pdiskCols = new HashMap<String, String>();
			pdiskCols.put("Name", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.2");
			pdiskCols.put("Manufacturer", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.3");
			pdiskCols.put("State", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.4");
			pdiskCols.put("ProductID", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.6");
			pdiskCols.put("SerialNo", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.7");
			pdiskCols.put("CapacityInMB", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.11");
			pdiskCols.put("ManufactureYear", ".1.3.6.1.4.1.674.10892.5.5.1.20.130.4.1.34");
			getAndPrintSelectedCol("physicalDiskTable", pdiskCols);

			// iDRAC-SMIv2.mib
			// .iso.org.dod.internet.private.enterprises.dell.server3.outOfBandGroup.storageDetailsGroup.software.storageManagement.logicalDevices.virtualDiskTable
			// not support for iDRAC8
			HashMap<String, String> vdiskCols = new HashMap<String, String>();
			vdiskCols.put("Name", ".1.3.6.1.4.1.674.10892.5.5.1.20.140.1.1.2");
			vdiskCols.put("State", ".1.3.6.1.4.1.674.10892.5.5.1.20.140.1.1.4");
			vdiskCols.put("DiskSizeInMB", ".1.3.6.1.4.1.674.10892.5.5.1.20.140.1.1.6");
			getAndPrintSelectedCol("virtualDiskTable", vdiskCols);
		} else if ("test".equalsIgnoreCase(vendor)) {
			HashMap<String, String> cols = new HashMap<String, String>();
			cols.put("ifDescr", ".1.3.6.1.2.1.2.2.1.2");
			cols.put("ifOperStatus", ".1.3.6.1.2.1.2.2.1.8");
			cols.put("ifMtu", ".1.3.6.1.2.1.2.2.1.4");
			getAndPrintSelectedCol("ifTable", cols);
		}
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
			
			ArrayList statusProbeEnum = new ArrayList();
			statusProbeEnum.add("voltageProbeTable");
			statusProbeEnum.add("amperageProbeTable");
			statusProbeEnum.add("coolingDeviceTable");
			statusProbeEnum.add("temperatureProbeTable");
			
			ArrayList objectStatusEnum = new ArrayList();
			objectStatusEnum.add("processorDeviceTable");
			objectStatusEnum.add("memoryDeviceTable");
			objectStatusEnum.add("pCIDeviceTable");
			objectStatusEnum.add("networkDeviceTable");
			
			
			if(statusProbeEnum.contains(tableName) && "Status".equalsIgnoreCase(name)) {
				result = number2Value(result, "StatusProbeEnum");
			}else if(objectStatusEnum.contains(tableName) && "Status".equalsIgnoreCase(name)) {
				result = number2Value(result, "ObjectStatusEnum");
			}else if("physicalDiskTable".equals(tableName) && "State".contentEquals(name)) {
				result = number2Value(result, "physicalDiskState");
			}else if("virtualDiskTable".equals(tableName) && "State".contentEquals(name)) {
				result = number2Value(result, "virtualDiskTable");
			}else if("networkDeviceTable".equals(tableName) && "ConnectionStatus".contentEquals(name)) {
				result = number2Value(result, "NetworkDeviceConnectionStatusEnum");
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

	}

	public String number2Value(String number, String type) {
		String array[] = null;
		// got below number vs value mapping from iDRAC-SMIv1.mib
		String ObjectStatusEnum[] = { "NA", "other", "unknown", "ok", "nonCritical", "critical", "nonRecoverable" };
		String PowerStateStatusEnum[] = { "NA", "other", "unknown", "off", "on" };
		String StateCapabilitiesFlags[] = { "NA", "unknownCapabilities", "enableCapable", "NA", "notReadyCapable", "NA",
				"enableAndNotReadyCapable" };
		String StateSettingsFlags[] = { "NA", "unknown", "enabled", "NA", "notReady", "NA", "enabledAndNotReady" };
		String StatusProbeEnum[] = { "NA", "other", "unknown", "ok", "nonCriticalUpper", "criticalUpper",
				"nonRecoverableUpper", "nonCriticalLower", "criticalLower", "nonRecoverableLower", "failed" };
		String StatusRedundancyEnum[] = { "NA", "other", "unknown", "full", "degraded", "lost", "notRedundant",
				"redundancyOffline" };
		String RacTypeEnum[] = { "NA", "other", "unknown", "idrac7monolithic", "idrac7modular", "lost", "notRedundant",
				"redundancyOffline" };
		String physicalDiskState[] = { "NA", "unknown", "ready", "online", "foreign", "offline", "blocked", "failed",
				"nonraid", "removed", "readonly" };
		String virtualDiskState[] = { "NA", "unknown", "online", "failed", "degraded" };
		String NetworkDeviceConnectionStatusEnum[] = { "unknown", "connected", "disconnected", "driverBad", "driverDisabled","NA","NA","NA","NA","NA","hardwareInitalizing","hardwareResetting","hardwareClosing","hardwareNotReady" };
		switch (type) {
		case "ObjectStatusEnum":
			array = ObjectStatusEnum;
			break;
		case "PowerStateStatusEnum":
			array = PowerStateStatusEnum;
			break;
		case "StateCapabilitiesFlags":
			array = StateCapabilitiesFlags;
			break;
		case "StateSettingsFlags":
			array = StateSettingsFlags;
			break;
		case "StatusProbeEnum":
			array = StatusProbeEnum;
			break;
		case "StatusRedundancyEnum":
			array = StatusRedundancyEnum;
			break;
		case "RacTypeEnum":
			array = RacTypeEnum;
			break;
		case "physicalDiskState":
			array = physicalDiskState;
			break;
		case "virtualDiskState":
			array = virtualDiskState;
			break;
		case "NetworkDeviceConnectionStatusEnum":
			array = NetworkDeviceConnectionStatusEnum;
			break;
		default:
			return number;
		}
		try {
			int result_N = Integer.parseInt(number);
			if (result_N <= array.length) {
				number = array[result_N];
			}
		} catch (Exception e) {
			logger.info("-------------"+number+"-----------------");
			logger.info(e.toString());
		}
		
		
		return number;
	}

	public ArrayList number2Value(ArrayList numbers, String type) {
		for (int i = 0; i < numbers.size(); i++) {
			numbers.set(i, number2Value((String) numbers.get(i), type));
		}
		return numbers;
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
		String vendor = "dell";
		APMonitor_DelliDRAC client = new APMonitor_DelliDRAC();
		client.logger.info("\n------------ Version: " + VER + " ----------");
		String host = "192.168.0.19";
		String community = "public";
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
			client.processiDRACData(vendor);
		} else {
			System.out.println("script_availability=1");
			System.out.println("script_message=Can not connect to snmp agent");
		}

		System.exit(0);
	}
}
