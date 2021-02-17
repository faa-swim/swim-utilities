package us.dot.faa.swim.utilities;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissedMessageTracker extends TimerTask {

	private final static Logger _logger = LoggerFactory.getLogger(MissedMessageTracker.class);
	private Timer _timer;

	private int scheduleTimeInSeconds = 10;
	private int missedMessageTriggerTimeInMinutes = 5;
	private int staleMessageTriggerTimeInMinutes = 10;
	private long _lastRecievedTrackingId = -1;
	private Instant _lastMessageRecievedTime = Instant.now();
	private ConcurrentHashMap<Long, Instant> _missedMessagesHashMap = new ConcurrentHashMap<Long, Instant>();

	public MissedMessageTracker() {}
	
	public MissedMessageTracker(int scheduleTimeInSeconds, int missedMessageTriggerTimeInMinutes,
			int staleMessageTriggerTimeInMinutes) {
		this.scheduleTimeInSeconds = scheduleTimeInSeconds;
		this.missedMessageTriggerTimeInMinutes = missedMessageTriggerTimeInMinutes;
		this.staleMessageTriggerTimeInMinutes = staleMessageTriggerTimeInMinutes;
	}

	public int getScheduleTimeInSeconds()
	{
		return this.scheduleTimeInSeconds;
	}
	
	public int getMissedMessageTriggerTimeInMinutes()
	{
		return this.missedMessageTriggerTimeInMinutes;
	}	
	
	public int getStaleMessageTriggerTimeInMinutes()
	{
		return this.staleMessageTriggerTimeInMinutes;
	}
	
	public void setScheduleTimeInSeconds(int scheduleTimeInSeconds)
	{
		this.scheduleTimeInSeconds = scheduleTimeInSeconds;
	}
	
	public void setMissedMessageTriggerTimeInMinutes(int missedMessageTriggerTimeInMinutes)
	{
		this.missedMessageTriggerTimeInMinutes = missedMessageTriggerTimeInMinutes;
	}	
	
	public void setStaleMessageTriggerTimeInMinutes(int staleMessageTriggerTimeInMinutes)
	{
		this.staleMessageTriggerTimeInMinutes = staleMessageTriggerTimeInMinutes;
	}
	
	public Instant getLastMessageRecievedTime() {
		return _lastMessageRecievedTime;
	}

	public void setLastMessageRecievedTime(Instant lastMessageRecievedTime) {
		_lastMessageRecievedTime = lastMessageRecievedTime;
	}

	public long getLastRecievedTrackingId() {
		return _lastRecievedTrackingId;
	}

	public void setLastRecievedTrackingId(long lastRecievedTrackingId) {
		_lastRecievedTrackingId = lastRecievedTrackingId;
	}

	public synchronized void put(long trackingId, Instant recievedTime) {

		_lastMessageRecievedTime = recievedTime;
		
		if (_lastRecievedTrackingId == -1) {
			_lastRecievedTrackingId = trackingId;
			_logger.debug("Setting last recieved tracking it to: " + trackingId);
		} else {

			if (_missedMessagesHashMap.remove(trackingId) != null) {
				_logger.debug("Removed " + trackingId + " from missed messges hash map");
			}

			if (trackingId - _lastRecievedTrackingId == 1) {
				_lastRecievedTrackingId = trackingId;
				_logger.debug("Setting last recieved tracking it to: " + trackingId);
			} else if (trackingId - _lastRecievedTrackingId > 1) {
				long countOfMissedMessages = trackingId - _lastRecievedTrackingId - 1;

				for (int i = 0; i < countOfMissedMessages; i++) {
					Long missedMessageId = _lastRecievedTrackingId + i + 1;
					_missedMessagesHashMap.put(missedMessageId, _lastMessageRecievedTime);
					_logger.debug("Missed Message Identified, putting in missed message hash map: " + missedMessageId);
				}

				_lastRecievedTrackingId = trackingId;
				_logger.debug("Setting last recieved tracking it to: " + trackingId);
			}
		}
	}

	public Map<Long, Instant> getMissedMessage() {
		return _missedMessagesHashMap.entrySet().stream().filter(
				m -> Duration.between(m.getValue(), Instant.now()).toMinutes() >= this.missedMessageTriggerTimeInMinutes)
				.collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));
	}

	public void clearAllMessages() {
		_missedMessagesHashMap.clear();
	}

	public void clearOnlyMissedMessages() {
		synchronized(_missedMessagesHashMap){
		_missedMessagesHashMap.entrySet().removeIf(
				m -> Duration.between(m.getValue(), Instant.now()).toMinutes() >= this.missedMessageTriggerTimeInMinutes);
		}
	}

	public void start() {
		if (_timer == null) {
			_timer = new Timer(true);
			_timer.scheduleAtFixedRate(this, 0, this.scheduleTimeInSeconds * 1000);
		}
	}

	public void stop() {
		if (_timer != null) {
			_timer.cancel();
		}
	}

	public void onMissed(Map<Long, Instant> missedMessages) {
		String cachedCorellationIds = missedMessages.entrySet().stream()
				.map(kvp -> kvp.getKey() + ":" + missedMessages.get(kvp.getKey()))
				.collect(Collectors.joining(", ", "{", "}"));

		_logger.warn("Missed message(s) identified: " + cachedCorellationIds);
	}

	public void onStale(Long lastRecievedId, Instant lastRecievedTime) {
		_logger.warn(
				"Stale message identified, last id " + lastRecievedId + " recieved at " + lastRecievedTime.toString());
	}

	@Override
	public void run() {
		Map<Long, Instant> missedMessages = getMissedMessage();

		if (!missedMessages.isEmpty()) {
			clearOnlyMissedMessages();
			onMissed(missedMessages);
		}

		if (Duration.between(_lastMessageRecievedTime, Instant.now()).toMinutes() > this.staleMessageTriggerTimeInMinutes) {
			onStale(_lastRecievedTrackingId, _lastMessageRecievedTime);
		}
	}
	
}
