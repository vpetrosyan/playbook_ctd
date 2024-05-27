package com.cfde.playbook_ctd.controllers;

import com.cfde.playbook_ctd.model.GeneSet;
import com.cfde.playbook_ctd.model.Report;
import com.cfde.playbook_ctd.model.ResponseFormat;
import com.cfde.playbook_ctd.services.CTDService;
import com.cfde.playbook_ctd.utils.constants.Constants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping(value = "/rest/playbook_ctd")
public class CTDController {
    private static Logger logger = Logger.getLogger(CTDController.class);

    @Value("${graphTypes}")
    private String graphTypes;
    private String[] graphTypesList;

    @Autowired
    private CTDService ctdService;

    @PostConstruct
    private void initialize(){
        logger.info("Supported graph types: "+graphTypes);
        graphTypesList = graphTypes.split(",");
    }

    @RequestMapping(value = "/test", method= RequestMethod.GET)
    public String test() {
        return "OK!";
    }

    @PostMapping(value = "/ctd/geneList",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseFormat mainCTDList(@RequestBody GeneSet geneSet){
        if(geneSet == null || geneSet.getGraphType() == null || !checkSubmittedGraphTypeValue(geneSet.getGraphType()) || geneSet.getGeneList()  == null){
            return new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Request parameters not valid, please check the submitted values!"));
        }

        int gN = geneSet.getGeneList().size();

        if(gN == 0){
            return new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"The submitted gene list is empty!"));
        }

        if(gN < 10 || gN > 150){
            return new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Number of genes submitted is out of allowed parameters, min number is 10, max is 150!"));
        }

        return ctdService.mainCTDList(geneSet.getGraphType(), geneSet.getGeneList());
    }

    @PostMapping(value = "/ctd/file")
    public ResponseFormat mainCTDFile(@RequestParam("graphType") String graphType, @RequestParam("csvGenesFile") MultipartFile csvGenesFile){
        if(graphType == null || !checkSubmittedGraphTypeValue(graphType) || csvGenesFile == null){
            return new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Request parameters not valid, please check the submitted values!!"));
        }
        return ctdService.mainCTDFile(graphType, csvGenesFile);
    }

    @PostMapping(value = "/ctd/createCustomMatrix")
    public ResponseEntity<Resource> createCustomMatrix(@RequestParam("csvExpressionsFile") MultipartFile geneExpressionsCSV){
        if(geneExpressionsCSV == null){
            return null;
        }
        return ctdService.createCustomMatrix(geneExpressionsCSV);
    }

    @PostMapping(value = "/ctd/getCustomPermutations", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getCustomPermutations(@RequestParam("customMatrix") MultipartFile matrix,
                                                          @RequestParam("geneList") MultipartFile geneList){
        if(matrix == null || geneList == null){
            return null;
        }
        return ctdService.getCustomPermutations(matrix, geneList);
    }

    @PostMapping(value = "/ctd/useCustomMatrix")
    public ResponseFormat useCustomMatrix(@RequestParam("customMatrix") MultipartFile matrix,
                                          @RequestParam("csvGenesFile") MultipartFile csvGenesFile,
                                          @RequestParam("customRData") MultipartFile customRData){

        if(matrix == null || csvGenesFile == null || customRData == null){
            return new ResponseFormat(new Report(Constants.REPORT_TYPE_ERROR,"Request parameters not valid!"));
        }
        return ctdService.useCustomMatrix(matrix, csvGenesFile, customRData);
    }

    private boolean checkSubmittedGraphTypeValue(String graphValue){
        if(graphTypesList == null || graphTypesList.length == 0){
            return false;
        }
        for(String gType : graphTypesList){
            if(graphValue.equals(gType)){
                return true;
            }
        }
        return false;
    }
}
