// import static org.junit.Assert.*;
// import static org.mockito.Mockito.*;

// import java.net.Socket;
// import java.util.concurrent.ConcurrentHashMap;

// import org.junit.Before;
// import org.junit.Test;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;

// public class MessageBrokerTest {

//     private MessageBroker messageBroker;

//     @Mock
//     private Socket mockSocket;

//     @Before
//     public void setUp() {
//         MockitoAnnotations.initMocks(this);
//         messageBroker = new MessageBroker();
//     }

//     @Test
//     public void testHeartbeatMessage() {
//         String heartbeatMessage = "type:heartbeat";
//         ConcurrentHashMap<Socket, Long> connectedServers = new ConcurrentHashMap<>();
//         messageBroker.setConnectedServers(connectedServers);

//         messageBroker.handleMessage(heartbeatMessage, mockSocket);

//         assertTrue(connectedServers.containsKey(mockSocket));
//     }

//     @Test
//     public void testBroadcastMessage() {
//         String normalMessage = "Hello, World!";
//         messageBroker = spy(new MessageBroker());

//         doNothing().when(messageBroker).broadcastMessage(normalMessage, mockSocket);

//         messageBroker.handleMessage(normalMessage, mockSocket);

//         verify(messageBroker, times(1)).broadcastMessage(normalMessage, mockSocket);
//     }

//     @Test
//     public void testRemoveStaleServer() {
//         ConcurrentHashMap<Socket, Long> connectedServers = new ConcurrentHashMap<>();
//         connectedServers.put(mockSocket, System.currentTimeMillis() - MessageBroker.HEARTBEAT_TIMEOUT - 1);
//         messageBroker.setConnectedServers(connectedServers);

//         messageBroker.new HeartbeatMonitor().run();

//         assertFalse(connectedServers.containsKey(mockSocket));
//     }
// }