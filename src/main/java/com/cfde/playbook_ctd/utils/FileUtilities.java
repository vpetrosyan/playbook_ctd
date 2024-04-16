package com.cfde.playbook_ctd.utils;

import org.apache.log4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtilities {

    static Logger logger = Logger.getLogger(FileUtilities.class);

    public static boolean checkIfFileExists(String path) {
        try {
            if(Files.exists(Paths.get(path))){
                logger.info("File found: "+path);
                return true;
            }else{
                logger.info("WARNING: File: "+path+" NOT found!");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static ArrayList<String> readFileContentLineByLine(File f){
        BufferedReader reader;
        ArrayList<String> fileLines = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            fileLines = new ArrayList<String>();

            String line = reader.readLine();
            while (line != null) {
                fileLines.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return fileLines;
    }

    public static boolean checkFileForPattern(File file, String pattern) {
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String lineFromFile = scanner.nextLine();
                if (lineFromFile.contains(pattern)) {
                    // a match!
                    return true;
                }
            }

            return false;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    public static File getFileFromPatternInFolderPath(String path, String regexPattern){
        File dir = new File(path);
        //get all files and folders in the utput directory, depth: 1
        List<String> filesAndDirName = Arrays.asList(dir.list());
        for(String fileOrDirName : filesAndDirName){
            String fileFullPath = path+"/"+fileOrDirName;
            File f = new File(fileFullPath);
            if(!f.isDirectory()){
                Pattern p = Pattern.compile(regexPattern);
                Matcher m = p.matcher(f.getName());
                if(m.matches()){
                    return f;
                }
            }
        }
        return null;
    }

    public static List<File> getAllDirectoriesFromPath(String path){
        File dir = new File(path);
        List<File> dirsToScan = new ArrayList<File>();
        //get all files and folders in the utput directory, depth: 1
        List<String> filesAndDirName = Arrays.asList(dir.list());
        for(String fileOrDirName : filesAndDirName){
            String fileFullPath = path+"/"+fileOrDirName;
            logger.info("Searching fileFullPath: "+fileFullPath);
            File f = new File(fileFullPath);
            if(f.isDirectory() || (f.list() != null && f.list().length >= 0)){
                dirsToScan.add(f);
            }
        }
        return dirsToScan;
    }

    public static String copyContentToFileAtPath(MultipartFile mf, Path copyLocation){
        try {
            Files.copy(mf.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            return copyLocation.toAbsolutePath().toString();
        } catch (IOException ioe) {
            logger.error(StackTracePrinter.printStackTrace(ioe));
            return null;
        }
    }

    public static String concatonateFileContentInto(Path[] inpFilesPath, Path outFilePath){
        try {
            File outFile = new File(outFilePath.toString());
            outFile.createNewFile();
            if(!outFile.exists()){
                logger.error("Error, unable to create out file: "+outFilePath.toString());
                return null;
            }
            PrintWriter pwOutFile = new PrintWriter(outFile);

            BufferedReader brInpt = null;
            for (Path path : inpFilesPath) {
                File impF = new File(path.toString());
                if(impF.exists()){
                    brInpt = new BufferedReader(new FileReader(impF));
                    String line = brInpt.readLine();
                    while (line != null) {
                        pwOutFile.println(line);
                        line = brInpt.readLine();
                    }
                    brInpt.close();
                }else{
                    logger.error("Error, file "+path.toString()+" does not exist!");
                }
            }
            pwOutFile.close();
            return outFilePath.toString();
        }catch(Exception e){
            logger.error("Error concat: "+e.getMessage()+", using outFastqFile: "+outFilePath.toString());
            e.printStackTrace();
            return null;
        }
    }
}
