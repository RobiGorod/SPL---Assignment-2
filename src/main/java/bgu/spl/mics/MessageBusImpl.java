package bgu.spl.mics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	private static class messageBusHolder{
		private static MessageBus instance = new MessageBusImpl();
	}

    // Maps a microservice to its queue of messages
	// Robi changed to public for tests
    public final Map<MicroService, BlockingQueue<Message>> microServiceQueues;

    // Maps an event type to the list of subscribing microservices
	public final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers;

    // Maps a broadcast type to the list of subscribing microservices
	public final Map<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers;

	// New field: Maps events to their corresponding Future objects
	public final Map<Event<?>, Future<?>> eventFutures;


	// Private constructor
    private MessageBusImpl() {
		microServiceQueues = new ConcurrentHashMap<>();
        eventSubscribers = new ConcurrentHashMap<>();
        broadcastSubscribers = new ConcurrentHashMap<>();
		eventFutures = new ConcurrentHashMap<>();
	};

	// Public method to get the singleton instance
    public static MessageBus getInstance() {
       return messageBusHolder.instance;
    }

	
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        eventSubscribers.putIfAbsent(type, new LinkedList<>());
		synchronized(eventSubscribers.get(type)){
        	eventSubscribers.get(type).add(m);
		}
    }

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		 broadcastSubscribers.putIfAbsent(type, new ArrayList<>());
		 synchronized(broadcastSubscribers.get(type)){
         broadcastSubscribers.get(type).add(m);
		 System.out.println("MicroService " + m.getName() + " subscribed to broadcast " + type.getSimpleName());

		 }

	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		 // Resolve the Future associated with the event
		 @SuppressWarnings("unchecked")
		 Future<T> future = (Future<T>) eventFutures.remove(e); // Remove the mapping after resolving
		 if (future != null) {
			 future.resolve(result); // Set the result of the Future
		 }
	 }

	@Override
	public void sendBroadcast(Broadcast b) {
		 List<MicroService> subscribers;
        synchronized (this) {
            subscribers = broadcastSubscribers.getOrDefault(b.getClass(), Collections.emptyList());
        }
        // Add the broadcast message to each subscriber's queue
		synchronized(subscribers){
        	for (MicroService microService : subscribers) {
            	BlockingQueue<Message> queue = microServiceQueues.get(microService);
            	if (queue != null) {
                	queue.add(b);
					System.out.println("Broadcast " + b.getClass().getSimpleName() + " sent to " + microService.getName());

				}
        	}
		}
    }


	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		Queue<MicroService> subscribers = eventSubscribers.getOrDefault(e.getClass(), null);

    	if (subscribers == null || subscribers.isEmpty()) {
        	return null; // No subscribers for this event
    	}

    	MicroService microService;
    	synchronized (subscribers) { // Synchronize only on the queue for round-robin
        	microService = subscribers.poll(); // Get the next subscriber
        	subscribers.add(microService); // Add it back to the queue
    	}

    	BlockingQueue<Message> queue = microServiceQueues.get(microService);
    	if (queue != null) {
        	Future<T> future = new Future<>(); // Create a Future for the event
        	eventFutures.put(e, future); // Store the mapping between the event and its Future
        	queue.add(e); // Add the event to the microservice's queue
			System.out.println("Event " + e.getClass().getSimpleName() + " sent to " + microService.getName());
        	return future; // Return the Future to the sender
    	}

    	return null;
	}
	

	@Override
	public void register(MicroService m) {
		microServiceQueues.putIfAbsent(m, new LinkedBlockingQueue<>());

	}

	@Override
	public void unregister(MicroService m) {
		microServiceQueues.remove(m);

		eventSubscribers.values().forEach(queue -> {
			synchronized (queue) {
				queue.remove(m);
			}
		});
	
		broadcastSubscribers.values().forEach(list -> {
			synchronized (list) {
				list.remove(m);
			}
		});
	}


	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		BlockingQueue<Message> queue = microServiceQueues.get(m);
        if (queue == null) {
            throw new IllegalStateException("Microservice is not registered.");
        }
		System.out.println("Awaiting message for MicroService " + m.getName());
		return queue.take(); // Blocking call until a message is available
    }
	

	

}
