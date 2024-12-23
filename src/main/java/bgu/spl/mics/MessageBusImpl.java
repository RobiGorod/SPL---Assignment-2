package bgu.spl.mics;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	
	private static MessageBus instance;

    // Maps a microservice to its queue of messages
    private final Map<MicroService, BlockingQueue<Message>> microServiceQueues;

    // Maps an event type to the list of subscribing microservices
    private final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers;

    // Maps a broadcast type to the list of subscribing microservices
    private final Map<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers;

	// Private constructor
    private MessageBusImpl() {};

	// Static method to get the single instance
    public static synchronized MessageBus getInstance() {
        if (instance == null) {
            instance = new MessageBusImpl();
        }
        return instance;
    }

	
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendBroadcast(Broadcast b) {
		// TODO Auto-generated method stub

	}

	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void register(MicroService m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregister(MicroService m) {
		// TODO Auto-generated method stub

	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	

}
