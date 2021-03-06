/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.engine.Constants.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.extension.sub.SystemPropertyCondition;
import org.junit.jupiter.engine.extension.sub.SystemPropertyCondition.SystemProperty;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.ExecutionResults;

/**
 * Integration tests that verify support for the {@link ExecutionCondition}
 * extension point in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class ExecutionConditionTests extends AbstractJupiterTestEngineTests {

	private static final String FOO = "DisabledTests.foo";
	private static final String BAR = "DisabledTests.bar";
	private static final String BOGUS = "DisabledTests.bogus";

	@BeforeEach
	public void setUp() {
		System.setProperty(FOO, BAR);
	}

	@AfterEach
	public void tearDown() {
		System.clearProperty(FOO);
	}

	@Test
	void conditionWorksOnContainer() {
		ExecutionResults executionResults = executeTestsForClass(TestCaseWithExecutionConditionOnClass.class);

		assertEquals(1, executionResults.getContainersSkippedCount(), "# container skipped");
		assertEquals(0, executionResults.getTestsStartedCount(), "# tests started");
	}

	@Test
	void conditionWorksOnTest() {
		LauncherDiscoveryRequest request = request().selectors(
			selectClass(TestCaseWithExecutionConditionOnMethods.class)).build();
		ExecutionResults executionResults = executeTests(request);

		assertEquals(2, executionResults.getTestsStartedCount(), "# tests started");
		assertEquals(2, executionResults.getTestsSuccessfulCount(), "# tests succeeded");
		assertEquals(3, executionResults.getTestsSkippedCount(), "# tests skipped");
	}

	@Test
	void overrideConditionsUsingFullyQualifiedClassName() {
		String deactivatePattern = SystemPropertyCondition.class.getName();
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void overrideConditionsUsingStar() {
		// "*" should deactivate DisabledCondition and SystemPropertyCondition
		String deactivatePattern = "*";
		assertExecutionConditionOverride(deactivatePattern, 2, 2);
		assertExecutionConditionOverride(deactivatePattern, 5, 2, 3);
	}

	@Test
	void overrideConditionsUsingStarPlusSimpleClassName() {
		// DisabledCondition should remain activated
		String deactivatePattern = "*" + SystemPropertyCondition.class.getSimpleName();
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void overrideConditionsUsingPackageNamePlusDotStar() {
		// DisabledCondition should remain activated
		String deactivatePattern = SystemPropertyCondition.class.getPackage().getName() + ".*";
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	@Test
	void overrideConditionsUsingMultipleWildcards() {
		// DisabledCondition should remain activated
		String deactivatePattern = "org.junit.jupiter.*.System*Condition";
		assertExecutionConditionOverride(deactivatePattern, 1, 1);
		assertExecutionConditionOverride(deactivatePattern, 4, 2, 2);
	}

	private void assertExecutionConditionOverride(String deactivatePattern, int testStartedCount, int testFailedCount) {
		// @formatter:off
		LauncherDiscoveryRequest request = request()
				.selectors(selectClass(TestCaseWithExecutionConditionOnClass.class))
				.configurationParameter(DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME, deactivatePattern)
				.build();
		// @formatter:on

		ExecutionResults executionResults = executeTests(request);

		assertEquals(0, executionResults.getContainersSkippedCount(), "# containers skipped");
		assertEquals(2, executionResults.getContainersStartedCount(), "# containers started");
		assertEquals(testStartedCount, executionResults.getTestsStartedCount(), "# tests started");
		assertEquals(testFailedCount, executionResults.getTestsFailedCount(), "# tests failed");
	}

	private void assertExecutionConditionOverride(String deactivatePattern, int started, int succeeded, int failed) {
		// @formatter:off
		LauncherDiscoveryRequest request = request()
				.selectors(selectClass(TestCaseWithExecutionConditionOnMethods.class))
				.configurationParameter(DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME, deactivatePattern)
				.build();
		// @formatter:on

		ExecutionResults executionResults = executeTests(request);

		assertEquals(started, executionResults.getTestsStartedCount(), "# tests started");
		assertEquals(succeeded, executionResults.getTestsSuccessfulCount(), "# tests succeeded");
		assertEquals(failed, executionResults.getTestsFailedCount(), "# tests failed");
	}

	// -------------------------------------------------------------------

	@SystemProperty(key = FOO, value = BOGUS)
	static class TestCaseWithExecutionConditionOnClass {

		@Test
		void disabledTest() {
			fail("this should be disabled");
		}

		@Test
		@Disabled
		void atDisabledTest() {
			fail("this should be @Disabled");
		}
	}

	static class TestCaseWithExecutionConditionOnMethods {

		@Test
		void enabledTest() {
		}

		@Test
		@Disabled
		void atDisabledTest() {
			fail("this should be @Disabled");
		}

		@Test
		@SystemProperty(key = FOO, value = BAR)
		void systemPropertyEnabledTest() {
		}

		@Test
		@SystemProperty(key = FOO, value = BOGUS)
		void systemPropertyWithIncorrectValueTest() {
			fail("this should be disabled");
		}

		@Test
		@SystemProperty(key = BOGUS, value = "doesn't matter")
		void systemPropertyNotSetTest() {
			fail("this should be disabled");
		}

	}

}
