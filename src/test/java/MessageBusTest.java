import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.Event;
import bgu.spl.mics.Future;
import bgu.spl.mics.Message;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;

public class MessageBusTest {
    private MessageBusImpl messageBus;
    private MicroService microService1;
    private MicroService microService2;

    @BeforeEach
    public void setUp() {
        messageBus = (MessageBusImpl) MessageBusImpl.getInstance();

        // Mock microservices for testing
        microService1 = new MicroService("Service1") {
            @Override
            protected void initialize() {
            }
        };

        microService2 = new MicroService("Service2") {
            @Override
            protected void initialize() {
            }
        };
    }

    @Test
    public void testRegisterAndUnregister() {
        // Register microservices
        messageBus.register(microService1);
        messageBus.register(microService2);

        // Subscribe microservices to the TestBroadcast
        messageBus.subscribeBroadcast(TestBroadcast.class, microService1);
        messageBus.subscribeBroadcast(TestBroadcast.class, microService2);

        // Add a dummy broadcast message to ensure queues are not empty
        TestBroadcast broadcast = new TestBroadcast();
        messageBus.sendBroadcast(broadcast);

        // Verify registration by checking if the queue exists
        assertNotNull(messageBus.microServiceQueues.get(microService1));
        assertNotNull(messageBus.microServiceQueues.get(microService2));

        // Verify registration by checking that microservices can receive messages
        assertDoesNotThrow(() -> {
            Message message1 = messageBus.awaitMessage(microService1);
            assertEquals(broadcast, message1, "microService1 should have received the broadcast.");
        });

        assertDoesNotThrow(() -> {
            Message message2 = messageBus.awaitMessage(microService2);
            assertEquals(broadcast, message2, "microService2 should have received the broadcast.");
        });

        // Unregister a microservice
        messageBus.unregister(microService1);

        // Verify unregistration by checking if the queue was removed
        assertNull(messageBus.microServiceQueues.get(microService1));


        // Verify unregistration
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(microService1),
                "Unregistered microService1 should throw an exception when awaiting messages.");

    }

    @Test
    public void testSubscribeEventAndSendEvent() throws InterruptedException {
        // Register microservices
        messageBus.register(microService1);

        // Subscribe microservice1 to a specific event type
        messageBus.subscribeEvent(TestEvent.class, microService1);

        // Send an event
        TestEvent event = new TestEvent();
        Future<String> future = messageBus.sendEvent(event);

        // Verify the event was received
        Message receivedMessage = messageBus.awaitMessage(microService1);
        assertEquals(event, receivedMessage);

        // Complete the event and resolve the Future
        messageBus.complete(event, "Success");
        assertTrue(future.isDone());
        assertEquals("Success", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testSubscribeBroadcastAndSendBroadcast()  {
        // Register microservices
        messageBus.register(microService1);
        messageBus.register(microService2);

        // Subscribe microservices to the TestBroadcast
        messageBus.subscribeBroadcast(TestBroadcast.class, microService1);
        messageBus.subscribeBroadcast(TestBroadcast.class, microService2);

        // Add a dummy broadcast message to ensure queues are not empty
        TestBroadcast broadcast = new TestBroadcast();
        messageBus.sendBroadcast(broadcast);

        // Verify registration by checking if the queue exists
        assertNotNull(messageBus.microServiceQueues.get(microService1), "Queue for microService1 should exist.");
        assertNotNull(messageBus.microServiceQueues.get(microService2), "Queue for microService2 should exist.");

        // Verify broadcast was received by both microservices
        assertDoesNotThrow(() -> {
            Message message1 = messageBus.awaitMessage(microService1);
            assertEquals(broadcast, message1, "microService1 should have received the broadcast.");
        });

        assertDoesNotThrow(() -> {
            Message message2 = messageBus.awaitMessage(microService2);
            assertEquals(broadcast, message2, "microService2 should have received the broadcast.");
        });

        // Unregister microService1
        messageBus.unregister(microService1);

        // Verify unregistration by checking if the queue was removed
        assertNull(messageBus.microServiceQueues.get(microService1), "Queue for microService1 should be removed after unregistration.");

        // Verify unregistration throws exception when awaiting messages
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(microService1),
                "Unregistered microService1 should throw an exception when awaiting messages.");
    }

    @Test
    public void testRoundRobinEventDispatch() {
        // Register microservices
        messageBus.register(microService1);
        messageBus.register(microService2);

        // Subscribe both microservices to an event type
        messageBus.subscribeEvent(TestEvent.class, microService1);
        messageBus.subscribeEvent(TestEvent.class, microService2);

        // Verify registration by checking if the queues exist
        assertNotNull(messageBus.microServiceQueues.get(microService1), "Queue for microService1 should exist.");
        assertNotNull(messageBus.microServiceQueues.get(microService2), "Queue for microService2 should exist.");

        // Send two events
        TestEvent event1 = new TestEvent();
        TestEvent event2 = new TestEvent();
        messageBus.sendEvent(event1);
        messageBus.sendEvent(event2);

        // Verify round-robin
        assertDoesNotThrow(() -> {
            Message receivedMessage1 = messageBus.awaitMessage(microService1);
            assertEquals(event1, receivedMessage1, "microService1 should have received the first event.");
        });

        assertDoesNotThrow(() -> {
            Message receivedMessage2 = messageBus.awaitMessage(microService2);
            assertEquals(event2, receivedMessage2, "microService2 should have received the second event.");
        });

        // Unregister microService1
        messageBus.unregister(microService1);

        // Verify unregistration by checking if the queue was removed
        assertNull(messageBus.microServiceQueues.get(microService1), "Queue for microService1 should be removed after unregistration.");

        // Verify unregistration throws exception when awaiting messages
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(microService1),
                "Unregistered microService1 should throw an exception when awaiting messages.");
    }

    @Test
    public void testAwaitMessageThrowsExceptionForUnregisteredMicroService() {
        // Try to call awaitMessage for an unregistered microService
        assertThrows(IllegalStateException.class, () -> {
            messageBus.awaitMessage(microService1);
        }, "awaitMessage should throw an IllegalStateException for unregistered microService.");
    }

    @Test
    public void testAwaitMessageBlocksUntilMessageIsAvailable() throws InterruptedException {
        // Register microService
        messageBus.register(microService1);
        messageBus.subscribeBroadcast(TestBroadcast.class, microService1);

        // Start a thread to add a message after a short delay
        Thread senderThread = new Thread(() -> {
            try {
                System.out.println("Thread: Starting delay...");
                Thread.sleep(200); // Simulate delay before sending the broadcast
                System.out.println("Thread: Sending broadcast...");
                messageBus.sendBroadcast(new TestBroadcast()); // Send the broadcast
            } catch (InterruptedException e) {
                System.out.println("Thread: Interrupted!");
                Thread.currentThread().interrupt();
            }
        });

        // Start the sender thread
        senderThread.start();

        // Call awaitMessage and measure the time taken
        System.out.println("Main Thread: Calling awaitMessage...");
        long startTime = System.currentTimeMillis();
        Message message = messageBus.awaitMessage(microService1); // Blocking call
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Ensure elapsed time is at least the delay introduced by the sender thread
        System.out.println("Main Thread: Verifying results...");
        assertTrue(elapsedTime >= 200, "awaitMessage should block until a message is available.");
        assertTrue(elapsedTime < 1000, "awaitMessage should not block indefinitely.");
        assertNotNull(message, "awaitMessage should return a message.");
        assertTrue(message instanceof TestBroadcast, "The message type should be TestBroadcast.");

        // Wait for the sender thread to complete
        senderThread.join();
        System.out.println("Main Thread: Test completed successfully.");
    }

    // Helper classes for testing
    private static class TestEvent implements Event<String> {
    }

    private static class TestBroadcast implements Broadcast {
    }


    @AfterEach
    public void tearDown() {
        // Unregister microservices to clean up resources
        messageBus.unregister(microService1);
        messageBus.unregister(microService2);

        // Clear internal data structures if necessary
        clearMessageBusData();

        System.out.println("TearDown: Cleaned up all resources after test.");
    }

    private void clearMessageBusData() {
        // Access and clear internal data structures
        messageBus.microServiceQueues.clear();
        messageBus.eventSubscribers.clear();
        messageBus.broadcastSubscribers.clear();
        messageBus.eventFutures.clear();
        System.out.println("TearDown: Cleared internal data structures of MessageBus.");
    }
}