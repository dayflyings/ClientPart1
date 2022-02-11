import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    public static void main(String[] args) throws Exception{
        if (args.length != 5) {
            System.out.println("Invalid input.");
        }
        int numThreads = Integer.parseInt(args[0]);
        int numSkiers = Integer.parseInt(args[1]);
        int numLifts = Integer.parseInt(args[2]);
        int numRuns = Integer.parseInt(args[3]);
        String ip = args[4];
        String address = "http://" + ip + "/CS6650Assignment1_war";
        LinkedBlockingQueue<Record> queue = new LinkedBlockingQueue<>();
        // total number of threads that will be created in the client
        int total = numThreads / 4 + numThreads + numThreads / 10;
        CountDownLatch totalCountDown = new CountDownLatch(total);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
            startUp(numThreads, numSkiers, numLifts, numRuns, client, totalCountDown, queue, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startUp(int numThreads, int numSkiers,
                               int numLifts, int numRuns,
                               HttpClient client, CountDownLatch totalCountDown,
                               LinkedBlockingQueue<Record> queue, String address)
            throws InterruptedException, FileNotFoundException, ParseException {
        int numPhaseThreads = numThreads / 4;
        int numRequests = (numRuns / 5) * (numSkiers / numPhaseThreads);
        int numID = numSkiers / numPhaseThreads;
        int startTime = 1, endTime = 90;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        int threshold = numThreads % 5 == 0 ? numThreads / 5 : numThreads / 5 + 1;
        CountDownLatch startUpCountDown = new CountDownLatch(threshold);
        startThread(numThreads, numID, numLifts, numRequests,
                startTime, endTime, client,
                successCount, failureCount, startUpCountDown,
                totalCountDown, queue, address);
        System.out.println("Start up phase working...");
        System.out.println("==================================================");
        startUpCountDown.await();
        System.out.println("Peak phase start");
        System.out.println("Success request: " + successCount);
        System.out.println("Failure request: " + failureCount);
        System.out.println("==================================================");
        System.out.println("==================================================");
        peakPhase(numThreads, numSkiers, numLifts, numRuns, client,
                successCount, failureCount, totalCountDown, queue, address);
    }

    public static void peakPhase(int numThreads, int numSkiers,
                                 int numLifts, int numRuns,
                                 HttpClient client, AtomicInteger successCount,
                                 AtomicInteger failureCount, CountDownLatch totalCountDown,
                                 LinkedBlockingQueue<Record> queue, String address)
            throws InterruptedException, FileNotFoundException, ParseException {
        int numRequests = (numRuns * 3 / 5) * (numSkiers / numThreads);
        int numID = numSkiers / numThreads;
        int startTime = 91, endTime = 360;
        int threshold = numThreads % 5 == 0 ? numThreads / 5 : numThreads / 5 + 1;
        CountDownLatch peakCountDown = new CountDownLatch(threshold);
        startThread(numThreads, numID, numLifts, numRequests,
                startTime, endTime, client,
                successCount, failureCount, peakCountDown,
                totalCountDown, queue, address);
        peakCountDown.await();
        System.out.println("Cool down phase start");
        System.out.println("==================================================");
        System.out.println("==================================================");
        coolDown(numThreads, numSkiers, numLifts, numRuns, client,
                successCount, failureCount, totalCountDown, queue, address);
    }

    public static void coolDown(int numThreads, int numSkiers,
                                int numLifts, int numRuns,
                                HttpClient client, AtomicInteger successCount,
                                AtomicInteger failureCount, CountDownLatch totalCountDown,
                                LinkedBlockingQueue<Record> queue, String address)
            throws InterruptedException, FileNotFoundException, ParseException {
        int numPhaseThreads = numThreads / 10;
        int numRequests = (numRuns / 10) * (numSkiers / numPhaseThreads);
        int numID = numSkiers / numPhaseThreads;
        int startTime = 361, endTime = 420;
        startThread(numPhaseThreads, numID, numLifts, numRequests,
                startTime, endTime, client,
                successCount, failureCount, null, totalCountDown, queue, address);
        totalCountDown.await();
        System.out.println("All phases finished.");
        System.out.println("Success requests: " + successCount.get());
        System.out.println("Failure requests: " + failureCount.get());
        writeCSV(queue);
        processData();
    }

    public static void startThread(int numPhaseThreads, int numID,
                                   int numLifts, int numRequests, int startTime, int endTime,
                                   HttpClient client, AtomicInteger successCount,
                                   AtomicInteger failureCount, CountDownLatch currentCountDown,
                                   CountDownLatch totalCountDown, LinkedBlockingQueue<Record> queue,
                                   String address) {
        Thread[] threads = new Thread[numPhaseThreads];
        for (int i = 0; i < numPhaseThreads; i++) {
            int startID = i * numID + 1;
            int endID = (i + 1) * numID;
            threads[i] = new Thread(
                    new Worker(
                            numRequests, startID, endID, startTime, endTime,
                            numLifts, client, address,
                            successCount, failureCount, totalCountDown, currentCountDown, queue)
            );
            threads[i].start();
        }
    }

    public static void writeCSV(LinkedBlockingQueue<Record> queue) {
//        System.out.println("Write data to csv file...");
        try (PrintWriter writer = new PrintWriter("report.csv")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Start time,Request type,Latency,Response code\n");
            var formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Record record : queue) {
                sb.append(formatter.format(record.getStartTime())).append(",");
                sb.append("Post,");
                sb.append(record.getEndTime() - record.getStartTime()).append(",");
                sb.append(record.getStatusCode()).append("\n");
            }
            writer.write(sb.toString());
//            System.out.println("Finish write data.");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void processData() throws FileNotFoundException, ParseException {
        List<String> list = new ArrayList<>();
        Scanner file = new Scanner(new File("report.csv"));
        String line = null;
        while (file.hasNext()) {
            line = file.nextLine();
            list.add(line);
        }
        file.close();
        List<ReportElement> resultList = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            String[] parts = list.get(i).split(",");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startTime = sdf.parse(parts[0]);
            resultList.add(new ReportElement(startTime, parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        }
        resultList.sort(Comparator.comparing(ReportElement::getStartTime));
        ReportElement first = resultList.get(0);
        ReportElement last = resultList.get(resultList.size() - 1);
        double ThroughPut =
                (double) resultList.size() /
                        (last.getStartTime().getTime() - first.getStartTime().getTime() + last.getLatency());
        System.out.println("ThroughPut: " + ThroughPut + "\n");
        long firstTime = resultList.get(0).getStartTime().getTime();
        ReportElement lastElement = resultList.get(resultList.size() - 1);
        long lastTime = lastElement.getStartTime().getTime() + lastElement.getLatency();
        System.out.println("Total wall time: " + (lastTime - firstTime) + "ms\n");
        System.out.println("Finish all tasks.");
    }
}

