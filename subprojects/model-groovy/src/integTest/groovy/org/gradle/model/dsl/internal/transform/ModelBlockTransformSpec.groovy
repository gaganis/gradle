/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.model.dsl.internal.transform

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

import static org.hamcrest.Matchers.containsString

class ModelBlockTransformSpec extends AbstractIntegrationSpec {

    def setup() {
        executer.requireOwnGradleUserHomeDir()
    }

    def "only literal strings can be given to dollar - #code"() {
        when:
        buildScript """
        model {
          foo {
            $code
          }
        }
        """

        then:
        fails "tasks"
        failure.assertHasLineNumber 4
        failure.assertHasFileName("Build file '${buildFile}'")
        failure.assertThatCause(containsString(RuleClosureVisitor.INVALID_ARGUMENT_LIST))

        where:
        code << [
            '$(1)',
            '$("$name")',
            '$("a" + "b")',
            'def a = "foo"; $(a)',
            '$("foo", "bar")',
        ]
    }

    def "dollar method is only detected with no explicit receiver"() {
        when:
        buildScript """
            import org.gradle.model.*

            class MyPlugin implements Plugin<Project> {
              void apply(Project project) {}
              @RuleSource
              static class Rules {
                @Model
                String foo() {
                  "foo"
                }
              }
            }

            apply plugin: MyPlugin

            model {
              foo {
                $code
              }
            }
        """

        then:
        succeeds "tasks" // succeeds because we don't fail on invalid usage, and don't fail due to unbound inputs

        where:
        code << [
            'something.$(1)',
            'this.$("$name")',
            'foo.bar().$("a" + "b")',
        ]
    }

    def "input references are found in nested code - #code"() {
        when:
        buildScript """
            import org.gradle.model.*
            import org.gradle.model.collection.*

            class MyPlugin implements Plugin<Project> {
                void apply(Project project) {}
                @RuleSource
                static class Rules {
                    @Mutate void addPrintTask(CollectionBuilder<Task> tasks, List<String> strings) {
                        tasks.create("printMessage", PrintTask) {
                            it.message = strings
                        }
                    }

                    @Model String foo() {
                        "foo"
                    }

                    @Model List<String> strings() {
                        []
                    }
                }
            }

            class PrintTask extends DefaultTask {
                String message

                @TaskAction
                void doPrint() {
                    println "message: " + message
                }
            }

            apply plugin: MyPlugin

            model {
                strings {
                    $code
                }
            }
        """

        then:
        succeeds "printMessage"
        output.contains("message: [foo]")

        where:
        code << [
            'if (true) { add $("foo") }',
            'if (false) {} else if (true) { add $("foo") }',
            'if (false) {} else { add $("foo") }',
            'def i = true; while(i) { add $("foo"); i = false }',
            '[1].each { add $("foo") }',
            'add "${$("foo")}"',
            'def v = $("foo"); add(v)',
            'add($("foo"))',
            'add($("foo").toString())',
        ]
    }

    def "model blocks are detected"() {
        when:
        buildScript """
            import org.gradle.model.*

            class MyPlugin implements Plugin<Project> {

              void apply(Project project) {}

              @RuleSource
              static class Rules {
                @Model
                String foo() {
                  "foo"
                }

                @Model
                List<String> strings() {
                  []
                }
              }
            }

            apply plugin: MyPlugin

            model {
              tasks {
                create("printStrings").doLast {
                  println "strings: " + \$("strings")
                }
              }
              strings {
                add \$("foo")
              }
            }
        """

        then:
        succeeds "printStrings"
        output.contains "strings: " + ["foo"]
    }

}
