package com.skodev.REST;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class HttpServer {

    private Connection connection;
    private Container container;
    private boolean isWorking = false;

    public HttpServer(Container container) {
        this.container = container;
    }
    
    public void start(int onPort) throws IOException {
        Server server = new ContainerServer(container);
        connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(onPort);
        connection.connect(address);
        isWorking = true;
    }

    public void stop() throws IOException {
        if (connection != null) {
            connection.close();
            isWorking = false;
        }
    }

    public boolean isWorking() {
        return isWorking;
    }   
}
