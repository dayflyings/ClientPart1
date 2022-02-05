import java.util.Date;

public class ReportElement {
    private final Date startTime;
    private final String requestType;
    private final int latency;
    private final int requestCode;

    public ReportElement(Date startTime, String requestType, int latency, int requestCode) {
        this.startTime = startTime;
        this.requestType = requestType;
        this.latency = latency;
        this.requestCode = requestCode;
    }

    public Date getStartTime() {
        return startTime;
    }

    public String getRequestType() {
        return requestType;
    }

    public int getLatency() {
        return latency;
    }

    public int getRequestCode() {
        return requestCode;
    }

    @Override
    public String toString() {
        return "ReportElement{" +
                "startTime=" + startTime +
                ", requestType='" + requestType + '\'' +
                ", latency=" + latency +
                ", requestCode=" + requestCode +
                "}\n";
    }
}
