/**
 * Deployment configuration for dbp-prv
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


deployment(name: 'norm-provider') {
    groups getInitialMemberGroups();

    codebase getCodebase()

    artifact id: 'norm-dl', 'org.apache.river:norm-dl:2.2.1'
    artifact id: 'norm-cfg', "org.sorcersoft.sorcer:norm-cfg:" + getSorcerVersion()

    service(name: 'Norm') {
        interfaces {
            classes 'com.sun.jini.norm.NormServer'
            artifact ref: 'norm-dl'
        }
        implementation(class: 'com.sun.jini.norm.TransientNormServerImpl') {
            artifact ref: 'norm-cfg'
        }
        configuration file: 'classpath:norm.config'
        maintain 1
    }

}