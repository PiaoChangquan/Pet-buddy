

public class SensorData {

	private String id;
	private String timestamp;
	private String value;

	public SensorData(String id, String Timestamp, String f) {
		this.id = id;
		this.timestamp = Timestamp;
		this.value = f;
	}

	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}



	public String getTimestamp() {
		return timestamp;
	}



	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}



	public String getValue() {
		return value;
	}



	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "SensorData [id=" + id + ", timestamp=" + timestamp + ", value=" + value + "]";
	}



}
