import java.io.IOException;
import java.net.Socket;

/**
 * This is a test program for checking the functionality
 * of TimedSocket
 */
class Main {

   static TimedSocket ts;
   static Socket sock;
   static boolean socketDone = false;
   static Object doneLock = new Object();

   public static void main(String args[]) throws IOException {

        // Configure the server that does not respond or
        // takes long time to connect.
        String server = "hdcookbook.com";
        int port = 389;
        long timeout = 5000; // 5 secs

        if (args.length > 0) {
            if (args[0].equals("-help")) {
                printUsageAndExit();
            }
            server = args[0];
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        ts = new TimedSocket (server, port, timeout);
        System.out.println("Trying for socket connection to: " + server +
                         ":" + port + " with timeout:" + timeout + " ms.");
        System.out.println("Testing timeout ...");
        try {
            sock = ts.getSocket();
            System.out.println("Got the socket:" + sock);
            sock.close();       
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Try abort
        ts = new TimedSocket (server, port, timeout);
        System.out.println("Trying for socket connection to: " + server +
                         ":" + port + " with timeout:" + timeout + " ms.");
        System.out.println("Testing aborting the timed connection ...");
        Runnable r = new Runnable() {
            public void run() {
                try {
                    sock = ts.getSocket();
                    System.out.println("Got the socket:" + sock);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized(doneLock) {
                    socketDone = true;
                    doneLock.notifyAll();
                }
            }   
        };
        Thread connThread = new Thread(r, "Connect Thread");
        connThread.start();

        synchronized (doneLock) {
            try {
                // adjust the wait time to be well within the socket timeout.
                doneLock.wait(3000);
            } catch (InterruptedException e) {
                // ignore in this case
            }
            if (!socketDone) { 
                ts.abort();
            }
        }
   }

  static void printUsageAndExit() {
        System.out.println("Usage:");
        System.out.println("java Main -- Uses the default host name and port number");
        System.out.println("java Main <host name> <port number>");
        System.exit(1);
  }
}
