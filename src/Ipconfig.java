class Ipconfig {
    public String ip = "";
    public String port = "";

    public static void main(String args[]) {
    }

    Ipconfig(String ip1, String port1)  {
        ip = ip1;
        port = port1;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }
}
