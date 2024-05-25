/*
 * Copyright 2024 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cthing.projectversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;


public class ProjectVersionTest {

    @Test
    public void testSnapshotDeveloperVersion() throws Exception {
        withEnvironmentVariable("CTHING_CI", null)
                .and("GIT_BRANCH", null)
                .and("GIT_COMMIT", null)
                .execute(() -> {
                    final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.snapshot);

                    assertThat(ProjectVersion.isDeveloperBuild()).isTrue();
                    assertThat(version.getMajorVersion()).isEqualTo(1);
                    assertThat(version.getMinorVersion()).isEqualTo(2);
                    assertThat(version.getPatchVersion()).isEqualTo(3);
                    assertThat(version.getSemanticVersion()).isEqualTo("1.2.3-0");
                    assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
                    assertThat(version.getBuildNumber()).isEqualTo("0");
                    assertThat(version.getBuildType()).isEqualTo(BuildType.snapshot);
                    assertThat(version.isReleaseBuild()).isFalse();
                    assertThat(version.isSnapshotBuild()).isTrue();
                    assertThat(version.getBuildDate()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
                    assertThat(version.getBuildDateMillis()).isGreaterThan(0L);
                    assertThat(version.getBranch()).isEqualTo("unknown");
                    assertThat(version.getCommit()).isEqualTo("unknown");
                    assertThat(version.toString()).isEqualTo("1.2.3-0");
                });
        withEnvironmentVariable("CTHING_CI", "")
                .and("GIT_BRANCH", "")
                .and("GIT_COMMIT", "  ")
                .execute(() -> {
                    final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.snapshot);

                    assertThat(ProjectVersion.isDeveloperBuild()).isTrue();
                    assertThat(version.getMajorVersion()).isEqualTo(1);
                    assertThat(version.getMinorVersion()).isEqualTo(2);
                    assertThat(version.getPatchVersion()).isEqualTo(3);
                    assertThat(version.getSemanticVersion()).isEqualTo("1.2.3-0");
                    assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
                    assertThat(version.getBuildNumber()).isEqualTo("0");
                    assertThat(version.getBuildType()).isEqualTo(BuildType.snapshot);
                    assertThat(version.isReleaseBuild()).isFalse();
                    assertThat(version.isSnapshotBuild()).isTrue();
                    assertThat(version.getBuildDate()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
                    assertThat(version.getBuildDateMillis()).isGreaterThan(0L);
                    assertThat(version.getBranch()).isEqualTo("unknown");
                    assertThat(version.getCommit()).isEqualTo("unknown");
                    assertThat(version.toString()).isEqualTo("1.2.3-0");
                });
    }

    @Test
    public void testSnapshotCIVersion() throws Exception {
        withEnvironmentVariable("CTHING_CI", "true")
                .and("GIT_BRANCH", "master")
                .and("GIT_COMMIT", "a5b7f46")
                .execute(() -> {
                    final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.snapshot);

                    assertThat(ProjectVersion.isDeveloperBuild()).isFalse();
                    assertThat(version.getMajorVersion()).isEqualTo(1);
                    assertThat(version.getMinorVersion()).isEqualTo(2);
                    assertThat(version.getPatchVersion()).isEqualTo(3);
                    assertThat(version.getSemanticVersion()).matches("1\\.2\\.3-\\d{2,}");
                    assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
                    assertThat(version.getBuildNumber()).isNotEqualTo("0");
                    assertThat(version.getBuildType()).isEqualTo(BuildType.snapshot);
                    assertThat(version.isReleaseBuild()).isFalse();
                    assertThat(version.isSnapshotBuild()).isTrue();
                    assertThat(version.getBuildDate()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
                    assertThat(version.getBuildDateMillis()).isGreaterThan(0L);
                    assertThat(version.getBranch()).isEqualTo("master");
                    assertThat(version.getCommit()).isEqualTo("a5b7f46");
                    assertThat(version.toString()).matches("1\\.2\\.3-\\d{2,}");
                });
    }

    @Test
    public void testReleaseDeveloperVersion() throws Exception {
        withEnvironmentVariable("CTHING_CI", null)
                .and("GIT_BRANCH", null)
                .and("GIT_COMMIT", null)
                .execute(() -> {
                    final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.release);

                    assertThat(ProjectVersion.isDeveloperBuild()).isTrue();
                    assertThat(version.getMajorVersion()).isEqualTo(1);
                    assertThat(version.getMinorVersion()).isEqualTo(2);
                    assertThat(version.getPatchVersion()).isEqualTo(3);
                    assertThat(version.getSemanticVersion()).isEqualTo("1.2.3-0");
                    assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
                    assertThat(version.getBuildNumber()).isEqualTo("0");
                    assertThat(version.getBuildType()).isEqualTo(BuildType.snapshot);
                    assertThat(version.isReleaseBuild()).isFalse();
                    assertThat(version.isSnapshotBuild()).isTrue();
                    assertThat(version.getBuildDate()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
                    assertThat(version.getBuildDateMillis()).isGreaterThan(0L);
                    assertThat(version.getBranch()).isEqualTo("unknown");
                    assertThat(version.getCommit()).isEqualTo("unknown");
                    assertThat(version.toString()).isEqualTo("1.2.3-0");
                });
    }

    @Test
    public void testReleaseCIVersion() throws Exception {
        withEnvironmentVariable("CTHING_CI", "true")
                .and("GIT_BRANCH", "master")
                .and("GIT_COMMIT", "a5b7f46")
                .execute(() -> {
                    final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.release);

                    assertThat(ProjectVersion.isDeveloperBuild()).isFalse();
                    assertThat(version.getMajorVersion()).isEqualTo(1);
                    assertThat(version.getMinorVersion()).isEqualTo(2);
                    assertThat(version.getPatchVersion()).isEqualTo(3);
                    assertThat(version.getSemanticVersion()).isEqualTo("1.2.3");
                    assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
                    assertThat(version.getBuildNumber()).isNotEqualTo("0");
                    assertThat(version.getBuildType()).isEqualTo(BuildType.release);
                    assertThat(version.isReleaseBuild()).isTrue();
                    assertThat(version.isSnapshotBuild()).isFalse();
                    assertThat(version.getBuildDate()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
                    assertThat(version.getBuildDateMillis()).isGreaterThan(0L);
                    assertThat(version.getBranch()).isEqualTo("master");
                    assertThat(version.getCommit()).isEqualTo("a5b7f46");
                    assertThat(version.toString()).isEqualTo("1.2.3");
                });
    }

    @Test
    public void testNoVersion() {
        assertThat(ProjectVersion.NO_VERSION.getMajorVersion()).isEqualTo(0);
        assertThat(ProjectVersion.NO_VERSION.getMinorVersion()).isEqualTo(0);
        assertThat(ProjectVersion.NO_VERSION.getPatchVersion()).isEqualTo(0);
        assertThat(ProjectVersion.NO_VERSION.getSemanticVersion()).isEqualTo("0.0.0-0");
        assertThat(ProjectVersion.NO_VERSION.getCoreVersion()).isEqualTo("0.0.0");
        assertThat(ProjectVersion.NO_VERSION.getBuildNumber()).isEqualTo("0");
        assertThat(ProjectVersion.NO_VERSION.getBuildType()).isEqualTo(BuildType.snapshot);
        assertThat(ProjectVersion.NO_VERSION.isReleaseBuild()).isFalse();
        assertThat(ProjectVersion.NO_VERSION.isSnapshotBuild()).isTrue();
        assertThat(ProjectVersion.NO_VERSION.getBuildDate()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
        assertThat(ProjectVersion.NO_VERSION.getBuildDateMillis()).isEqualTo(0L);
        assertThat(ProjectVersion.NO_VERSION.getBranch()).isEqualTo("unknown");
        assertThat(ProjectVersion.NO_VERSION.getCommit()).isEqualTo("unknown");
        assertThat(ProjectVersion.NO_VERSION.toString()).isEqualTo("0.0.0-0");
    }

    @Test
    public void testBadVersions() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ProjectVersion("", BuildType.release));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProjectVersion("  ", BuildType.release));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProjectVersion("1.2.a", BuildType.release));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProjectVersion("1.a.0", BuildType.release));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProjectVersion("a.2.0", BuildType.release));
    }

    @Test
    public void testSerialize() throws Exception {
        withEnvironmentVariable("CTHING_CI", "true")
                .and("GIT_BRANCH", "master")
                .and("GIT_COMMIT", "a5b7f46")
                .execute(() -> {
                    final ProjectVersion originalVersion = new ProjectVersion("1.2.3", BuildType.snapshot);
                    assertThat(originalVersion).isInstanceOf(Serializable.class);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (ObjectOutputStream os = new ObjectOutputStream(baos)) {
                        os.writeObject(originalVersion);
                        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                        try (ObjectInputStream is = new ObjectInputStream(bais)) {
                            final ProjectVersion restoredVersion = (ProjectVersion)is.readObject();
                            assertThat(restoredVersion).isEqualTo(originalVersion);
                        }
                    }
                });
    }

    @Test
    public void testOrder() throws Exception {
        withEnvironmentVariable("CTHING_CI", "true")
                .and("GIT_BRANCH", "master")
                .and("GIT_COMMIT", "a5b7f46")
                .execute(() -> {
                    final Date now = new Date();
                    final ProjectVersion ver1 = new ProjectVersion("1.2.3", BuildType.snapshot, now);
                    final ProjectVersion ver2 = new ProjectVersion("1.2.3", BuildType.snapshot, now);
                    final ProjectVersion ver3 = new ProjectVersion("0.2.3", BuildType.snapshot, now);
                    final ProjectVersion ver4 = new ProjectVersion("2.2.3", BuildType.snapshot, now);
                    final ProjectVersion ver5 = new ProjectVersion("1.1.3", BuildType.snapshot, now);
                    final ProjectVersion ver6 = new ProjectVersion("1.3.3", BuildType.snapshot, now);
                    final ProjectVersion ver7 = new ProjectVersion("1.2.1", BuildType.snapshot, now);
                    final ProjectVersion ver8 = new ProjectVersion("1.2.4", BuildType.snapshot, now);
                    final ProjectVersion ver9 = new ProjectVersion("1.2.3", BuildType.snapshot, new Date(10L));
                    final ProjectVersion ver10 = new ProjectVersion("1.2.3", BuildType.release);
                    final ProjectVersion ver11 = new ProjectVersion("1.2.3", BuildType.release);

                    assertThat(ver1).isEqualByComparingTo(ver2);
                    assertThat(ver10).isEqualByComparingTo(ver11);
                    assertThat(ver1).isGreaterThan(ver3);
                    assertThat(ver1).isLessThan(ver4);
                    assertThat(ver1).isGreaterThan(ver5);
                    assertThat(ver1).isLessThan(ver6);
                    assertThat(ver1).isGreaterThan(ver7);
                    assertThat(ver1).isLessThan(ver8);
                    assertThat(ver1).isGreaterThan(ver9);
                    assertThat(ver1).isLessThan(ver10);
                    assertThat(ver10).isGreaterThan(ver1);
                });
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    public void testEquality() {
        EqualsVerifier.forClass(ProjectVersion.class)
                      .usingGetClass()
                      .suppress(Warning.ALL_FIELDS_SHOULD_BE_USED)
                      .verify();
    }
}
