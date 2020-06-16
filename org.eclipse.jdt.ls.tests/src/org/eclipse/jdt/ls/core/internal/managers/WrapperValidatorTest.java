/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.ls.internal.gradle.checksums.ValidationResult;
import org.eclipse.jdt.ls.internal.gradle.checksums.WrapperValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author snjeza
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WrapperValidatorTest extends AbstractGradleBasedTest{

	@Before
	public void setProperty() throws Exception {
		System.setProperty("gradle.checksum.cacheDir", "target/gradle/checksums");
		WrapperValidator.clear();
	}

	@After
	public void clearProperty() throws IOException {
		System.clearProperty("gradle.checksum.cacheDir");
	}

	@Test
	public void testGradleWrapper() throws Exception {
		File file = new File(getSourceProjectDirectory(), "gradle/simple-gradle");
		assertTrue(file.isDirectory());
		File sha256Directory = WrapperValidator.getSha256CacheFile();
		assertTrue(sha256Directory.isDirectory());
		ValidationResult result = new WrapperValidator(100).checkWrapper(file.getAbsolutePath());
		assertTrue(result.isValid());
		// test cache
		assertTrue(sha256Directory.isDirectory());
		String message = Files.list(Paths.get(sha256Directory.getAbsolutePath())).collect(Collectors.toList()).toString();
		file = new File(sha256Directory, "gradle-6.3-wrapper.jar.sha256");
		assertTrue(message, file.isFile());
		String sha256 = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8).findFirst().get();
		assertEquals("1cef53de8dc192036e7b0cc47584449b0cf570a00d560bfaa6c9eabe06e1fc06", sha256);
	}

	@Test
	public void testMissingSha256() throws Exception {
		WrapperValidator wrapperValidator = new WrapperValidator(100);
		Set<String> allowed = WrapperValidator.getAllowed();
		Set<String> disallowed = WrapperValidator.getDisallowed();
		File file = new File(getSourceProjectDirectory(), "gradle/gradle-4.0");
		wrapperValidator.checkWrapper(file.getAbsolutePath());
		int size = WrapperValidator.size();
		List<String> sha256 = new ArrayList<>();
		try {
			sha256.add("41c8aa7a337a44af18d8cda0d632ebba469aef34f3041827624ef5c1a4e4419d");
			WrapperValidator.clear();
			WrapperValidator.disallow(sha256);
			assertTrue(file.isDirectory());
			ValidationResult result = wrapperValidator.checkWrapper(file.getAbsolutePath());
			assertFalse(result.isValid());
			assertNotNull(result.getChecksum());
			WrapperValidator.clear();
			WrapperValidator.allow(sha256);
			result = wrapperValidator.checkWrapper(file.getAbsolutePath());
			assertTrue(result.isValid());
		} finally {
			WrapperValidator.clear();
			WrapperValidator.allow(allowed);
			WrapperValidator.disallow(disallowed);
			wrapperValidator.checkWrapper(file.getAbsolutePath());
			assertEquals(size, WrapperValidator.size());
		}
	}
}