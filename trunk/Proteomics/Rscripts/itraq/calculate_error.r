########################################################################
# Program to calculate weighted protein quantities and errors for 113/114
# Lina Hultin Rosenberg 20120612 
# lina.hultin-rosenberg@ki.se
########################################################################

library(Hmisc)

#Load getWeight
source("@SCRIPT_PATH@@FILESEP@getWeight.r")

#Specify folders, full path to data and weights
folder.data    = "@DATA_PATH@"
folder.weights = "@WEIGHTS_PATH@"

#Dataset
dataset.name = "test"

#Parameters for calculating weighted quant and errors
channel.1 = "113"       #iTRAQ channel for weight calculation, training data
channel.2 = "114"       #iTRAQ channel for weight calculation, training data
num = "113"             #iTRAQ channel numerator for protein ratios
den = "114"             #iTRAQ channel denominator used to calculate protein ratios
ratio = 1               #Expected ratio between duplicates

#Load peptide data
filename = paste(folder.data,dataset.name,"_norm_median_quant.txt",sep="")
pep.quant = read.delim(filename,header=TRUE,row.names=1,sep="\t")
  
#Load protein accessions
filename = paste(folder.data,dataset.name,"_protein_accession.txt",sep="")
protein.acc = read.delim(filename,header=TRUE,row.names=1,sep="\t")
  
#Get unique proteins
proteins = protein.acc[rownames(pep.quant),]$Protein.Group.Accessions
unique.proteins = unique(proteins)
  
#Remove empty protein accession string
unique.proteins = unique.proteins[unique.proteins!=""]
no.proteins = length(unique.proteins)
  
##Calculate weighted protein quantities for 113/114 ratio
        
#Load weight matrix
filename = paste(folder.weights,dataset.name,"_",channel.1,channel.2,"_weight_matrix.txt",sep="")
weight.matrix = read.delim(filename,header=TRUE,sep="\t")

#Select iTRAQ channels to calculate protein quant for
index.den = grep(den,colnames(pep.quant))
index.num = grep(num,colnames(pep.quant))
quant.num = pep.quant[,index.num]
quant.den = pep.quant[,index.den]

#Calculate weighted protein quant and error for each protein
weight.results = data.frame(ratios=rep(0,no.proteins),protein.weights=rep(0,no.proteins),peptides=rep(0,no.proteins),errors=rep(0,no.proteins),row.names=unique.proteins)
        
for (protein.i in 1:no.proteins) {
  
  quant.num.protein = quant.num[proteins==unique.proteins[protein.i]]
  quant.den.protein = quant.den[proteins==unique.proteins[protein.i]]
          
  #Get weights for peptide ratio, use minimum intensity as reference
  reference = log2(apply(cbind(quant.num.protein,quant.den.protein),1,min,na.rm=TRUE))
  
  #Call function getWeight
  ratio.weights = getWeight(reference,weight.matrix[,3],weight.matrix[,4])
  weight.factor = ratio.weights[,2]
  
  #Calculate peptide ratios and remove empty
  pep.ratio = log2(quant.num.protein/quant.den.protein)
  index.na = which(is.na(pep.ratio))
  if (length(index.na)>0) {
    pep.ratio = pep.ratio[-index.na]
    weight.factor = weight.factor[-index.na]
  } 
          
  #Calculate total protein weight as average over peptide weights
  weight.results$protein.weights[protein.i] = mean(weight.factor)
          
  #Use Hmisc function to calculate weighted average (protein quantity)
  protein.ratio.w = wtd.mean(pep.ratio,weight.factor) 
  
  #Move back from log space to regular ratio
  weight.results$ratios[protein.i] = 2^protein.ratio.w
  
  #Calculate relative error for weighted protein quant
  error = abs(ratio-weight.results$ratios[protein.i])
  weight.results$errors[protein.i] = (error/ratio)*100
          
  #Number of peptides for protein
  weight.results$peptides[protein.i] = length(pep.ratio)
}
      
#Save weight results
filename = paste(folder.weights,dataset.name,"_",channel.1,channel.2,"_weight_results.txt",sep="")
write.table(weight.results,file=filename,col.names=NA,sep="\t")



