package won.protocol.model.unread;

import java.util.Date;

public class UnreadMessageInfo {
	private long count;
	private Date newestTimestamp;
	private Date oldestTimestamp;
	public UnreadMessageInfo(long count, Date newestTimestamp, Date oldestTimestamp) {
		super();
		this.count = count;
		this.newestTimestamp = newestTimestamp;
		this.oldestTimestamp = oldestTimestamp;
	}
	public long getCount() {
		return count;
	}
	public Date getNewestTimestamp() {
		return newestTimestamp;
	}
	public Date getOldestTimestamp() {
		return oldestTimestamp;
	}
	
	public UnreadMessageInfo aggregateWith(UnreadMessageInfo other) {
		return new UnreadMessageInfo(
				this.count + other.count, 
				this.newestTimestamp.after(other.newestTimestamp) ? this.newestTimestamp :  other.newestTimestamp, 
				this.oldestTimestamp.after(other.oldestTimestamp) ? other.oldestTimestamp : this.oldestTimestamp);
	}
	
	public UnreadMessageInfo clone() {
		return new UnreadMessageInfo(count, newestTimestamp, oldestTimestamp);
	}
	
}
