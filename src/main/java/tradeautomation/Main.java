package tradeautomation;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;

import tradeautomation.service.strategies.macdStrategy;

/**
 * This small program demonstrates how to initialize Dukascopy client and start a strategy
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static String jnlpUrl = "http://platform.dukascopy.com/demo/jforex.jnlp";
    private static String userName = "username";
    private static String password = "password";

    private static IClient client;

    private static int lightReconnects = 3;

    public static void main(String[] args) throws Exception {
        //get the instance of the IClient interface
        client = ClientFactory.getDefaultInstance();

        setSystemListener();
        tryToConnect();
        subscribeToInstruments();

        LOGGER.info("Starting strategy");
        client.startStrategy(new macdStrategy());
        //now it's running
    }

    private static void setSystemListener() {
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {

        	@Override
        	public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
        	}

			@Override
			public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
			}

			@Override
			public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
			}

			@Override
			public void onDisconnect() {
                tryToReconnect();
			}
        });
    }

    private static void tryToConnect() throws Exception {
        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }
    }

    private static void tryToReconnect() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (lightReconnects > 0) {
                    client.reconnect();
                    --lightReconnects;
                } else {
                    do {
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e) {
                        }
                        try {
                            if(client.isConnected()) {
                                break;
                            }
                            client.connect(jnlpUrl, userName, password);

                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    } while(!client.isConnected());
                }
            }
        };
        new Thread(runnable).start();
    }

    private static void subscribeToInstruments() {
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
    }
}
