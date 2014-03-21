package org.fluxtream.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GetLastGitCommit {
    public static void main(final String[] args) {
	String commitId = "badbadbadbad";
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "log", "-1");
            //pb.directory(new File("."));
            Process p = pb.start();
            final List<String> lines = IOUtils.readLines(p.getInputStream());
            commitId = lines.get(0).split(" ")[1].substring(0, 12);
        } catch (final Throwable t) {}
        try {
            final File flxWebDir = new File("fluxtream-web");
            final String propertiesPath = flxWebDir.exists() ? "fluxtream-web/src/main/resources/lastcommit.properties" : "src/main/resources/lastcommit.properties";
            final FileOutputStream output = new FileOutputStream(propertiesPath);
            IOUtils.write(String.format("release=%s_%s", args[0], commitId), output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
