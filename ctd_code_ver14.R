#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)
require(igraph)
require(CTD)
require(jsonlite)
require(optparse)

#Add options 



option_list = list(
  make_option(c("-g", "--graph_type"), action="store", default="NULL", type='character',
              help="graph type"),
  
  make_option(c("-i", "--input_gene_list"), action="store", default="NULL", type='character',
              help="input gene set"),
  
  make_option(c("-a", "--adjacency_matrix"), action="store", default="NULL",type='character',
              help="user input adjacency"),
  
  make_option(c("-p", "--permutation_matrix"), action="store", default="NULL",type='character',
              help="user input ctd perms ")
  
)


args = parse_args(OptionParser(option_list=option_list),print_help_and_exit = FALSE)


#User can use their own files or string
keggRanksFile = "precalculatedMatrices/kegg_ranks.RData"
keggLargeGraphFile = "precalculatedMatrices/kegg_large_graph.csv"
stringRanksFileAps = "precalculatedMatrices/string_ranks.RData"
stringLowerGraphFile = "precalculatedMatrices/string_med_conf_lower.csv"
wikipathwayRanksFileAps = "precalculatedMatrices/wikipathway_ranks.RData"
wikipathwayLowerGraphFile = "precalculatedMatrices/wikipathway_large_graph.csv"
outputFilesFolder = "outputFiles/"


ctd_connect = function(gene_set, perms, G_graph) {
  if (is.null(gene_set)) {
    stop("gene_set is NULL. Please provide a valid gene set.")
  }
  
  if (is.null(perms)) {
    stop("perms is NULL. Please provide a valid permutation data.")
  }
  
  if (is.null(G_graph)) {
    stop("G_graph is NULL. Please provide a valid graph data.")
  }
  
  if (!is.vector(gene_set)) {
    stop("gene_set must be a vector.")
  }
  
  if (!is.list(perms)) {
    stop("perms must be a list.")
  }
  
  if (!is.igraph(G_graph)) {
    stop("G_graph must be an igraph object.")
  }
  
  output = list()
  gene_set = intersect(gene_set, names(V(G_graph)))

  if (length(gene_set) >= 10) {
    genes_in = gene_set
    test = mle.getPtBSbyK(S = gene_set, ranks = perms, num.misses = 1000)
    result = mle.getEncodingLength(test, NULL, "set1", G_graph)
    result$pvalue = (2^-(result$d.score - log2(length(G_graph))))
    result = result[result$pvalue < 0.05, ]
    result$num_miss = lengths(regmatches(result$optimalBS, gregexpr("0", result$optimalBS)))
    
    if (nrow(result) >= 1) {
      for (x in 1:nrow(result)) {
        subset_size = result[x, "subsetSize"]
        result$genes[[x]] = list(names(test[[subset_size]]))
      }
      
      result$distinct = NA
      result$guilty_by_association = NA
      
      for (x in 1:nrow(result)) {
        genes_x = unique(unlist(result$genes[[x]]))
        other_genes = unique(unlist(result[-x, "genes"]))
        ifelse(length(setdiff(genes_x, other_genes)) > 0, result[x, "distinct"] <- "yes", result[x, "distinct"] <- "no")
        new_genes = list(setdiff(genes_x, genes_in))
        result$guilty_by_association[x] <- new_genes
      }
      
      result = result[result$distinct == "yes", ]
    } else {
      stop("No significantly connected results found. Result is NA.")
    }
  } else {
    result = NA
    print("Not enough genes to run CTD")
  }
  
  return(result)
}

setUpPathsToResources = function(){
print(paste("Working Directory: ", getwd()))
keggRanksFile = paste0(getwd(),"/",keggRanksFile) 
print(keggRanksFile)
keggLargeGraphFile =  paste0(getwd(),"/",keggLargeGraphFile)
print(keggLargeGraphFile)
stringRanksFileAps =  paste0(getwd(),"/",stringRanksFileAps)
print(stringRanksFileAps)
stringLowerGraphFile =  paste0(getwd(),"/",stringLowerGraphFile)
print(stringLowerGraphFile)
wikipathwayRanksFileAps =  paste0(getwd(),"/",wikipathwayRanksFileAps)
print(wikipathwayRanksFileAps)
wikipathwayLowerGraphFile =  paste0(getwd(),"/",wikipathwayLowerGraphFile)
print(wikipathwayLowerGraphFile)
outputFilesFolder =  paste0(getwd(),"/",outputFilesFolder)
print(outputFilesFolder)
}

output_ctd = function(gene_set,ctd_result,G_graph){
  if(length(ctd_result$genes)>0){
  highly_connected = intersect(unlist(ctd_result$genes),gene_set)
  guilty_association = setdiff(unlist(ctd_result$genes),gene_set)
  both = c(highly_connected,guilty_association)
  graph_json = induced.subgraph(G_graph,c(highly_connected,guilty_association))
  graph_json = simplify(graph_json)
  V(graph_json)$label = names(V(graph_json))
  nodes = data.frame(cbind(name = V(graph_json)$name,type = "gene"))
  nodes_json = toJSON(nodes)
  interactions = data.frame(as_edgelist(graph_json))
  colnames(interactions) = c("source","target")
  interactions_json = toJSON(interactions)
  list_json = list(fromJSON(nodes_json),fromJSON(interactions_json))
  names(list_json) = c("nodes","interactions")
  graph_json = toJSON(list_json, pretty = TRUE)
  out = list(highly_connected = highly_connected,guilty_association = guilty_association,graph_json = graph_json)}
  else{
    out = list(highly_connected = "No significantly connected sets",guilty_association = "No significantly connected sets",graph_json = "No significantly connected sets")}
  return(out)
}

ctd_wrapper_kegg = function(graph_type,genes_in_file_path,adj_file,perms_user_in_file){
  
  setUpPathsToResources()
  genes = read.csv(genes_in_file_path,header = FALSE)
  genes = genes[,1]
  genes = unlist(genes)
  genes = tolower(genes)
  
  if(graph_type == "kegg"){
	load(keggRanksFile)
	names(perm) = tolower(names(perm))
	perm = lapply(perm, function(i) as.character(sapply(i, tolower)))
	perms = perm
	perm = NULL
	adj = read.csv(keggLargeGraphFile)
	rownames(adj) = adj$X
	adj$X = NULL
	adj_matrix = as.matrix(adj)
	ig = graph_from_adjacency_matrix(adj_matrix,weighted = TRUE)
	adjacency_matrix =  adj_matrix
	gene_set = intersect(genes,names(perms))
	ctd_result = ctd_connect(gene_set,perms,ig)
	output_ctd_kegg  =  output_ctd(gene_set,ctd_result,ig)
  }
  
  if(graph_type == "string"){
    load(stringRanksFileAps)
    
    perm = permutationByStartNode
    
    names(perm) = tolower(names(perm))
    perm = lapply(perm, function(i) as.character(sapply(i, tolower)))
    perms = perm
    perm = NULL
    adj = read.csv(stringLowerGraphFile)
    rownames(adj) = adj$X
    adj$X = NULL
    adj_matrix = as.matrix(adj)
    ig = graph_from_adjacency_matrix(adj_matrix,weighted = TRUE)
    adjacency_matrix =  adj_matrix
    
    gene_set = intersect(genes,names(perms))
    
    ctd_result = ctd_connect(gene_set,perms,ig)
    print(ctd_result)
    output_ctd_kegg  =  output_ctd(gene_set,ctd_result,ig)
  }
  
  
  if(graph_type == "wikipathway"){
    load(wikipathwayRanksFileAps)
    
    perm = permutationByStartNode
    
    names(perm) = tolower(names(perm))
    perm = lapply(perm, function(i) as.character(sapply(i, tolower)))
    perms = perm
    perm = NULL
    adj = read.csv(wikipathwayLowerGraphFile)
    rownames(adj) = adj$X
    adj$X = NULL
    adj_matrix = as.matrix(adj)
    ig = graph_from_adjacency_matrix(adj_matrix,weighted = TRUE)
    adjacency_matrix =  adj_matrix
    
    gene_set = intersect(genes,names(perms))
    
    ctd_result = ctd_connect(gene_set,perms,ig)
    print(ctd_result)
    output_ctd_kegg  =  output_ctd(gene_set,ctd_result,ig)
  }
  
  
  
  
  if(graph_type == "user_input"){
	load(perms_user_in_file)
  perm = permutationByStartNode
	names(perm) = tolower(names(perm))
	perm = lapply(perm, function(i) as.character(sapply(i, tolower)))
	perms = perm
	perm = NULL
	permutationByStartNode = NULL

	
	adj = read.csv(adj_file)
	rownames(adj) = adj$X
	adj$X = NULL
	rownames(adj) = tolower(rownames(adj))
	colnames(adj) = tolower(colnames(adj))

	adj_matrix = as.matrix(adj)
    ig = graph_from_adjacency_matrix(adj_matrix,weighted = TRUE)
	adjacency_matrix =  adj_matrix
	
	gene_set = intersect(genes,names(perms))
	ctd_result = ctd_connect(gene_set,perms,ig)
	print(ctd_result)
	
	output_ctd_kegg  =  output_ctd(gene_set,ctd_result,ig)
  }
  return(output_ctd_kegg)
}

output_file_writer = function(dataToWrite, baseFileName, fileTypeStr){
  fileName = paste(outputFilesFolder, toString(baseFileName), fileTypeStr, sep = "", collapse=NULL)
  write(dataToWrite, file = fileName)
}

start_processing = function(args){
  
  graph = as.character(args$g)
  genes_in_file_path = args$i
  adj_mat_user = args$a
  perms_user = args$p

  list_all = c(graph,genes_in_file_path,adj_mat_user,perms_user)
  
  input = as.vector(unlist(list_all))

  outF = ctd_wrapper_kegg(as.character(graph),as.character(genes_in_file_path),as.character(adj_mat_user),as.character(perms_user))
  
  baseFileName = tools::file_path_sans_ext(basename(genes_in))
  
  highly_connected = outF$highly_connected
  output_file_writer(highly_connected, baseFileName, "_highly.csv")
 
  guilty_association = outF$guilty_association
  output_file_writer(guilty_association, baseFileName, "_guilty.csv")
  
  graph_json = outF$graph_json
  output_file_writer(graph_json, baseFileName, "_graph.json")
}



if (length(args) <2 & length(args) > 5) {
  stop("Error: There must be between two and four input parameters!", call.=FALSE)
} else if (length(args) >=2 & length(args) <= 5) {
  graph = args$g
  genes_in = args$i
  adj_mat_user = args$a
  perms_user = args$p

    start_processing(args)
}

