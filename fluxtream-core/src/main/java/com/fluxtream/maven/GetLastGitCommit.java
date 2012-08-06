package com.fluxtream.maven;

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
        try {
            System.out.println("retrieving last commit from git");
            ProcessBuilder pb = new ProcessBuilder("git", "log", "-1");
            //pb.directory(new File("."));
            Process p = pb.start();
            final List<String> lines = IOUtils.readLines(p.getInputStream());
            System.out.println(lines.get(0).split(" ")[1]);
            File flxWebDir = new File("fluxtream-web");
            String propertiesPath = "fluxtream-web/src/main/resources/lastcommit.properties";
            if (!flxWebDir.exists())
                propertiesPath = "src/main/resources/lastcommit.properties";
            FileOutputStream output = new FileOutputStream(propertiesPath);
            IOUtils.write("release="+lines.get(0).split(" ")[1], output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
