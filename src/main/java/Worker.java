import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

public class Worker implements Runnable{
    private final int numRequest;
    private final int startID;
    private final int endID;
    private final int startTime;
    private final int endTime;
    private final int liftNum;
    private final HttpClient client;
    private final String address;
    private final Gson gson = new Gson();
    public AtomicInteger totalSuccess;
    public AtomicInteger totalFailure;
    public CountDownLatch totalCount;
    public CountDownLatch currentCount;
    public int successCount;
    public int failCount;
    public LinkedBlockingQueue<Record> queue;

    public Worker(
            int numRequest, int startID, int endID, int startTime, int endTime,
            int liftNum, HttpClient client, String address, AtomicInteger totalSuccess,
            AtomicInteger totalFailure, CountDownLatch totalCount, CountDownLatch currentCount,
            LinkedBlockingQueue<Record> queue) {
        this.numRequest = numRequest;
        this.startID = startID;
        this.endID = endID;
        this.startTime = startTime;
        this.endTime = endTime;
        this.liftNum = liftNum;
        this.client = client;
        this.address = address;
        this.totalSuccess = totalSuccess;
        this.totalFailure = totalFailure;
        this.totalCount = totalCount;
        this.currentCount = currentCount;
        this.queue = queue;
    }

    @Override
    public void run() {
        for (int i = 0; i < numRequest; i++) {
            long startTime = System.currentTimeMillis();
            int statusCode = sendRequest();
            long endTime = System.currentTimeMillis();
            try {
                queue.put(new Record(startTime, endTime, statusCode));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        totalSuccess.getAndAdd(successCount);
        totalFailure.getAndAdd(failCount);
        totalCount.countDown();
        if (currentCount != null) {
            currentCount.countDown();
        }
    }

    public int sendRequest() {
        Random rand = new Random();
        int id = rand.nextInt(endID - startID + 1) + startID;
        int liftID = rand.nextInt(liftNum);
        int time = rand.nextInt(endTime - startTime + 1) + startTime;
        int waitTime = 10;
        int wait = rand.nextInt(waitTime);
        int count = 1;
        Map<Object, Object> data = new HashMap<>();
        data.put("time", time);
        data.put("liftID", liftID);
        data.put("waitTime", wait);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(address + "/skiers/1/seasons/2017/days/120/skiers/" + id))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                .build();
        HttpResponse<String> response;
        try {
            while (count <= 5) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 201) {
                    count++;
                } else {
                    successCount++;
                    return 201;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        failCount++;
        return 500;
    }
}
