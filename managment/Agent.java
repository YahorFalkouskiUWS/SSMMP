package managment;

public class Agent {
    private Manager manager;

    public Agent(Manager manager) {
        this.manager = manager;
    }

    public ServiceInfo getServiceInfo(String serviceName) {
        return manager.getServiceInfo(serviceName);
    }
}
