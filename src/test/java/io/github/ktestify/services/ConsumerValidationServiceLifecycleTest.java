/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ktestify.services;

import io.github.ktestify.exceptions.ConsumerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsumerValidationService — executor lifecycle and shutdown")
class ConsumerValidationServiceLifecycleTest {

    @Test
    @DisplayName("repeated timeouts do not leak worker threads after close and use named worker threads")
    void repeatedTimeoutsDoNotLeakThreadsAndUseNamedWorkers() {
        AtomicReference<String> observedThreadName = new AtomicReference<>();
        ConsumerValidationService service = new ConsumerValidationService(
                Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "ktestify-consumer-worker-test");
                    t.setDaemon(true);
                    return t;
                }),
                1000L,
                0L);

        try {
            for (int i = 0; i < 3; i++) {
                assertThrows(
                        ConsumerException.class,
                        () -> service.runWithTimeout(
                                () -> {
                                    observedThreadName.set(
                                            Thread.currentThread().getName());
                                    Thread.sleep(5_000L);
                                    return true;
                                },
                                30L));
            }
        } finally {
            service.close();
        }

        assertEquals("ktestify-consumer-worker-test", observedThreadName.get());
        assertTrue(service.isClosed());
        assertTrue(service.isTerminated(), "Executor should terminate cleanly after cancelled tasks exit");
        assertFalse(service.isForcedShutdownTimedOut());
    }

    @Test
    @DisplayName("cancelled task closes its AutoCloseable consumer resource in finally")
    void cancelledTaskClosesResource() throws Exception {
        CountDownLatch closedLatch = new CountDownLatch(1);
        AtomicBoolean resourceClosed = new AtomicBoolean(false);

        class CloseableBlockingConsumer implements Callable<Boolean>, AutoCloseable {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep(5_000L);
                return true;
            }

            @Override
            public void close() {
                resourceClosed.set(true);
                closedLatch.countDown();
            }
        }

        try (ConsumerValidationService service =
                new ConsumerValidationService(Executors.newSingleThreadExecutor(), 1000L, 0L)) {
            assertThrows(ConsumerException.class, () -> service.runWithTimeout(new CloseableBlockingConsumer(), 30L));
            assertTrue(closedLatch.await(2, TimeUnit.SECONDS), "Resource should be closed when task is cancelled");
            assertTrue(resourceClosed.get());
        }
    }

    @Test
    @DisplayName("close waits for managed tasks to complete and rejects further submissions")
    void closeWaitsForManagedTaskAndRejectsNewWork() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean finished = new AtomicBoolean(false);
        ExecutorService backing = Executors.newSingleThreadExecutor();
        ConsumerValidationService service = new ConsumerValidationService(backing, 2000L, 0L);

        backing.submit(() -> {
            started.countDown();
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finished.set(true);
            }
        });

        assertTrue(started.await(1, TimeUnit.SECONDS));
        service.close();

        assertTrue(finished.get(), "close() should wait for managed in-flight task to finish");
        assertTrue(service.isTerminated());
        assertThrows(ConsumerException.class, () -> service.runWithTimeout(() -> true, 100L));
    }

    @Test
    @DisplayName("reports forced shutdown timeout when a task ignores interruption")
    void reportsTaskIgnoringInterruption() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        ExecutorService backing = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "stubborn-worker");
            t.setDaemon(true);
            return t;
        });

        ConsumerValidationService service = new ConsumerValidationService(backing, 50L, 0L);
        backing.submit(() -> {
            started.countDown();
            // Deliberately ignore interruption until test releases the latch
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (releaseTask.getCount() > 0 && System.nanoTime() < deadline) {
                try {
                    releaseTask.await(20, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    // Intentionally ignored to simulate an uninterruptible client
                }
            }
        });

        assertTrue(started.await(1, TimeUnit.SECONDS));
        try {
            service.close();
            assertTrue(
                    service.isForcedShutdownTimedOut(),
                    "Service must record forced shutdown timeout when worker ignores interruption");
        } finally {
            releaseTask.countDown();
        }
    }
}
