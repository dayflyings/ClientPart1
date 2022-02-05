public class Record {
    private final long startTime;
    private final long endTime;
    private final int statusCode;

    public Record(long startTime, long endTime, int statusCode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.statusCode = statusCode;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
