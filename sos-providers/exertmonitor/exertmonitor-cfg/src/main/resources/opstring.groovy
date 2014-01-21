/**
 * Deployment configuration for exertmonitor-prv
 *
 * @author Pawel Rubach
 */
import sorcer.core.SorcerEnv;

String[] getInitialMemberGroups() {
    def groups = SorcerEnv.getLookupGroups();
    return groups as String[]
}

def getSorcerVersion() {
    return sorcerVersion = SorcerEnv.getSorcerVersion();
}

def String getCodebase() {
    return 'http://'+SorcerEnv.getLocalHost().getHostAddress()+":9010"
}


deployment(name: 'exertmonitor-provider') {
    groups getInitialMemberGroups();

    codebase getCodebase()

    artifact id:'exertmonitor-api', 'org.sorcersoft.sorcer:exertmonitor-api:'+getSorcerVersion()
    artifact id:'exertmonitor-cfg', 'org.sorcersoft.sorcer:exertmonitor-cfg:'+getSorcerVersion()

    service(name: "ExertMonitor") {
        interfaces {
            classes 'sorcer.core.monitor.MonitoringManagement'
            artifact ref: 'sos-exertlet-sui'
        }
        implementation(class: 'sorcer.core.provider.exertmonitor.ExertMonitor') {
            artifact ref: 'exertmonitor-cfg'
        }
        configuration file: 'classpath:exertmonitor.config'
        maintain 1
    }
}