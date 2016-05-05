package org.dbpedia.links;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static org.dbpedia.links.LinksUtils.filterFileWithEndsWith;
import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    private static Logger L = Logger.getLogger(GenerateLinks.class);

    public static void main(String[] args) throws Exception {

        File f = new File(Paths.get(".").toAbsolutePath().normalize().toString()).getParentFile();  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        //ExecuteShellScriptsFromFolders(filterFileWithEndsWith(allFilesInRepo, ".sh"));
        // alternative to above but uses only scripts defined in metadata.ttl filesvn
        ExecuteShellScriptsFromAllMetadataFiles(filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));


    }

    private static void ExecuteShellScriptsFromFolders(List<File> filesList){
        filesList.stream().forEach(file -> {

            	executeShellScript(file);

        });
    }

    private static void ExecuteShellScriptsFromAllMetadataFiles(List<File> filesList){
        filesList.stream().forEach(file -> {

            L.info("Processing " + file);
            Model model = LinksUtils.getModelFromFile(file);
            model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script"))
                .forEachRemaining( node -> {

                    String scriptFilePath = null;
                    try {
                        scriptFilePath = FilenameUtils.normalize(new URI(node.asResource().getURI()).getPath());
                    } catch (URISyntaxException e) {
                        L.error(e);
                    }

                    L.info("  Script   " + scriptFilePath);
                    File scriptFile = new File(scriptFilePath);
                    if (scriptFile.exists()) {
                        executeShellScript(scriptFile);
                    } else {
                        L.warn("  No file           " + scriptFilePath);
                    }
                });


        });
    }

    private static void executeShellScript(File file) {
        L.info(" Executing " + file.getAbsolutePath());

        Process p = null;
        try {
        	String[] cmd = {"/bin/bash","-c"," ( cd "+ file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ) "};
//        	System.out.println(" bash -c cd " + file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ");
//            p = Runtime.getRuntime().exec(" bash -c ( cd " + file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ) ");
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader read = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                p.waitFor();

            } catch (InterruptedException e) {
                throw new IllegalArgumentException("Cannot execute script: " + file.getAbsolutePath(), e);
            }
            while (read.ready()) {
                System.out.println(read.readLine());
            }
        } catch (IOException  e) {
            throw new IllegalArgumentException("Cannot execute script: " + file.getAbsolutePath(), e);
        }
    }
}