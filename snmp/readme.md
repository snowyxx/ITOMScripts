## Descripton

Some scripts by SNMP. 

## History

`APActiveClientCount` 
1. 2019/8/27: check if the snmp target can be connect or not first and print target info if connect failed.
2. 2019/8/28: v19.82801; add version number and print it in log file.
3. 2019/8/30: v19.83001; add 'autoOffline' arg. default value is false. autoOffline let the -1 AP only print and save once (TSL requested)

`APMonitor_AppManager`
1. 2019/8/28: v19.82801; add version number and print it in log file.
2. 2019/11/22: v19.112201; help Liu Liang added a output table of HillstoneIpsec
## oids.properties

Conf file for such scirpts.

- add your custome oids 
    - table oid name must start with TABLE_  e.g. #TABLE_Error=.1.3.6.1.4.1.2.3.51.3.2.1.1
    - single item oid name must start with ATTRI_    e.g #ATTRI_Power\ Status=.1.3.6.1.4.1.2.3.51.3.5.1.1.0
- snmp.debug.mode=true Debug mode : set this to log snmp api debug info. only "true" means enable the log
- snmpGetTableByMib = false  : Only set to "true" to get a table data by snmpGetAllList() base on a mib file, otherwise will try to use snmpGetNext()
- maxRowsNumber = 100  :Max rows to be got of a table oid, works when snmpGetTableByMib is not true
- snmp.target.set.timeout : snmp timeout setting in second, default value is 5.
- snmp.target.set.retries : Sets the SNMP target retries value. Default is 0.

## APActiveClientCount.java

A script to monitor wifi controller get all AP list. For OpManager

__Usage__

`java -cp .;bin;lib\* APActiveClientCount <IP or host name> <snmp community> v2c <verdor:cisco|huawei|h3c|ruijie> <print all AP list(option):|true|false> <autoOffline(option):|true|false>`

## APMonitor_AppManager.java

A script to monitor wifi controller get all AP list. For Applications Manager

__Usage__

`java -cp .;bin;lib\* APMonitor_AppManager <IP or host name> <snmp community> v2c <verdor:cisco|huawei|h3c|ruijie>`

## APMonitor_DelliDRAC.java

A script to monitor Dell iDRAC. For Applications Manager

__Usage__

`java -cp .;bin;lib\* APMonitor_DelliDRAC <IP or host name> <snmp community> v2c


## IpmiSnmpTest.java

A script to get Impi data for Applications Manager - For IBM IMM

__Monitored data__

Default:

- TMP
- VOLT
- FAN
- Disk  (imm2 only)
- Power (imm2 only)
- CPU  (imm2 only)
- Memory
- Power Status
- System Status

Custome:

- Last 5 error log
- others

__Files__

`imm.mib` and `immalert.mib` : IMM mib files of IMM2

`imm_1.mib` : IMM mib files of IMM v1


__Usage__

`java -cp .;bin;lib\* IpmiSnmpTest <IP or host name> <snmp community>`

