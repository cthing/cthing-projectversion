/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.projectversion;

import java.io.Serial;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;


/**
 * Represents a project version. This is a semantic version with additional build identification information.
 * The additional information indicates the build type and whether the build is being performed by the C Thing
 * Software Continuous Integration service or on a developer's machine.
 *
 * <table>
 *     <caption style="padding-top: 5px; font-weight: bold;">Versioning Scheme</caption>
 *     <thead>
 *         <tr>
 *             <th style="text-align: left; padding: 5px;">Build Environment</th>
 *             <th style="text-align: left; padding: 5px;">Requested Build Type</th>
 *             <th style="text-align: left; padding: 5px;">Actual Build Type</th>
 *             <th style="text-align: left; padding: 5px;">Semantic Version</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td style="text-align: left; padding: 5px;">CTHING_CI defined (i.e. CI build)</td>
 *             <td style="text-align: left; padding: 5px;">snapshot</td>
 *             <td style="text-align: left; padding: 5px;">snapshot</td>
 *             <td style="text-align: left; padding: 5px;">n.n.n-t (t = ms since Unix Epoch)</td>
 *         </tr>
 *         <tr>
 *             <td style="text-align: left; padding: 5px;">CTHING_CI undefined (i.e. developer build)</td>
 *             <td style="text-align: left; padding: 5px;">snapshot</td>
 *             <td style="text-align: left; padding: 5px;">snapshot</td>
 *             <td style="text-align: left; padding: 5px;">n.n.n-0</td>
 *         </tr>
 *         <tr>
 *             <td style="text-align: left; padding: 5px;">CTHING_CI defined</td>
 *             <td style="text-align: left; padding: 5px;">release</td>
 *             <td style="text-align: left; padding: 5px;">release</td>
 *             <td style="text-align: left; padding: 5px;">n.n.n</td>
 *         </tr>
 *         <tr>
 *             <td style="text-align: left; padding: 5px;">CTHING_CI undefined</td>
 *             <td style="text-align: left; padding: 5px;">release</td>
 *             <td style="text-align: left; padding: 5px;">snapshot</td>
 *             <td style="text-align: left; padding: 5px;">n.n.n-0</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @see "http://semver.org/"
 */
public final class ProjectVersion implements Comparable<ProjectVersion>, Serializable {

    /** A version indicating no version has been specified. */
    public static final ProjectVersion NO_VERSION;

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern SEMVER_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final int BASE_10 = 10;
    private static final String BUILD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String UNKNOWN_COMMIT = "unknown";
    private static final String UNKNOWN_BRANCH = "unknown";

    static {
        NO_VERSION = new ProjectVersion();
    }

    private final String coreVersion;
    private final String semanticVersion;
    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;
    private final String buildNumber;
    private final BuildType buildType;
    private final String buildDate;
    private final long buildDateMillis;
    private final String branch;
    private final String commit;

    /**
     * Constructs a semantic version based on the specified version string, the type of build, the
     * specified time of the build, and the Git branch and commit hash.
     *
     * @param coreVersion Major, minor and patch version (e.g. 1.2.3)
     * @param buildType Type of build (e.g. release, snapshot)
     * @param now The date of the build
     * @param branch Name of the Git repository branch containing this version. May by {@code null} if unknown.
     * @param commit Git commit hash of this version. May be {@code null} if unknown.
     */
    public ProjectVersion(final String coreVersion, final BuildType buildType, final Date now,
                          @Nullable final String branch, @Nullable final String commit) {
        this.coreVersion = coreVersion.trim();
        if (this.coreVersion.isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or blank");
        }

        final Matcher coreMatcher = SEMVER_PATTERN.matcher(this.coreVersion);
        if (!coreMatcher.matches()) {
            throw new IllegalArgumentException("Version must consist of three positive integers: major.minor.patch");
        }

        this.majorVersion = Integer.parseInt(coreMatcher.group(1), BASE_10);
        this.minorVersion = Integer.parseInt(coreMatcher.group(2), BASE_10);
        this.patchVersion = Integer.parseInt(coreMatcher.group(3), BASE_10);

        if (isDeveloperBuild()) {
            this.buildNumber = "0";
            this.buildType = BuildType.snapshot;
        } else {
            this.buildNumber = Long.toString(now.getTime());
            this.buildType = buildType;
        }

        final DateFormat dateFormat = new SimpleDateFormat(BUILD_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.buildDate = dateFormat.format(now);
        this.buildDateMillis = now.getTime();

        this.branch = (branch == null || branch.isBlank()) ? UNKNOWN_BRANCH : branch;
        this.commit = (commit == null || commit.isBlank()) ? UNKNOWN_COMMIT : commit;

        this.semanticVersion = isSnapshotBuild() ? this.coreVersion + "-" + this.buildNumber : this.coreVersion;
    }

    /**
     * Constructs a semantic version based on the specified version string, the type of build and the
     * specified time of the build. If the {@code GIT_BRANCH} and {@code GIT_COMMIT} environment variables
     * are defined, they are used as the version's branch name and commit hash.
     *
     * @param semanticVersion Version string in Semantic Version format
     * @param buildType Type of build (e.g. release, snapshot)
     * @param now The date of the build
     */
    public ProjectVersion(final String semanticVersion, final BuildType buildType, final Date now) {
        this(semanticVersion, buildType, now, System.getenv("GIT_BRANCH"), System.getenv("GIT_COMMIT"));
    }

    /**
     * Constructs a semantic version based on the specified version string, and the type of build. It is
     * assumed that the build is taking place now. If the {@code GIT_BRANCH} and {@code GIT_COMMIT} environment
     * variables are defined, they are used as the version's branch name and commit hash.
     *
     * @param semanticVersion Version string in Semantic Version format
     * @param buildType Type of build (e.g. release, snapshot)
     */
    public ProjectVersion(final String semanticVersion, final BuildType buildType) {
        this(semanticVersion, buildType, new Date());
    }

    private ProjectVersion() {
        this.coreVersion = "0.0.0";
        this.majorVersion = 0;
        this.minorVersion = 0;
        this.patchVersion = 0;
        this.buildNumber = "0";
        this.buildType = BuildType.snapshot;
        this.branch = UNKNOWN_BRANCH;
        this.commit = UNKNOWN_COMMIT;

        this.buildDateMillis = 0L;
        final DateFormat dateFormat = new SimpleDateFormat(BUILD_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.buildDate = dateFormat.format(new Date(this.buildDateMillis));

        this.semanticVersion = this.coreVersion + "-" + this.buildNumber;
    }

    /**
     * Obtains the complete semantic version including the pre-release portion, if this is a snapshot build.
     *
     * @return Complete semantic version (e.g. 1.2.3-1716422556680).
     */
    public String getSemanticVersion() {
        return this.semanticVersion;
    }

    /**
     * Obtains the major, minor and patch version without the pre-release portion.
     *
     * @return Major, minor, and patch version (e.g. 1.2.3)
     */
    public String getCoreVersion() {
        return this.coreVersion;
    }

    /**
     * Obtains the major version component (i.e. first component) of the semantic version.
     *
     * @return Major version component
     */
    public int getMajorVersion() {
        return this.majorVersion;
    }

    /**
     * Obtains the minor version component (i.e. second component) of the semantic version.
     *
     * @return Minor version component
     */
    public int getMinorVersion() {
        return this.minorVersion;
    }

    /**
     * Obtains the patch component (i.e. third component) of the semantic version.
     *
     * @return Patch component
     */
    public int getPatchVersion() {
        return this.patchVersion;
    }

    /**
     * Obtains the build number for this version.
     *
     * @return Build number
     */
    public String getBuildNumber() {
        return this.buildNumber;
    }

    /**
     * Obtains the type of the build represented by this version.
     *
     * @return Build type
     */
    public BuildType getBuildType() {
        return this.buildType;
    }

    /**
     * Indicates whether this version represents a release build.
     *
     * @return {@code true} if this version represents a release build.
     */
    public boolean isReleaseBuild() {
        return this.buildType == BuildType.release;
    }

    /**
     * Indicates whether this version represents a snapshot build.
     *
     * @return {@code true} if this version represents a snapshot build.
     */
    public boolean isSnapshotBuild() {
        return this.buildType == BuildType.snapshot;
    }

    /**
     * Indicates whether the version represents a build taking place on a developer's machine.
     *
     * @return {@code true} if the build is taking place on a developer's machine.
     */
    public static boolean isDeveloperBuild() {
        final String cthingCI = System.getenv("CTHING_CI");
        return cthingCI == null || cthingCI.isBlank();
    }

    /**
     * Obtains the date that this version was built.
     *
     * @return Build date in ISO 8601 format (i.e. {@code yyyy-MM-dd'T'HH:mm:ssXXX})
     */
    public String getBuildDate() {
        return this.buildDate;
    }

    /**
     * Obtains the data that this version was built.
     *
     * @return Build date as the number of milliseconds since the Unix Epoch.
     */
    public long getBuildDateMillis() {
        return this.buildDateMillis;
    }

    /**
     * Obtains the Git branch name from which this version was built.
     *
     * @return Git branch name or "unknown"
     */
    public String getBranch() {
        return this.branch;
    }

    /**
     * Obtains the Git commit hash from which this version was built.
     *
     * @return Git commit hash or "unknown"
     */
    public String getCommit() {
        return this.commit;
    }

    @Override
    public int compareTo(final ProjectVersion that) {
        int cmp = Integer.compare(this.majorVersion, that.majorVersion);
        if (cmp != 0) {
            return cmp;
        }

        cmp = Integer.compare(this.minorVersion, that.minorVersion);
        if (cmp != 0) {
            return cmp;
        }

        cmp = Integer.compare(this.patchVersion, that.patchVersion);
        if (cmp != 0) {
            return cmp;
        }

        if (isReleaseBuild() && that.isReleaseBuild()) {
            return 0;
        }
        if (isReleaseBuild()) {
            return 1;
        }
        if (that.isReleaseBuild()) {
            return -1;
        }

        return Long.compare(this.buildDateMillis, that.buildDateMillis);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final ProjectVersion that = (ProjectVersion)obj;
        return this.majorVersion == that.majorVersion
                && this.minorVersion == that.minorVersion
                && this.patchVersion == that.patchVersion
                && Objects.equals(this.buildNumber, that.buildNumber)
                && this.buildType == that.buildType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.majorVersion, this.minorVersion, this.patchVersion, this.buildNumber, this.buildType);
    }

    /**
     * Equivalent to calling {@link #getSemanticVersion()}.
     *
     * @return Complete semantic version.
     */
    @Override
    public String toString() {
        return this.semanticVersion;
    }
}
