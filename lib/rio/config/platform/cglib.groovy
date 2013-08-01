import sorcer.rio.util.SorcerCapabilityDescriptor

def getPlatformCapabilityConfig() {
    def cap = new SorcerCapabilityDescriptor()
    cap.name = 'CGLib'
    cap.version = '2.1_3'
    cap.setClasspath('cglib:cglib-nodep')

    return cap;
}