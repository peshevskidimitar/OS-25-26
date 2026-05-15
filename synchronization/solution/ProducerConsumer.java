package synchronization.solution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumer {
    static final int NUM_RUNS = 100;
    static final int NUM_CONSUMERS = 50;

    static Lock lock;
    static Semaphore canProduce;
    static Semaphore[] canConsume;

    public static void init() {
        lock = new ReentrantLock();
        canProduce = new Semaphore(1);
        canConsume = new Semaphore[NUM_CONSUMERS];
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            canConsume[i] = new Semaphore(0);
        }
    }

    public static void main(String[] args) {
        init();

        Buffer sharedBuffer = new Buffer(NUM_CONSUMERS);
        Producer p = new Producer(sharedBuffer);
        p.start();

        List<Consumer> consumers = new ArrayList<>();
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumers.add(new Consumer(i, sharedBuffer));
        }

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumers.get(i).start();
        }
    }

    static class Buffer {
        private int numItems = 0;

        private final int numConsumers;

        public Buffer(int numConsumers) {
            this.numConsumers = numConsumers;
        }

        public int getBufferCapacity() {
            return numConsumers;
        }

        public void fillBuffer() {
            if (numItems != 0) {
                throw new RuntimeException("The buffer is not empty!");
            }
            numItems = numConsumers;
            System.out.println("The buffer is full.");
        }

        public void decrementNumberOfItemsLeft() {
            if (numItems <= 0) {
                throw new RuntimeException("Can't get item, no items left in the buffer!");
            }
            numItems--;
        }

        public boolean isBufferEmpty() {
            return numItems == 0;
        }

        public void getItem(int consumerId) {
            System.out.println(String.format("Get item for consumer with id: %d.", consumerId));
        }
    }

    static class Consumer extends Thread {
        private Buffer buffer;
        private int consumerId;

        public Consumer(int consumerId, Buffer buffer) {
            this.buffer = buffer;
            this.consumerId = consumerId;
        }

        public void execute() throws InterruptedException {
            canConsume[consumerId].acquire();
            buffer.getItem(consumerId);

            lock.lock();
            buffer.decrementNumberOfItemsLeft();
            if (buffer.isBufferEmpty()) {
                canProduce.release();
            }
            lock.unlock();
        }

        @Override
        public void run() {
            for (int i = 0; i < ProducerConsumer.NUM_RUNS; i++) {
                try {
                    execute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class Producer extends Thread {
        private Buffer buffer;

        public Producer(Buffer buffer) {
            this.buffer = buffer;
        }

        public void execute() throws InterruptedException {
            canProduce.acquire();

            lock.lock();
            buffer.fillBuffer();
            lock.unlock();

            for (int i = 0; i < NUM_CONSUMERS; i++) {
                canConsume[i].release();
            }
        }

        @Override
        public void run() {
            for (int i = 0; i < ProducerConsumer.NUM_RUNS; i++) {
                try {
                    execute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}