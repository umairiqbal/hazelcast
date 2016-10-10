/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.spi.impl.operationexecutor.classic;

import com.hazelcast.instance.HazelcastThreadGroup;
import com.hazelcast.instance.NodeExtension;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.impl.operationexecutor.OperationRunner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An {@link OperationThread} that executes Operations for a particular partition, e.g. a map.get operation.
 */
public final class PartitionOperationThread extends OperationThread {

    private final OperationRunner[] partitionOperationRunners;

    @SuppressFBWarnings({"EI_EXPOSE_REP" })
    public PartitionOperationThread(String name, int threadId,
                                    ScheduleQueue scheduleQueue, ILogger logger,
                                    HazelcastThreadGroup threadGroup, NodeExtension nodeExtension,
                                    OperationRunner[] partitionOperationRunners) {
        super(name, threadId, scheduleQueue, logger, threadGroup, nodeExtension);
        this.partitionOperationRunners = partitionOperationRunners;
    }

    /**
     * For each partition there is a {@link com.hazelcast.spi.impl.operationexecutor.OperationRunner} instance. So we need to
     * find the right one based on the partition-id.
     */
    @Override
    public OperationRunner getOperationRunner(int partitionId) {
        return partitionOperationRunners[partitionId];
    }
}