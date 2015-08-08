package com.smartcodeltd;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.smartcodeltd.domain.Version;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLSource;
import de.pdark.decentxml.XMLStringSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

abstract public class ReleaseCandidateMojo
    extends AbstractMojo
{
    private final static String default_output_uri      = "stdout";
    private final static String default_output_template = "{{ version }}";
    private final static String default_version_format  = "{{ version }}";
    private final static String default_encoding        = "UTF-8";

    protected final Charset charset;

    /**
     * Describes how the pom.xml project version should be translated to a new version number.
     *
     * Let's say that the project version is set to `1.2.0-beta-SNAPSHOT`, specifying `releaseVersionFormat` as per below examples
     * will result in following outcomes:
     *
     * Format                       Outcome                 Comment
     * --------------------------------------------------------------------------------------------------------------------------------
     * {{ version }}                1.2.0-beta-SNAPSHOT     Leave the version as it is
     * {{ api_version }}            1.2.0                   Use only the API version
     * {{ qualified_api_version }}  1.2.0-beta              API version plus the qualifier
     * {{ timestamp('YYYYMMdd') }}  20150801                Current time (JodaTime-compatible timestamp format can be used as argument)
     *                                                      http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
     *
     * Above tokens can also be used together, so specifying
     *   releaseVersionFormat: "{{ api_version }}.{{ timestamp('YYYYMMdd') }}"
     *   yields:               1.2.0.20150801
     *
     *   releaseVersionFormat: "{{ api_version }}-builton.{{ timestamp('YYYYMMdd') }}"
     *   yields:               1.2.0-builton.20150801
     *
     * Standard maven tokens can be used as well, so if you provide, say a `build_number` parameter when you build your project:
     *   mvn clean package -Dbuild_number=176
     *
     * and given that you set `releaseVersionFormat` to:
     *   "{{ qualified_api_version }}-build.${build_number}"
     *
     * the resulting version number will be:
     *   1.2.0-build.176
     */
    @Parameter(defaultValue = default_version_format, required = false, property = "releaseVersionFormat")
    protected String releaseVersionFormat;

    /**
     * Encoding used when reading and writing to files on disk.
     */
    @Parameter(defaultValue = default_encoding, required = false)
    protected String encoding;

    /**
     * Defines where to direct the output of `release-candidate:version`
     *
     * Setting outputUri to `stdout` will print output to the console,
     * using `file://${project.basedir}/project.properties` will direct the output to `project.properties` file.
     *
     * Please note that you should use an absolute path when specifying the outputUri.
     * You can get hold of your project base directory by using `${project.basedir}` as per the example above.
     */
    @Parameter(defaultValue = default_output_uri, required = false, property = "outputUri")
    protected URI outputUri;

    /**
     * Defines how to structure the output of `release-candidate:version`
     *
     * If your build server of choice understands text output produced by maven (which is the case if you're using
     * TeamCity), you can specify the `outputTemplate` as:
     *
     * <pre>
     * {@code
     * <outputTemplate>
     *  ##teamcity[setParameter name='env.PROJECT_VERSION' value='{{ version }}']
     *  ##teamcity[message text='Project version: {{ version }}']
     * </outputTemplate>
     * }
     * </pre>
     *
     * If your build server prefers to use env variables defined using property files (Jenkins with EnvInject plugin)
     * you can specify the `outputTemplate as:
     *
     * <pre>
     * {@code
     * <outputTemplate>PROJECT_VERSION={{ version }}</outputTemplate>
     * }
     * </pre>
     *
     * Please note that when using multi-line templates leading whitespace characters will be stripped.
     */
    @Parameter(defaultValue = default_output_template, required = false, property = "outputTemplate")
    protected String outputTemplate;

    @Parameter(defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    public ReleaseCandidateMojo() {
        this.charset = Charset.forName(getOrElse(encoding, default_encoding));
    }

    protected void info(String template, Object... values) {
        getLog().info(String.format(template, values));
    }

    protected <T> T with(T value) {
        return value;
    }

    protected Version versionOf(MavenProject project) {
        return new Version(project.getVersion());
    }

    protected <T> T getOrElse(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    protected Document parsed(File pom) throws IOException {
        XMLParser parser = new XMLParser();
        XMLSource source = new XMLStringSource(CharStreams.toString(Files.newReader(pom, charset)));

        return parser.parse(source);
    }
}