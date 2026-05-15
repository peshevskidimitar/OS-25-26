package synchronization.starter;

import java.util.Random;

class LinearCounterThread extends Thread {
    private int[] arr;
    private int target;
    private int start;
    private int end;

    public LinearCounterThread(int[] arr, int target, int start, int end) {
        this.arr = arr;
        this.target = target;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        int localCount = 0;
        for (int i = start; i < end; i++) {
            if (arr[i] == target) {
                localCount++;
            }
        }
        ParallelLinearCounter.count += localCount;
    }
}

public class ParallelLinearCounter {
    static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        int n = 1_000_000;
        int m = 1000;
        int target = 73;

        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i % 1000;
        }

        Random random = new Random();
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }

        LinearCounterThread[] threads = new LinearCounterThread[m];

        int chunkSize = n / m;

        for (int t = 0; t < m; t++) {
            int start = t * chunkSize;
            int end = t == m - 1 ? n : start + chunkSize;

            threads[t] = new LinearCounterThread(arr, target, start, end);
            threads[t].start();
        }

        for (int t = 0; t < m; t++) {
            threads[t].join();
        }

        if (count != 0) {
            System.out.println("Found: " + count);
        } else {
            System.out.println("Not found.");
        }
    }
}
