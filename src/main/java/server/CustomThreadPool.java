package server;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomThreadPool {
    private static final Logger LOGGER = Logger.getLogger(CustomThreadPool.class.getName());

    // Core configuration
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final BlockingQueue<Runnable> workQueue; // Work queue

    // Thread management
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final ThreadFactory threadFactory;

    // Lifecycle management
    private volatile boolean isShutdown = false;

    // Performance monitoring
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger rejectedTasks = new AtomicInteger(0);
    private final AtomicInteger queuedTasks = new AtomicInteger(0);

    // Thread performance thresholds
    private static final double HIGH_LOAD_THRESHOLD = 0.8; // 80% of threads active
    private static final double LOW_LOAD_THRESHOLD = 0.2;  // 20% of threads active
    private static final int QUEUE_SIZE_THRESHOLD = 2;     // Tasks in queue per thread

    /**
     * Creates a new CustomThreadPool with the specified parameters.
     *
     * @param corePoolSize the minimum number of threads to keep alive
     * @param maxPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime the time excess idle non-core threads will wait before terminating
     * @param timeUnit the time unit for the keepAliveTime
     * @param workQueueCapacity the capacity of the work queue
     */
    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime,
                            TimeUnit timeUnit, int workQueueCapacity) {

        // Validate input parameters
        if (corePoolSize < 1 || maxPoolSize <= corePoolSize || keepAliveTime < 0) {
            throw new IllegalArgumentException("Invalid thread pool parameters");
        }

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.workQueue = new LinkedBlockingQueue<>(workQueueCapacity);

        // Create custom thread factory that names threads for better debugging
        this.threadFactory = new ThreadFactory() {
            private final AtomicInteger threadCounter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DictionaryWorker-" + threadCounter.getAndIncrement());
                thread.setDaemon(false); // Make worker threads non-daemon
                return thread;
            }
        };

        // Initialize the core pool of threads
        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }

        LOGGER.info("Thread pool initialized with " + corePoolSize + " core threads, " +
                "max " + maxPoolSize + " threads, and queue capacity " + workQueueCapacity);
    }

    /**
     * Executes the given task sometime in the future.
     *
     * @param task the task to execute
     * @return true if the task was successfully queued, false otherwise
     */
    public boolean execute(Runnable task) {
        if (isShutdown) {
            rejectedTasks.incrementAndGet();
            LOGGER.warning("Task rejected - pool is shutdown");
            return false;
        }

        // Try to add to the work queue
        if (workQueue.offer(task)) {
            queuedTasks.incrementAndGet();

            // Check if we need more threads based on load
            checkAndAdjustThreadCount();
            return true;
        }

        // Queue is full, try to add a new thread if possible
        if (poolSize.get() < maxPoolSize) {
            if (addWorker()) {
                // Successfully added a new worker, try to queue again
                if (workQueue.offer(task)) {
                    queuedTasks.incrementAndGet();
                    return true;
                }
            }
        }

        // Queue is full and at max threads, reject the task
        rejectedTasks.incrementAndGet();
        LOGGER.warning("Task rejected - queue full and at max threads");
        return false;
    }

    /**
     * Check the current load and adjust thread count if necessary.
     */
    private void checkAndAdjustThreadCount() {
        int currentPoolSize = poolSize.get();
        int currentActive = activeThreads.get();
        int currentQueued = queuedTasks.get();

        // Only adjust if we're not at core or max limits already
        if (currentPoolSize > corePoolSize && currentPoolSize < maxPoolSize) {
            // Calculate load metrics
            double activeRatio = (double) currentActive / currentPoolSize;
            double queuedPerThread = (double) currentQueued / currentPoolSize;

            // Add threads if we're under high load
            if (activeRatio > HIGH_LOAD_THRESHOLD || queuedPerThread > QUEUE_SIZE_THRESHOLD) {
                // Calculate how many threads to add based on load
                int threadsToAdd = Math.min(
                        maxPoolSize - currentPoolSize,
                        Math.max(1, currentQueued / 2) // Add at least 1, or more based on queue size
                );

                for (int i = 0; i < threadsToAdd; i++) {
                    if (!addWorker()) {
                        break; // Stop if we can't add more
                    }
                }

                LOGGER.info("Added " + threadsToAdd + " threads due to high load. New pool size: " + poolSize.get());
            }
            // Remove excess threads if we're under low load
            else if (activeRatio < LOW_LOAD_THRESHOLD && currentPoolSize > corePoolSize) {
                // We'll let the natural thread termination handle this
                // The worker threads check this condition before processing next task
                LOGGER.fine("Low load detected. Current threads will terminate if idle too long.");
            }
        }
    }

    /**
     * Adds a new worker thread to the pool.
     *
     * @return true if successfully added, false otherwise
     */
    private boolean addWorker() {
        if (isShutdown || poolSize.get() >= maxPoolSize) {
            return false;
        }

        // Create and start a new worker thread
        Runnable workerTask = new Worker();
        Thread thread = threadFactory.newThread(workerTask);

        if (thread != null) {
            poolSize.incrementAndGet();
            thread.start();
            LOGGER.fine("Added new worker thread: " + thread.getName());
            return true;
        }

        return false;
    }

    /**
     * The inner class worker processes jobs from the queue.
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            boolean timedWait = false;

            try {
                while (!isShutdown) {
                    Runnable task = null;

                    try {
                        // If this is not a core thread and we're over the core pool size,
                        // use timed poll instead of blocking take
                        if (poolSize.get() > corePoolSize) {
                            task = workQueue.poll(keepAliveTime, timeUnit);
                            timedWait = true;
                        } else {
                            task = workQueue.take(); // Block until a task is available
                            timedWait = false;
                        }
                    } catch (InterruptedException e) {
                        // Thread was interrupted, check shutdown status
                        if (isShutdown) {
                            break;
                        }
                        continue;
                    }

                    // If timeout with on task, this thread can terminate
                    // if above the core pool size
                    if (timedWait && task == null && poolSize.get() > corePoolSize) {
                        // Try to decrement poolSize, but only if we're still above core size
                        if (poolSize.decrementAndGet() < corePoolSize) {
                            // Oops, went below core size, increment it back
                            poolSize.incrementAndGet();
                        } else {
                            LOGGER.fine("Worker thread terminating due to timeout");
                            break; // Exit the loop and terminate this thread
                        }
                    }

                    // If have a task, execute it
                    if (task != null) {
                        queuedTasks.decrementAndGet();
                        activeThreads.incrementAndGet();

                        try {
                            task.run();
                            completedTasks.incrementAndGet();
                        } catch (Throwable t) {
                            LOGGER.log(Level.SEVERE, "Task execution failed", t);
                        } finally {
                            activeThreads.decrementAndGet();
                        }
                    }
                }
            } finally {
                // timeout or shutdown, decrement pool size
                poolSize.decrementAndGet();
                LOGGER.fine("Worker thread terminated");
            }
        }
    }

    /**
     * Initiates an orderly shutdown of the thread pool.
     */
    public void shutdown() {
        isShutdown = true;

        // Interrupt all worker threads to wake them up
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);

        for (Thread thread : threads) {
            if (thread != null && thread.getName().startsWith("DictionaryWorker-")) {
                thread.interrupt();
            }
        }

        LOGGER.info("Thread pool shutdown initiated");
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait after no tasks are in the queue
     * @param unit the time unit of the timeout argument
     * @return true if this executor terminated and false if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long endTime = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() < endTime) {
            if (poolSize.get() == 0 && workQueue.isEmpty()) {
                return true;
            }
            Thread.sleep(100); // Sleep a bit to avoid busy waiting
        }

        return poolSize.get() == 0 && workQueue.isEmpty();
    }

    // Getters for monitoring thread pool state
    public int getPoolSize() {
        return poolSize.get();
    }

    public int getActiveCount() {
        return activeThreads.get();
    }

    public int getQueueSize() {
        return workQueue.size();
    }

    public int getCompletedTaskCount() {
        return completedTasks.get();
    }

    public int getRejectedTaskCount() {
        return rejectedTasks.get();
    }
}