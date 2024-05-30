package com.cfde.playbook_ctd.services;

import com.cfde.playbook_ctd.model.Report;
import com.cfde.playbook_ctd.model.ResponseFormat;
import com.cfde.playbook_ctd.utils.*;
import com.cfde.playbook_ctd.utils.constants.Constants;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class CTDServiceImpl implements CTDService {
    static Logger logger = Logger.getLogger(CTDServiceImpl.class);

    @Value("${inputFilePath}")
    private String inputFilePath;

    @Value("${outputFilePath}")
    private String outputFilePath;

    @Value("${rScriptName}")
    private String rScriptName;

    //for custom CTD matrix
    @Value("${ctdCustomMatrixScriptFolderLocation}")
    private String ctdCustomMatrixScriptFolderLocation;

    @Value("${rCustomMatrixScriptName}")
    private String rCustomMatrixScriptName;

    @Value("${creataCustomMatrixScriptName}")
    private String creataCustomMatrixScriptName;

    @Override
    public ResponseFormat mainCTDList(String graphType, ArrayList<String> geneList){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);
        Path savePathGeneFile = createAbsFilesPath(inputFilePath, createGeneSetFileName(dateTimeJoined));
        Path rScriptLocation = createAbsFilesPath(null, rScriptName);
        logger.info("New file: "+savePathGeneFile.getFileName().toString());

        try {
            copyGenesToLocalInputFile(geneList, savePathGeneFile);
        } catch (IOException ioe) {
            logger.error(StackTracePrinter.printStackTrace(ioe));
            return null;
        }

        return excuteCTD_RScript(graphType, rScriptLocation, savePathGeneFile);
    }

    @Override
    public ResponseFormat mainCTDFile(String graphType, MultipartFile csvGenesFile){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);
        Path savePathGeneFile = createAbsFilesPath(inputFilePath, createGeneSetFileName(dateTimeJoined));
        logger.info("New file: "+savePathGeneFile.getFileName().toString());

        Path rScriptLocation = createAbsFilesPath(null, rScriptName);

        ArrayList<String> geneList = getContentOfSubmittedGenesFile(csvGenesFile);
        if(geneList == null || geneList.size() == 0){
            new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Unable to get the Gene list from uploaded file!"));
        }

        if(geneList.size() < 10 || geneList.size() > 150){
            return new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Number of genes submitted is out of allowed parameters, min number is 10, max is 150!"));
        }

        try {
            copyGenesToLocalInputFile(geneList, savePathGeneFile);
        } catch (IOException ioe) {
            logger.error(StackTracePrinter.printStackTrace(ioe));
            return null;
        }

        return excuteCTD_RScript(graphType, rScriptLocation, savePathGeneFile);
    }

    @Override
    public ResponseEntity<Resource> createCustomAdjacency(MultipartFile geneExpressionsCSV){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);

        Path saveLocationGeneExpressions = createAbsFilesPath(inputFilePath, createGeneExpressionsFileName(dateTimeJoined));
        Path rScriptLocation = createAbsFilesPath(null, rScriptName);

        FileUtilities.copyContentToFileAtPath(geneExpressionsCSV, saveLocationGeneExpressions);

        return createCustomAdjacencyFile(saveLocationGeneExpressions, rScriptLocation);
    }

    @Override
    public ResponseEntity<Resource> getCustomPermutations(MultipartFile adjacencyJSON, MultipartFile geneList){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);

        Path savePathAdjacencyJSON = createAbsFilesPath(inputFilePath, createAdjacencyJSONFileName(dateTimeJoined));
        Path savePathGeneList = createAbsFilesPath(inputFilePath, createGeneSetFileName(dateTimeJoined));
        Path rScriptCustomMatrixPath = createAbsFilesPath(ctdCustomMatrixScriptFolderLocation, rCustomMatrixScriptName);

        FileUtilities.copyContentToFileAtPath(adjacencyJSON, savePathAdjacencyJSON);
        FileUtilities.copyContentToFileAtPath(geneList, savePathGeneList);

        return createCustomPermutationsFile(savePathAdjacencyJSON, savePathGeneList, rScriptCustomMatrixPath);
    }

    @Override
    public ResponseFormat useCustomMatrix(MultipartFile geneExpressionsCSV, MultipartFile csvGenesFile, MultipartFile customRData){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);

        Path savePathExpressions = createAbsFilesPath(inputFilePath, createGeneExpressionsFileName(dateTimeJoined));
        Path savePathGeneFile = createAbsFilesPath(inputFilePath, createGeneSetFileName(dateTimeJoined));
        Path savePathPermutations = createAbsFilesPath(inputFilePath, createRDataFileName(dateTimeJoined));
        Path rScriptLocation = createAbsFilesPath(null, rScriptName);

        FileUtilities.copyContentToFileAtPath(geneExpressionsCSV, savePathExpressions);
        FileUtilities.copyContentToFileAtPath(csvGenesFile, savePathGeneFile);
        FileUtilities.copyContentToFileAtPath(customRData, savePathPermutations);

        return executeCTDRScriptWithCustomFiles(savePathExpressions, savePathGeneFile, savePathPermutations, rScriptLocation);
    }

    private ResponseFormat excuteCTD_RScript(String graphType, Path rScriptLocation, Path absPathGenesFile){
        //example command: Rscript ctd_code.r -g “string” -i "inputFolder/module_genes.csv"
        String rScriptFullPathParam = Constants.R_SCRIPT_PATH;
        String graphTypeParam = Constants.GRAPH_TYPE;
        String absPathToGenesFileParam = Constants.GENES_FILE_PATH;

        String rScriptFullPath = rScriptLocation.toAbsolutePath().toString();
        String absPathToGenesFileFile = absPathGenesFile.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" -g "+graphTypeParam+" -i "+absPathToGenesFileParam;

        Map<String, String> values = new HashMap<>();
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(graphTypeParam, graphType);
        values.put(absPathToGenesFileParam, absPathToGenesFileFile);

        executeShellCommand(baseCommand, values);
        ResponseFormat of = processAndReturnOutputFiles(FilenameUtils.removeExtension(absPathGenesFile.getFileName().toString()));

        //deleting gene list input file
        deleteFileFromPath(new File(absPathGenesFile.toAbsolutePath().toString()));

        return of;
    }

    private ResponseFormat executeCTDRScriptWithCustomFiles(Path savePathExpressions, Path savePathGeneList, Path savePathPermutations, Path rScriptLocation){
        //example command: Rscript ctd_code.r -g “user_input” -i "inputFolder/module_genes.csv" -e "inputFolder/expressions.json" -p "inputFolder/adj_matrix_1234.json"
        String rScriptFullPathParam = Constants.R_SCRIPT_PATH;
        String graphTypeParam = Constants.GRAPH_TYPE;
        String absPathToExpressionsFileParam = Constants.GENE_EXPRESSIONS_CSV;
        String absPathToGenesFileParam = Constants.GENES_FILE_PATH;
        String absPathToPermutationsFileParam = Constants.PERMUTATIONS_FILE_PATH;

        String rScriptFullPath = rScriptLocation.toAbsolutePath().toString();
        String absPathToExpressionsFile = savePathExpressions.toAbsolutePath().toString();
        String absPathToGeneListFile = savePathGeneList.toAbsolutePath().toString();
        String absPathToPermutationsFile = savePathPermutations.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" -g "+graphTypeParam+" -i "+absPathToGenesFileParam+" -e "+absPathToExpressionsFileParam+" -p "+absPathToPermutationsFileParam;

        Map<String, String> values = new HashMap<>();
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(graphTypeParam, Constants.GRAPH_TYPE_USER_INPUT);
        values.put(absPathToExpressionsFileParam, absPathToExpressionsFile);
        values.put(absPathToGenesFileParam, absPathToGeneListFile);
        values.put(absPathToPermutationsFileParam, absPathToPermutationsFile);

        executeShellCommand(baseCommand, values);

        ResponseFormat of = processAndReturnOutputFiles(FilenameUtils.removeExtension(savePathGeneList.getFileName().toString()));

        //deleting gene list csv input file
        deleteFileFromPath(new File(savePathGeneList.toAbsolutePath().toString()));
        //deleting RData input file
        deleteFileFromPath(new File(savePathPermutations.toAbsolutePath().toString()));
        //deleting custom matrix csv input file
        deleteFileFromPath(new File(savePathExpressions.toAbsolutePath().toString()));

        return of;
    }

    private ResponseEntity<Resource> createCustomAdjacencyFile(Path saveLocationGeneExpressions, Path rScriptLocation){
        //example command: Rscript ctd_code.r -g “user_input” -e “inputFolder/geneExpressions_12345.csv"
        String graphTypeParam = Constants.GRAPH_TYPE;
        String rScriptFullPathParam = Constants.R_SCRIPT_PATH;
        String absPathToGeneExpressionsCSVParam = Constants.GENE_EXPRESSIONS_CSV;

        String rScriptFullPath = rScriptLocation.toAbsolutePath().toString();
        String absPathToGeneExpressionsCSV = saveLocationGeneExpressions.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" -g "+graphTypeParam+" -e "+absPathToGeneExpressionsCSVParam;

        Map<String, String> values = new HashMap<>();
        values.put(graphTypeParam, Constants.GRAPH_TYPE_USER_INPUT);
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(absPathToGeneExpressionsCSVParam, absPathToGeneExpressionsCSV);

        executeShellCommand(baseCommand, values);

        String customMatrixNameNoExt = FilenameUtils.removeExtension(saveLocationGeneExpressions.getFileName().toString());
        //example output file name: geneExpressions_TIMESTAMP_adjMatrix.csv
        String customPattern = customMatrixNameNoExt+"_adjMatrix.json";
        File outAdjacencyJSONFile = FileUtilities.getFileFromPatternInFolderPath(outputFilePath, customPattern);


        ResponseEntity re = null;
        if(outAdjacencyJSONFile != null){
            logger.info("Found custom Adjacency JSON file: "+outAdjacencyJSONFile.getName()+", using pattern: "+customPattern);
            re = createFileForResponse(outAdjacencyJSONFile);
            //delete matrix file
            deleteFileFromPath(outAdjacencyJSONFile);
        }else{
            logger.error("Unable to find the Adjacency JSON file in "+outputFilePath+", using patter: "+customPattern);
        }

        //deleting custom matrix csv input file
        deleteFileFromPath(new File(saveLocationGeneExpressions.toAbsolutePath().toString()));

        return re;
    }

    private ResponseEntity<Resource> createCustomPermutationsFile(Path savePathAdjacencyJSON, Path savePathGeneList, Path rScriptCustomMatrixPath){
        //example command: Rscript ctd_wrapper_cluster.R “adjacency.json” “module_genes.csv”
        String rScriptFullPathParam = Constants.R_SCRIPT_PATH;
        String absPathToAdjacencyFileParam = Constants.ADJACENCY_FILE_PATH;
        String absPathToGenesFileParam = Constants.GENES_FILE_PATH;

        String absPathToAdjacencyFile = savePathAdjacencyJSON.toAbsolutePath().toString();
        String absPathToGenesFile = savePathGeneList.toAbsolutePath().toString();
        String rScriptFullPath = rScriptCustomMatrixPath.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" "+absPathToAdjacencyFileParam+" "+absPathToGenesFileParam;

        Map<String, String> values = new HashMap<>();
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(absPathToAdjacencyFileParam, absPathToAdjacencyFile);
        values.put(absPathToGenesFileParam, absPathToGenesFile);

        executeShellCommand(baseCommand, values);

        String customAdjacencyNameNoExt = FilenameUtils.removeExtension(savePathAdjacencyJSON.getFileName().toString());
        String customPattern = customAdjacencyNameNoExt+"\\_.*\\.json";
        File outPermutationsJSONFile = FileUtilities.getFileFromPatternInFolderPath(outputFilePath, customPattern);

        ResponseEntity re = null;
        if(outPermutationsJSONFile != null){
            logger.info("Found Permutations JSON file: "+outPermutationsJSONFile.getName()+", using pattern: "+customPattern);
            re = createFileForResponse(outPermutationsJSONFile);
            //delete RData file
            deleteFileFromPath(outPermutationsJSONFile);
        }else{
            logger.error("Unable to find the Permutations JSON file in "+outputFilePath+", using patter: "+customPattern);
        }

        //delete input files
        deleteFileFromPath(new File(savePathAdjacencyJSON.toAbsolutePath().toString()));
        deleteFileFromPath(new File(savePathGeneList.toAbsolutePath().toString()));
        return re;
    }

    private ResponseEntity createFileForResponse(File outFile){
        ResponseEntity re = null;
        try{
            Path finalFilePath = Paths.get(outFile.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(finalFilePath));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + finalFilePath.getFileName().toString());
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            re = ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(outFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }catch(IOException ioe){
            logger.error(StackTracePrinter.printStackTrace(ioe));
        }
        return re;
    }

    private ResponseFormat processAndReturnOutputFiles(String geneListInpfileNameNoExt){
        ResponseFormat of = new ResponseFormat();

        // highly Connected Genes
        Path highlyConnected = Paths.get(outputFilePath, geneListInpfileNameNoExt+"_highly.csv");
        String[] highlyConnectedArray = readOutputFileContentAndDeleteIt(highlyConnected);
        if(highlyConnectedArray == null){
            of.setReport(new Report(Constants.REPORT_TYPE_NOTE,"No Highly Connected Genes found! "));
        }
        of.setHighlyConnectedGenes(highlyConnectedArray);

        // guilty By Association Genes
        Path guiltyByAssociation = Paths.get(outputFilePath, geneListInpfileNameNoExt+"_guilty.csv");
        String[] guiltyByAssociationArray = readOutputFileContentAndDeleteIt(guiltyByAssociation);
        if(guiltyByAssociationArray == null){
            of.getReport().appendToMessage("No Guilty By Association Genes found! ");
        }
        of.setGuiltyByAssociationGenes(guiltyByAssociationArray);

        // graph Json
        Path graphJson = Paths.get(outputFilePath, geneListInpfileNameNoExt+"_graph.json");
        File graphJsonFile = new File(graphJson.toAbsolutePath().toString());
        if(graphJsonFile != null && FileUtilities.checkIfFileExists(graphJsonFile.getAbsolutePath())){
            logger.info("Reading graphJsonFile!");
            JSONParser jsonParser = new JSONParser();
            try {
                FileReader reader = new FileReader(graphJsonFile);
                Object obj = jsonParser.parse(reader);
                of.setJsonGraph((JSONObject) obj);
            }catch(Exception e){
                logger.error(StackTracePrinter.printStackTrace(e));
                if(highlyConnectedArray == null){
                    of.getReport().appendToMessage("No Graph Nodes available! ");
                }
            }

            //delete graphJsonFile
            String rm_command = "rm -rf " + graphJsonFile.getAbsolutePath();
            Subprocess.shellCall(rm_command);
            logger.info("Deleted: " + graphJsonFile.getAbsolutePath());
        }else{
            logger.error("File not found: "+graphJsonFile.getAbsolutePath());
            of.getReport().appendToMessage("No Graph Nodes available! ");
        }
        return of;
    }

    private ArrayList<String> getContentOfSubmittedGenesFile(MultipartFile csvGenesFile){
        ArrayList<String> genesList = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvGenesFile.getInputStream()));
            if(reader == null){
                logger.error("Unable to open an input stream to the submitted gene list file!");
                return null;
            }
            genesList = new ArrayList<String>();

            String line;
            while ((line = reader.readLine()) != null) {
                if(line.equals("")){
                    continue;
                }
                genesList.add(line.trim());
            }
            return genesList;
        } catch (IOException ioe) {
            logger.error(StackTracePrinter.printStackTrace(ioe));
            return null;
        }
    }

    private void copyGenesToLocalInputFile(ArrayList<String> geneList, Path saveLocation) throws IOException{
        File f = saveLocation.toFile();
        if (f.createNewFile()){
            FileWriter fileWriter = new FileWriter(f);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            for(String geneName : geneList){
                printWriter.println(geneName);
            }
            printWriter.flush();
            printWriter.close();
        }
    }

    private Path createAbsFilesPath(String relativeFolderPath, String fileName){
        if(relativeFolderPath != null){
            Path relativeFolderPathObj = Paths.get(relativeFolderPath + File.separator);
            File f = new File(relativeFolderPathObj.toString());
            return Paths.get( f.getAbsolutePath() + File.separator + StringUtils.cleanPath(fileName));
        }else{
            File f = new File(fileName);
            return Paths.get(f.getAbsolutePath());
        }
    }

    private void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            logger.error(StackTracePrinter.printStackTrace(e));
        }
    }

    private String[] readOutputFileContentAndDeleteIt(Path pathToFile){
        File highlyConnectedFile = new File(pathToFile.toAbsolutePath().toString());
        if(FileUtilities.checkIfFileExists(highlyConnectedFile.getAbsolutePath())){
            logger.info("Reading: "+pathToFile.getFileName());
            ArrayList<String> content =  FileUtilities.readFileContentLineByLine(highlyConnectedFile);

            //delete highlyConnectedFile
            deleteFileFromPath(highlyConnectedFile);

            if(content == null || content.size() == 0){
                return null;
            }
            return content.toArray(new String[0]);
        }else{
            logger.error("File not found: "+highlyConnectedFile.getAbsolutePath());
            return null;
        }
    }

    private void executeShellCommand(String baseCommand, Map<String, String> values){
        String formattedCommand = ShellCommandFormatter.formatted(baseCommand, values);
        logger.info("Starting R Script: "+formattedCommand);
        long startT = System.currentTimeMillis();
        Subprocess.shellCall(formattedCommand);
        long totalTime = System.currentTimeMillis()-startT;
        logger.info("Execution COMPLETE, total time in seconds: "+Math.round(totalTime/1000));
    }

    private void deleteFileFromPath(File fileToDelete){
        String rm_command = "rm -rf " + fileToDelete.getAbsolutePath();
        Subprocess.shellCall(rm_command);
        logger.info("Deleted: " + fileToDelete.getAbsolutePath());
    }

    private String createGeneSetFileName(String dateTimeJoined){
        return "genes_"+dateTimeJoined+".csv";
    }

    private String createAdjacencyJSONFileName(String dateTimeJoined){
        return "adjMatrix_"+dateTimeJoined+".json";
    }

    private String createGeneExpressionsFileName(String dateTimeJoined){
        return "geneExpressions_"+dateTimeJoined+".csv";
    }

    private String createRDataFileName(String dateTimeJoined){
        return "computations_"+dateTimeJoined+".RData";
    }

    /*
    Path rDataPath = Paths.get(outputFilePath+"/"+FilenameUtils.removeExtension(saveLocationMatrix.getFileName().toString())+"_123456.RData");
    simulateRScript(rDataPath);
    logger.info("Created simulated file: "+rDataPath.toAbsolutePath().toString());

    private void simulateRScript(Path finalRDataPath){
        try{
            File outFile = new File(finalRDataPath.toAbsolutePath().toString());
            outFile.createNewFile();
            if(!outFile.exists()){
                logger.error("Error, unable to create output simulated file: "+finalRDataPath.toString());
            }

            PrintWriter pwOutFile = new PrintWriter(outFile);
            pwOutFile.println("This is test content!");
            pwOutFile.close();
        }catch(IOException ioe){
            logger.error(StackTracePrinter.printStackTrace(ioe));
        }
    }

    //simulate file creation
        Path savePathTest = createAbsFilesPath(outputFilePath, customMatrixNameNoExt+"_adjMatrix.json");
        try {
            Files.createFile(savePathTest);
            if(Files.exists(savePathTest)){
                logger.info("Created file: "+savePathTest.toAbsolutePath());
            }else{
                logger.info("Unable to create file: "+savePathTest.toAbsolutePath());
            }
        } catch (IOException ioe) {
            logger.error(StackTracePrinter.printStackTrace(ioe));
        }

        //simulate process duration
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            logger.error(StackTracePrinter.printStackTrace(e));
        }
    */
}
