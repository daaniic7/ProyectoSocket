public class Resource {
    int portMin = 50000;
    int portAsign;
    int maxClient = 30;

    public Resource() {
        portAsign = portMin;
    }

    public synchronized int devPort() {
        if (portAsign - portMin < maxClient)
            return portAsign++;
        else
            return -1;
    }

    public synchronized boolean endClient() {
        return (portAsign - portMin == maxClient + portMin);
    }
}
