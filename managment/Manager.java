package managment;

import java.util.HashMap;
import java.util.Map;

public class Manager {
    private Map<String, ServiceInfo> activeServices;

    public Manager() {
        this.activeServices = new HashMap<>();
    }

    public void startService(String serviceName, String ipAddress, int port) {
        activeServices.put(serviceName, new ServiceInfo(ipAddress, port));
        System.out.println("Service " + serviceName + " started on " + ipAddress + ":" + port);
    }

    public void stopService(String serviceName) {
        activeServices.remove(serviceName);
        System.out.println("Service " + serviceName + " stopped");
    }

    public ServiceInfo getServiceInfo(String serviceName) {
        return activeServices.get(serviceName);
    }
}
