package database;

public enum HistoryEvent {
	CHAT_FROM(false),
	CHAT_TO(false),
	REQUEST_TO_MADE(false),
	REQUEST_FROM_MADE(false),
	REQUEST_TO_ACCEPTED(false),
	REQUEST_FROM_ACCEPTED(false),
	
	JOIN(true),
	LEAVE(true),
	CREATE(true),
	CLOSE(true),
	FILE_SEND(true);
	
	HistoryEvent(boolean flag) {
		isArenaEvent = flag;
	}
	
	boolean isArenaEvent;
	public boolean isArenaEvent() {
		return isArenaEvent;
	}
}
