package com.cfde.playbook_ctd.services;

import com.cfde.playbook_ctd.model.ResponseFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;

public interface CTDService {
    ResponseFormat mainCTDList(String graphType, ArrayList<String> geneList);
    ResponseFormat mainCTDFile(String graphType, MultipartFile csvGenesFile);
    ResponseEntity<Resource> createCustomAdjacency(MultipartFile geneExpressionsCSV);
    ResponseEntity<Resource> getCustomPermutations(MultipartFile adjacencyJSON, MultipartFile geneList);
    ResponseFormat useCustomMatrix(MultipartFile geneExpressionsCSV, MultipartFile csvGenesFile, MultipartFile jsonPermutationsFile);
}
