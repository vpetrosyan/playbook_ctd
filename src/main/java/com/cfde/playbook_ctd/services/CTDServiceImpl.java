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
            new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Unable to extract genes from uploaded file!"));
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
    public ResponseEntity<Resource> createCustomMatrix(MultipartFile geneExpressionsCSV){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);

        Path saveLocationGeneExpressions = createAbsFilesPath(inputFilePath, createGeneExpressionsFileName(dateTimeJoined));
        Path rScriptLocation = createAbsFilesPath(null, rScriptName);

        FileUtilities.copyContentToFileAtPath(geneExpressionsCSV, saveLocationGeneExpressions);

        return createACustomMatrixFile(saveLocationGeneExpressions, rScriptLocation);
    }

    @Override
    public ResponseFormat useCustomMatrix(MultipartFile matrix, MultipartFile csvGenesFile, MultipartFile customRData){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);

        Path saveLocationMatrix = createAbsFilesPath(inputFilePath, createAdjMatrixFileName(dateTimeJoined));
        Path saveLocationGeneFile = createAbsFilesPath(inputFilePath, createGeneSetFileName(dateTimeJoined));
        Path saveLocationRData = createAbsFilesPath(inputFilePath, createRDataFileName(dateTimeJoined));
        Path rScriptLocation = createAbsFilesPath(null, rScriptName);

        FileUtilities.copyContentToFileAtPath(matrix, saveLocationMatrix);
        FileUtilities.copyContentToFileAtPath(csvGenesFile, saveLocationGeneFile);
        FileUtilities.copyContentToFileAtPath(customRData, saveLocationRData);

        return executeCTDRScriptWithCustomFiles(saveLocationMatrix, saveLocationGeneFile, saveLocationRData, rScriptLocation);
    }

    @Override
    public ResponseEntity<Resource> getCtdCustomRData(MultipartFile matrix, MultipartFile geneList){
        String dateTimeJoined = DateUtils.dateToStringParser(new Date(), Constants.TIME_PATTERN_JOINED);

        Path saveLocationMatrix = createAbsFilesPath(inputFilePath, createAdjMatrixFileName(dateTimeJoined));
        Path saveLocationGeneList = createAbsFilesPath(inputFilePath, createGeneSetFileName(dateTimeJoined));
        Path rScriptCustomMatrixPath = createAbsFilesPath(ctdCustomMatrixScriptFolderLocation, rCustomMatrixScriptName);

        FileUtilities.copyContentToFileAtPath(matrix, saveLocationMatrix);
        FileUtilities.copyContentToFileAtPath(geneList, saveLocationGeneList);

        return executeCustomMatrixCTD_RScript(saveLocationMatrix, saveLocationGeneList, rScriptCustomMatrixPath);
    }

    private ResponseFormat excuteCTD_RScript(String graphType, Path rScriptLocation, Path absPathGenesFile){
        //example command: Rscript ctd_code.r -g “string” -i "inputFolder/module_genes.csv"
        String rScriptFullPathParam = "{R_SCRIPT_PATH}";
        String graphTypeParam = "{GRAPH_TYPE}";
        String absPathToGenesFileParam = "{GENES_FILE_PATH}";

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

    private ResponseFormat executeCTDRScriptWithCustomFiles(Path saveLocationMatrix, Path saveLocationGeneList, Path saveLocationRData, Path rScriptLocation){
        //example command: Rscript ctd_code.r -g “user_input” -i "inputFolder/module_genes.csv" -a "inputFolder/adj_matrix.csv" -p "inputFolder/adj_matrix_1234.RData"
        String rScriptFullPathParam = "{R_SCRIPT_PATH}";
        String graphTypeParam = "{GRAPH_TYPE}";
        String absPathToMatrixFileParam = "{MATRIX_FILE_PATH}";
        String absPathToGenesFileParam = "{GENES_FILE_PATH}";
        String absPathToRDataFileParam = "{RDATA_FILE_PATH}";

        String rScriptFullPath = rScriptLocation.toAbsolutePath().toString();
        String absPathToMatrixFile = saveLocationMatrix.toAbsolutePath().toString();
        String absPathToGeneListFile = saveLocationGeneList.toAbsolutePath().toString();
        String absPathToRDataFile = saveLocationRData.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" -g "+graphTypeParam+" -i "+absPathToGenesFileParam+" -a "+absPathToMatrixFileParam+" -p "+absPathToRDataFileParam;

        Map<String, String> values = new HashMap<>();
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(graphTypeParam, "user_input");
        values.put(absPathToMatrixFileParam, absPathToMatrixFile);
        values.put(absPathToGenesFileParam, absPathToGeneListFile);
        values.put(absPathToRDataFileParam, absPathToRDataFile);

        executeShellCommand(baseCommand, values);

        ResponseFormat of = processAndReturnOutputFiles(FilenameUtils.removeExtension(saveLocationGeneList.getFileName().toString()));

        //deleting gene list csv input file
        deleteFileFromPath(new File(saveLocationGeneList.toAbsolutePath().toString()));
        //deleting RData input file
        deleteFileFromPath(new File(saveLocationRData.toAbsolutePath().toString()));
        //deleting custom matrix csv input file
        deleteFileFromPath(new File(saveLocationMatrix.toAbsolutePath().toString()));

        return of;
    }

    private ResponseEntity<Resource> createACustomMatrixFile(Path saveLocationGeneExpressions, Path rScriptLocation){
        //example command: Rscript ctd_code.r -e “inputFolder/geneExpressions_12345.csv"
        String rScriptFullPathParam = "{R_SCRIPT_PATH}";
        String absPathToGeneExpressionsCSVParam = "{GENE_EXPRESSIONS_CSV}";

        String rScriptFullPath = rScriptLocation.toAbsolutePath().toString();
        String absPathToGeneExpressionsCSV = saveLocationGeneExpressions.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" -e "+absPathToGeneExpressionsCSVParam;

        Map<String, String> values = new HashMap<>();
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(absPathToGeneExpressionsCSVParam, absPathToGeneExpressionsCSV);

        executeShellCommand(baseCommand, values);

        String customMatrixNameNoExt = FilenameUtils.removeExtension(saveLocationGeneExpressions.getFileName().toString());
        //example output file name: geneExpressions_TIMESTAMP_adjMatrix.csv
        String customPattern = customMatrixNameNoExt+"\\_adjMatrix.csv";
        File outMatrixFile = FileUtilities.getFileFromPatternInFolderPath(outputFilePath, customPattern);


        ResponseEntity re = null;
        if(outMatrixFile != null){
            logger.info("Found custom matrix file: "+outMatrixFile.getName()+", using pattern: "+customPattern);

            try{
                Path finalMatrixPath = Paths.get(outMatrixFile.getAbsolutePath());
                ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(finalMatrixPath));

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + finalMatrixPath.getFileName().toString());
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");

                re = ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(outMatrixFile.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            }catch(IOException ioe){
                logger.error(StackTracePrinter.printStackTrace(ioe));
            }

            //delete matrix file
            deleteFileFromPath(outMatrixFile);
        }else{
            logger.error("Unable to find the RData file in "+outputFilePath+", using patter: "+customPattern);
        }

        //deleting custom matrix csv input file
        deleteFileFromPath(new File(saveLocationGeneExpressions.toAbsolutePath().toString()));
        return re;
    }

    private ResponseEntity<Resource> executeCustomMatrixCTD_RScript(Path saveLocationMatrix, Path saveLocationGeneList, Path rScriptCustomMatrixPath){
        //example command: Rscript ctd_wrapper_cluster.R “adj_matrix.csv” “module_genes.csv”
        String rScriptFullPathParam = "{R_SCRIPT_PATH}";
        String absPathToMatrixFileParam = "{MATRIX_FILE_PATH}";
        String absPathToGenesFileParam = "{GENES_FILE_PATH}";

        String absPathToMatrixFile = saveLocationMatrix.toAbsolutePath().toString();
        String absPathToGenesFile = saveLocationGeneList.toAbsolutePath().toString();
        String rScriptFullPath = rScriptCustomMatrixPath.toAbsolutePath().toString();

        String baseCommand = "Rscript "+rScriptFullPathParam+" "+absPathToMatrixFileParam+" "+absPathToGenesFileParam;

        Map<String, String> values = new HashMap<>();
        values.put(rScriptFullPathParam, rScriptFullPath);
        values.put(absPathToMatrixFileParam, absPathToMatrixFile);
        values.put(absPathToGenesFileParam, absPathToGenesFile);

        executeShellCommand(baseCommand, values);

        String customMatrixNameNoExt = FilenameUtils.removeExtension(saveLocationMatrix.getFileName().toString());
        String customPattern = customMatrixNameNoExt+"\\_.*\\.RData";
        File outRDataFile = FileUtilities.getFileFromPatternInFolderPath(outputFilePath, customPattern);

        ResponseEntity re = null;
        if(outRDataFile != null){
            logger.info("Found RData file: "+outRDataFile.getName()+", using pattern: "+customPattern);
            try{
                Path finalRDataPath = Paths.get(outRDataFile.getAbsolutePath());
                ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(finalRDataPath));

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + finalRDataPath.getFileName().toString());
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");

                re = ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(outRDataFile.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            }catch(IOException ioe){
                logger.error(StackTracePrinter.printStackTrace(ioe));
            }

            //delete RData file
            deleteFileFromPath(outRDataFile);
        }else{
            logger.error("Unable to find the RData file in "+outputFilePath+", using patter: "+customPattern);
        }

        //delete input files
        deleteFileFromPath(new File(saveLocationMatrix.toAbsolutePath().toString()));
        deleteFileFromPath(new File(saveLocationGeneList.toAbsolutePath().toString()));
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
                return null;
            }
            genesList = new ArrayList<String>();

            String line;
            while ((line = reader.readLine()) != null) {
                if(line.equals("")){
                    continue;
                }
                genesList.add(line);
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

            if(content == null){
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

    private String createAdjMatrixFileName(String dateTimeJoined){
        return "adjMatrix_"+dateTimeJoined+".csv";
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
    }*/
}
