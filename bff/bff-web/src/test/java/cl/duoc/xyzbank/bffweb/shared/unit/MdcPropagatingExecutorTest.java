package cl.duoc.xyzbank.bffweb.shared.unit;

import cl.duoc.xyzbank.bffweb.shared.infrastructure.concurrency.MdcPropagatingExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The MDC-propagating executor")
class MdcPropagatingExecutorTest {

    /*
     * Cases:
     * 1. Restores the submitting thread's MDC context inside every task it dispatches
     * 2. Clears MDC on the worker thread after a task completes, so pooled threads don't
     *    leak context into unrelated later tasks
     * 3. Leaves the worker thread's MDC empty when the submitting thread had none
     */

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("restores the submitting thread's MDC context inside every dispatched task")
    void restoresSubmittingThreadsMdcContextInsideEveryDispatchedTask() throws InterruptedException {
        MdcPropagatingExecutor executor =
                new MdcPropagatingExecutor(Executors.newVirtualThreadPerTaskExecutor());
        int taskCount = 3;
        List<String> observed = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(taskCount);

        MDC.put("correlationId", "corr-test-1");
        try {
            IntStream.range(0, taskCount).forEach(i -> executor.execute(() -> {
                observed.add(MDC.get("correlationId"));
                done.countDown();
            }));
            assertTrue(done.await(2, TimeUnit.SECONDS), "tasks did not complete in time");
        } finally {
            MDC.clear();
        }

        assertEquals(taskCount, observed.size());
        observed.forEach(value -> assertEquals("corr-test-1", value));
    }

    @Test
    @DisplayName("clears MDC on the worker thread after the task completes")
    void clearsMdcOnTheWorkerThreadAfterTheTaskCompletes() {
        MdcPropagatingExecutor executor = new MdcPropagatingExecutor(Runnable::run);

        MDC.put("correlationId", "corr-test-2");
        try {
            executor.execute(() -> {
            });
            assertNull(MDC.get("correlationId"));
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("leaves the worker thread's MDC empty when the submitting thread had none")
    void leavesTheWorkerThreadsMdcEmptyWhenTheSubmittingThreadHadNone() throws InterruptedException {
        MdcPropagatingExecutor executor =
                new MdcPropagatingExecutor(Executors.newVirtualThreadPerTaskExecutor());
        String[] observed = new String[1];
        CountDownLatch done = new CountDownLatch(1);

        executor.execute(() -> {
            observed[0] = MDC.get("correlationId");
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS), "task did not complete in time");
        assertNull(observed[0]);
    }
}
