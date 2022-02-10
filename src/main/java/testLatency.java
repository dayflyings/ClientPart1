import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class testLatency {
    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Gson gson = new Gson();
                Random rand = new Random();
                String address = "http://3.90.202.39:8080/CS6650Assignment1_war";
                Map<Object, Object> data = new HashMap<>();
                int liftID = rand.nextInt(40);
                int time = rand.nextInt(420);
                int waitTime = 10;
                int wait = rand.nextInt(waitTime);
                data.put("time", time);
                data.put("liftID", liftID);
                data.put("waitTime", wait);
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(address + "/skiers/1/seasons/2017/days/120/skiers/1"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                            .build();
                    try {
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                long endTime = System.currentTimeMillis();
                System.out.println("Total wall time: " + (endTime - startTime) + "ms");
                System.out.println("Latency: " + ((endTime - startTime) / 10000));
            }
        }).start();
    }
}
