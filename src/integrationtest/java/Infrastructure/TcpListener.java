package Infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class TcpListener implements AutoCloseable{

    private final static Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private BlockingQueue<String> authorizationCodeQueue;
    private BlockingQueue<Boolean> tcpStartUpNotificationQueue;
    private int port;
    private Thread serverThread;


    public TcpListener(BlockingQueue<String> authorizationCodeQueue,
                       BlockingQueue<Boolean> tcpStartUpNotificationQueue){
        this.authorizationCodeQueue = authorizationCodeQueue;
        this.tcpStartUpNotificationQueue = tcpStartUpNotificationQueue;
    }

    public void startServer(){
        Runnable serverTask = () -> {
            try(ServerSocket serverSocket = new ServerSocket(0)) {
                port = serverSocket.getLocalPort();
                LOG.info("... Listening on port: " + port);
                tcpStartUpNotificationQueue.put(Boolean.TRUE);
                LOG.info("... TCP listener started");
                Socket clientSocket = serverSocket.accept();
                LOG.info("... server socket accepted");
                new ClientTask(clientSocket).run();
            } catch (Exception e) {
                LOG.error("Unable to process client request: " + e.getMessage());
                throw new RuntimeException("Unable to process client request: " + e.getMessage());
            }
        };
        LOG.info("... Creating new thread");
        serverThread = new Thread(serverTask);
        LOG.info("... starting new thread");
        serverThread.start();
    }

    private class ClientTask implements Runnable {
        private final Socket clientSocket;

        private ClientTask(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            LOG.info("... Running Client Task");
            StringBuilder builder = new StringBuilder();
            try(BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()))) {
                LOG.info("... Reading from client socket");
                String line = in.readLine();
                LOG.info("... First line" + line);
                while(!line.equals("")){
                    builder.append(line);
                    line = in.readLine();
                }
                authorizationCodeQueue.put(builder.toString());
            } catch (Exception e) {
                LOG.error("Error reading response from socket: " + e.getMessage());
                throw new RuntimeException("Error reading response from socket: " + e.getMessage());
            } finally {
                try {
                    LOG.info("... Closing client socket");
                    clientSocket.close();
                } catch (IOException e) {
                    LOG.error("Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void close(){
        serverThread.interrupt();
    }
}
