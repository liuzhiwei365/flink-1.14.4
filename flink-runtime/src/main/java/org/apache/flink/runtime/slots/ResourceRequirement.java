/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.slots;

import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.Objects;

/** Represents the number of required resources for a specific {@link ResourceProfile}. */
public class ResourceRequirement implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ResourceProfile resourceProfile; // 描述cpu 内存 信息

    private final int numberOfRequiredSlots; // 槽位数

    private ResourceRequirement(ResourceProfile resourceProfile, int numberOfRequiredSlots) {
        Preconditions.checkNotNull(resourceProfile);
        Preconditions.checkArgument(numberOfRequiredSlots > 0);

        this.resourceProfile = resourceProfile;
        this.numberOfRequiredSlots = numberOfRequiredSlots;
    }

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public int getNumberOfRequiredSlots() {
        return numberOfRequiredSlots;
    }

    public static ResourceRequirement create(
            ResourceProfile resourceProfile, int numberOfRequiredSlots) {
        return new ResourceRequirement(resourceProfile, numberOfRequiredSlots);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceRequirement that = (ResourceRequirement) o;
        return numberOfRequiredSlots == that.numberOfRequiredSlots
                && Objects.equals(resourceProfile, that.resourceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceProfile, numberOfRequiredSlots);
    }

    @Override
    public String toString() {
        return "ResourceRequirement{"
                + "resourceProfile="
                + resourceProfile
                + ", numberOfRequiredSlots="
                + numberOfRequiredSlots
                + '}';
    }
}
