import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;

import com.adventnet.snmp.snmp2.SnmpOID;

import java.util.Properties;

public class APActiveClientCount {
	static String VER = "19.83001";
	public static void main(String[] args) {
		ArrayList apNames = new ArrayList();
		ArrayList clientCount = new ArrayList();
		int ver = 0;
		String verStr = "v1";
		String vendor = "cisco";
		String printAll = "false";
		String autoOffline = "false";
		APSnmpTest client = new APSnmpTest();
		String host = "192.168.0.19";
		String community = "public";
		if (args.length > 1) {
			host = args[0];
			community = args[1];
			try {
				verStr = args[2];
				ver = ("v2c".equals(verStr.trim()))?1:0;  //0 = v1  1=v2c
			} catch (Exception e) {
			}
			try {
				vendor = args[3];
			} catch (Exception e) {
			}
			try {
				printAll = args[4];
			} catch (Exception e) {
			}
			try {
				autoOffline = args[5];
			} catch (Exception e) {
			}
		}

		client.target.setTargetHost(host);
		client.target.setTargetPort(161);
		client.target.setCommunity(community);
		client.target.setSnmpVersion(ver); 
		client.logger.info("\n------------ Script version: "+VER+" ----------");
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
		//check if the snmap target connectable or not
		client.logger.info("timeout:"+client.target.getTimeout()+" retry:"+client.target.getRetries());
		SnmpOID sysoid = new SnmpOID(".1.3.6.1.2.1.1.2.0");
		client.target.setSnmpOID(sysoid);
		String result = client.target.snmpGet();
		if (result == null) {
			System.out.println("Can not connect to snmp agent. Host:"+client.target.getTargetHost()+" community:"+client.target.getCommunity()+" timeout:"+client.target.getTimeout()+" retry:"+client.target.getRetries());
			System.exit(1024);
		}
		
		// apnames.properties content: <ap name> = <active client count> , if previous ap not available this time, it would be  <ap name> = -1
		String filename="apactiveclientcount"+host+".properties";
		Properties previousApNames = new Properties();
		try {
			previousApNames.load(new FileInputStream(filename));
		} catch (Exception e) {
		}
		
		String aPNamesOid = ".1.3.6.1.4.1.9.9.513.1.1.1.1.5";  // Cisco's AP name list oid
		String apValueOid = ".1.3.6.1.4.1.9.9.513.1.1.1.1.72"; // Cisco's AP ActiveclientCount oid
		if("h3c".equals(vendor)) {
			aPNamesOid = ".1.3.6.1.4.1.25506.2.75.2.1.1.1.5";  // h3c's AP name list oid  [hh3c-dot11-apmt.mib] 
			apValueOid = ".1.3.6.1.4.1.25506.2.75.2.1.1.1.4"; // h3c's AP status oid  
		}
		if("huawei".equals(vendor)) {
			aPNamesOid = ".1.3.6.1.4.1.2011.6.139.13.3.3.1.4";  // hauwei's AP name list oid  [HUAWEI-WLAN-AP.mib] 
			apValueOid = ".1.3.6.1.4.1.2011.6.139.13.3.3.1.6"; // huawei's AP status oid  hwWlanApRunState INTEGER  { idle ( 1 ) , autofind ( 2 ) , typeNotMatch ( 3 ) , fault ( 4 ) , config ( 5 ) , configFailed ( 6 ) , download ( 7 ) , normal ( 8 ) , committing ( 9 ) , commitFailed ( 10 ) , standby ( 11 ) , verMismatch ( 12 ) , nameConflicted ( 13 ) , invalid ( 14 ) , countryCodeMismatch ( 15 ) } 
		}
		if("ruijie".equals(vendor)) {
			aPNamesOid = ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.2";  // Ruijie's AP name list oid  [RUIJIE-AC-MGMT.mib, ruijieMgmt.ruijieAcMgmtMIB.ruijieAcMgmtApMIBObjects.ruijieAcMgmtApg.ruijieApgCfgTable] 
			apValueOid = ".1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.34"; //  "This object represents Sta Num attach to the AP." 
			// ruijieApState oid: .1.3.6.1.4.1.4881.1.1.10.2.56.2.1.1.1.48,  
			//"This object represents the AP's state.1 -- on line;2 -- off line"
			// But it only list online ap(value is 1). tested in ws6008
		}
		apNames = client.getByColumn(aPNamesOid);
		clientCount = client.getByColumn(apValueOid);
		String errorMsg = "";
		String dataLines = "Data:\n";
//		for(int i=0; i<apNames.size(); i++){
//			System.out.println(apNames.get(i)+"\t"+clientCount.get(i));
//		}
		
		//to check if this a new ap, and set name and active client count
		for(int i=0;i<apNames.size();i++){
			String name = (String) apNames.get(i);
			if(!previousApNames.containsKey(name)) {
        		previousApNames.put(name, clientCount.get(i));
        		//todo: more actions for new found ap
			}else{
				// to print ealier offline ap, this helps with clearing the alert.
				if(!"true".equalsIgnoreCase(printAll) && "-1".equals(previousApNames.get(name))) {
			
						dataLines+=name+"\t"+clientCount.get(i).toString()+"\n";
					
				}
				previousApNames.setProperty(name, clientCount.get(i).toString());
			}
		}

		String count2Print = "0";
		count2Print = String.valueOf(apNames.size());
		dataLines+="OnlineAP\t"+count2Print+"\n";
		// to check if an ap lost, set it value to -1
		Enumeration<?> pnames = previousApNames.propertyNames();
		while (pnames.hasMoreElements()) {
            String name = (String) pnames.nextElement();
            // autoOffline let the -1 AP only print and save once
			if("true".equalsIgnoreCase(autoOffline) && "-1".equals(previousApNames.get(name))) {
				client.logger.info(name + " autoOffline is true and it in previous list, so to remove it.");	
				previousApNames.remove(name);
				continue;
			}
			// add new offline ap
            if(!apNames.contains(name)) {
            		previousApNames.setProperty(name, "-1");
            }
        } 
		if(previousApNames.containsValue("-1")) {
            errorMsg = "Message:Offline AP:";          
			Iterator iter = previousApNames.keySet().iterator();
			while(iter.hasNext()) {
				String k = (String) iter.next();
				String v = previousApNames.getProperty(k);
				if ("-1".equals(v)) {
					dataLines+=k+"\t-1\n";
					errorMsg += k+"\t";
				}
//				if to many ap there, opm page load slow with script monitor page
//				else {
//					System.out.println(k+"\t"+v);
//				}
			}
        }else {
        	if("true".equalsIgnoreCase(printAll)) {
        		Iterator iter = previousApNames.keySet().iterator();
    			while(iter.hasNext()) {
    				String k = (String) iter.next();
    				String v = previousApNames.getProperty(k);
    				dataLines+=k+"\t"+v+"\n";

    			}
        	}
			
        	errorMsg ="Message: No issue found";
        }
		System.out.println(errorMsg);
		System.out.println(dataLines);

		try {
			previousApNames.store(new FileOutputStream(filename), "store all ap, value means active client count, -1=offline; h3c device means ap status; cisco means activeApCount");
		} catch (Exception e) {
			client.logger.info("failed to write apactiveclientcount.properties");
		}
		
		System.exit(0);
	}

}
