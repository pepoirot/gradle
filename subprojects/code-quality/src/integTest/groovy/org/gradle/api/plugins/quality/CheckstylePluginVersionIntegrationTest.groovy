/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.api.plugins.quality
import org.gradle.api.JavaVersion
import org.gradle.integtests.fixtures.MultiVersionIntegrationSpec
import org.gradle.integtests.fixtures.TargetCoverage
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.quality.integtest.fixtures.CheckstyleCoverage
import org.hamcrest.Matcher
import spock.lang.IgnoreIf

import static org.gradle.util.Matchers.containsLine
import static org.hamcrest.Matchers.*

@TargetCoverage({ JavaVersion.current().isJava6() ? CheckstyleCoverage.JDK6_SUPPORTED : CheckstyleCoverage.ALL})
class CheckstylePluginVersionIntegrationTest extends MultiVersionIntegrationSpec {

    def setup() {
        writeBuildFile()
        writeConfigFile()
    }

    def "analyze good code"() {
        goodCode()

        expect:
        succeeds('check')
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.Class1"))
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.Class2"))
        file("build/reports/checkstyle/test.xml").assertContents(containsClass("org.gradle.TestClass1"))
        file("build/reports/checkstyle/test.xml").assertContents(containsClass("org.gradle.TestClass2"))

        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.Class1"))
        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.Class2"))
        file("build/reports/checkstyle/test.html").assertContents(containsClass("org.gradle.TestClass1"))
        file("build/reports/checkstyle/test.html").assertContents(containsClass("org.gradle.TestClass2"))
    }

    def "analyze bad code"() {
        defaultLanguage('en')
        badCode()

        expect:
        fails("check")
        failure.assertHasDescription("Execution failed for task ':checkstyleMain'.")
        failure.assertThatCause(startsWith("Checkstyle rule violations were found. See the report at:"))
        failure.error.contains("Name 'class1' must match pattern")
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.class1"))
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.class2"))

        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.class1"))
        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.class2"))
    }

    def "can suppress console output"() {
        given:
        defaultLanguage('en')
        badCode()

        when:
        buildFile << "checkstyle { showViolations = false }"

        then:
        fails("check")
        failure.assertHasDescription("Execution failed for task ':checkstyleMain'.")
        failure.assertThatCause(startsWith("Checkstyle rule violations were found. See the report at:"))
        !failure.error.contains("Name 'class1' must match pattern")
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.class1"))
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.class2"))

        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.class1"))
        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.class2"))
    }

    def "can ignore failures"() {
        badCode()
        buildFile << """
            checkstyle {
                ignoreFailures = true
            }
        """

        expect:
        succeeds("check")
        output.contains("Checkstyle rule violations were found. See the report at:")
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.class1"))
        file("build/reports/checkstyle/main.xml").assertContents(containsClass("org.gradle.class2"))

        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.class1"))
        file("build/reports/checkstyle/main.html").assertContents(containsClass("org.gradle.class2"))
    }

    @IgnoreIf({GradleContextualExecuter.parallel})
    def "is incremental"() {
        given:
        goodCode()

        expect:
        succeeds("checkstyleMain") && ":checkstyleMain" in nonSkippedTasks
        executer.withArgument("-i")
        succeeds("checkstyleMain") && ":checkstyleMain" in skippedTasks

        when:
        file("build/reports/checkstyle/main.xml").delete()
        file("build/reports/checkstyle/main.html").delete()

        then:
        succeeds("checkstyleMain") && ":checkstyleMain" in nonSkippedTasks
    }

    def "can configure reporting"() {
        given:
        goodCode()

        when:
        buildFile << """
            checkstyleMain.reports {
                xml.destination "foo.xml"
                html.destination "bar.html"
            }
        """

        then:
        succeeds "checkstyleMain"
        file("foo.xml").exists()
        file("bar.html").exists()
    }

    private goodCode() {
        file('src/main/java/org/gradle/Class1.java') << 'package org.gradle; class Class1 { }'
        file('src/test/java/org/gradle/TestClass1.java') << 'package org.gradle; class TestClass1 { }'
        file('src/main/groovy/org/gradle/Class2.java') << 'package org.gradle; class Class2 { }'
        file('src/test/groovy/org/gradle/TestClass2.java') << 'package org.gradle; class TestClass2 { }'
    }

    private badCode() {
        file("src/main/java/org/gradle/class1.java") << "package org.gradle; class class1 { }"
        file("src/test/java/org/gradle/testclass1.java") << "package org.gradle; class testclass1 { }"
        file("src/main/groovy/org/gradle/class2.java") << "package org.gradle; class class2 { }"
        file("src/test/groovy/org/gradle/testclass2.java") << "package org.gradle; class testclass2 { }"
    }

    private Matcher<String> containsClass(String className) {
        containsLine(containsString(className.replace(".", File.separator)))
    }

    private void defaultLanguage(String defaultLanguage) {
        executer.withDefaultLocale(new Locale(defaultLanguage))
    }

    private void writeBuildFile() {
        file("build.gradle") << """
apply plugin: "groovy"
apply plugin: "checkstyle"

repositories {
    mavenCentral()
}

dependencies {
    compile localGroovy()
}

checkstyle {
    toolVersion = '$version'
}
        """
    }

    private void writeConfigFile() {
        file("config/checkstyle/checkstyle.xml") << """
<!DOCTYPE module PUBLIC
        "-//Puppy Crawl//DTD Check Configuration 1.2//EN"
        "http://www.puppycrawl.com/dtds/configuration_1_2.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="TypeName"/>
    </module>
</module>
        """
    }
}
