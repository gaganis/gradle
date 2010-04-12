/*
 * Copyright 2010 the original author or authors.
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



package org.gradle.api.internal.tasks

import spock.lang.Specification
import org.gradle.api.Task
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.Buildable
import org.gradle.api.internal.BuildableInternal

class CachingTaskDependencyResolveContextTest extends Specification {
    private final CachingTaskDependencyResolveContext context = new CachingTaskDependencyResolveContext()
    private final Task task = Mock()
    private final Task target = Mock()
    private final TaskDependencyInternal dependency = Mock()

    def setup() {
        _ * task.getTaskDependencies() >> dependency
    }
    
    def determinesTaskDependenciesByResolvingDependencyObjectForTask() {
        when:
        def tasks = context.getDependencies(task)

        then:
        1 * dependency.resolve(context)
        tasks.isEmpty()
    }

    def resolvesTaskObject() {
        when:
        def tasks = context.getDependencies(task)

        then:
        1 * dependency.resolve(context) >> { context.add(target) }
        tasks == [target] as LinkedHashSet
    }

    def resolvesTaskDependency() {
        TaskDependency otherDependency = Mock()

        when:
        def tasks = context.getDependencies(task)

        then:
        1 * dependency.resolve(context) >> { context.add(otherDependency) }
        1 * otherDependency.getDependencies(task) >> { [target] as Set }
        tasks == [target] as LinkedHashSet
    }

    def resolvesTaskDependencyInternal() {
        TaskDependencyInternal otherDependency = Mock()

        when:
        def tasks = context.getDependencies(task)

        then:
        1 * dependency.resolve(context) >> { context.add(otherDependency) }
        1 * otherDependency.resolve(context) >> { context.add(target) }
        tasks == [target] as LinkedHashSet
    }

    def resolvesBuildable() {
        Buildable buildable = Mock()
        TaskDependency otherDependency = Mock()

        when:
        def tasks = context.getDependencies(task)

        then:
        1 * dependency.resolve(context) >> { context.add(buildable) }
        1 * buildable.getBuildDependencies() >> { otherDependency }
        1 * otherDependency.getDependencies(task) >> { [target] as Set }
        tasks == [target] as LinkedHashSet
    }

    def resolvesBuildableInternal() {
        BuildableInternal buildable = Mock()
        TaskDependencyInternal otherDependency = Mock()

        when:
        def tasks = context.getDependencies(task)

        then:
        1 * dependency.resolve(context) >> { context.add(buildable) }
        1 * buildable.getBuildDependencies() >> { otherDependency }
        1 * otherDependency.resolve(context) >> { context.add(target) }
        tasks == [target] as LinkedHashSet
    }

    def throwsExceptionForUnresolvableObject() {
        when:
        context.getDependencies(task)

        then:
        1 * dependency.resolve(context) >> { context.add('unknown') }
        def e = thrown(IllegalArgumentException)
        e.message == "Cannot resolve object of unknown type String to a Task."
    }

    def cachesResultForTaskDependency() {
        throw new UnsupportedOperationException()
    }

    def cachesResultForBuildable() {
        throw new UnsupportedOperationException()
    }

    def cachesResultForTaskDependencyInternal() {
        throw new UnsupportedOperationException()
    }

    def cachesResultForBuildableInternal() {
        throw new UnsupportedOperationException()
    }

    def failsWhenThereIsACyclicDependency() {
        throw new UnsupportedOperationException()
    }

    def providesSomeWayToIndicateThatResultIsSpecificToTheResolvedTask() {
        throw new UnsupportedOperationException()
    }
}

