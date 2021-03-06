/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.logging.console;

import org.gradle.internal.logging.events.BatchOutputEventListener;
import org.gradle.internal.logging.events.EndOutputEvent;
import org.gradle.internal.logging.events.OutputEvent;
import org.gradle.internal.logging.events.OutputEventListener;
import org.gradle.internal.time.TimeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Queue output events to be forwarded and schedule flush when time passed or if end of build is signalled.
 */
public class ThrottlingOutputEventListener extends BatchOutputEventListener {
    private final OutputEventListener listener;

    private final ScheduledExecutorService executor;
    private final TimeProvider timeProvider;
    private final int throttleMs;
    private final Object lock = new Object();

    private long lastUpdate;
    private final List<OutputEvent> queue = new ArrayList<OutputEvent>();

    public ThrottlingOutputEventListener(OutputEventListener listener, TimeProvider timeProvider) {
        this(listener, Integer.getInteger("org.gradle.console.throttle", 85), Executors.newSingleThreadScheduledExecutor(), timeProvider);
    }

    ThrottlingOutputEventListener(OutputEventListener listener, int throttleMs, ScheduledExecutorService executor, TimeProvider timeProvider) {
        this.throttleMs = throttleMs;
        this.listener = listener;
        this.executor = executor;
        this.timeProvider = timeProvider;
    }

    public void onOutput(OutputEvent newEvent) {
        synchronized (lock) {
            queue.add(newEvent);

            if (newEvent instanceof EndOutputEvent) {
                // Flush and clean up
                renderNow(timeProvider.getCurrentTime());
                executor.shutdown();
                return;
            }

            if (queue.size() > 1) {
                // Currently queuing events, a thread is scheduled to flush the queue later
                return;
            }

            long now = timeProvider.getCurrentTime();
            if (now - lastUpdate >= throttleMs) {
                // Has been long enough since last update - flush now
                renderNow(now);
                return;
            }

            // This is the first queued event - schedule a thread to flush later
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        renderNow(timeProvider.getCurrentTime());
                    }
                }
            }, throttleMs, TimeUnit.MILLISECONDS);
        }
    }

    private void renderNow(long now) {
        if (queue.isEmpty()) {
            // Already rendered - don't update anything
            return;
        }

        listener.onOutput(new ArrayList<OutputEvent>(queue));
        queue.clear();
        lastUpdate = now;
    }
}
