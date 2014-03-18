#########################################################################################
# Program to generate output figure and table for weighted protein quantities (all ratios)
# Lina Hultin Rosenberg 20120612 
# lina.hultin-rosenberg@ki.se
#########################################################################################

library(Hmisc)

#Load getWeight
source("Path to R scripts/getWeight.r")

#Specify folders, full path to data, weights and results
folder.data = "Path to peptide intensity data folder"
folder.weights = "Path to weights folder"
folder.results =  "Path to output folder"

#Dataset
dataset.name = "test"

#Parameters for calculating weighted quant
channel.1 = "113"       #iTRAQ channel for weight calculation
channel.2 = "114"       #iTRAQ channel for weight calculation
den = "mean(113,114)"   #iTRAQ channel denominator used to calculate protein ratios
den.1 = "113"
den.2 = "114"
num.list = c("113","114","115","116","117","118","119","121") #iTRAQ channel numerators for protein ratios

#Number of peptides specified for plotting
no.peptides.num = c(1,2,3,4,5,5,10)
no.peptides = c("1","2","3","4","5","5-10",">10")

#Load raw data
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

##Calculate weighted protein ratios (all channels)
      
#Load weight matrix
filename = paste(folder.weights,dataset.name,"_",channel.1,channel.2,"_weight_matrix.txt",sep="")
weight.matrix = read.delim(filename,header=TRUE,sep="\t")

protein.weights = matrix(data=NA,nrow=no.proteins,ncol=length(num.list))
protein.ratios = matrix(data=NA,nrow=no.proteins,ncol=length(num.list))
protein.peptides = matrix(data=NA,nrow=no.proteins,ncol=length(num.list))

ratio.list = c()
ratio.i = 1
for (num in num.list) {
  
  ratio.list[ratio.i] = paste(num,den,sep="/")
  
  #Select iTRAQ channels to calculate protein quant for
  index.den.1 = grep(den.1,colnames(pep.quant))
  index.den.2 = grep(den.2,colnames(pep.quant))
  index.num = grep(num,colnames(pep.quant))
  quant.num = pep.quant[,index.num]
  quant.den = apply(cbind(pep.quant[,index.den.1],pep.quant[,index.den.2]),1,mean,na.rm=TRUE)
        
  #Get peptides belonging to each protein
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
    protein.weights[protein.i,ratio.i] = mean(weight.factor)
          
    #Use Hmisc function to calculate weighted average (protein quantity)
    protein.ratio.w = wtd.mean(pep.ratio,weight.factor) 
  
    #Move back from log space to regular ratio
    protein.ratios[protein.i,ratio.i] = 2^protein.ratio.w
          
    #Number of peptides for protein
    protein.peptides[protein.i,ratio.i] = length(pep.ratio)
  }
  ratio.i = ratio.i+1
}
colnames(protein.weights) = ratio.list
rownames(protein.weights) = unique.proteins
colnames(protein.ratios) = ratio.list
rownames(protein.ratios) = unique.proteins
colnames(protein.peptides) = ratio.list
rownames(protein.peptides) = unique.proteins
      
#Load data for 113/114 training set

#Load weight results
filename = paste(folder.weights,dataset.name,"_",channel.1,channel.2,"_weight_results.txt",sep="")
weight.results = read.delim(filename,row.names=1,header=TRUE,sep="\t")

#Get loess function for error versus average weight of proteins (95%) - for 113/114

#Plot error versus average weight of proteins (95% loess smoothers)
filename = paste(folder.results,dataset.name,"_protein_error_weights.tif",sep="")
tiff(file=filename)
par(mar=c(3,3,0.5,0.5),mgp=c(1.5,0.5,0))

#For each number of peptides
protein.weights.ref = c()
protein.errors.ref = c()
loess.list = list()
  
for (i in 1:length(no.peptides)) {
  if (i<6) {
    protein.list = rownames(weight.results[weight.results$peptides==no.peptides.num[i],])
  } else if (i==6) {
    protein.list = rownames(weight.results[weight.results$peptides>5&weight.results$peptides<=10,])
  } else {
    protein.list = rownames(weight.results[weight.results$peptides>10,])
  }
  protein.weights.ref = weight.results[protein.list,]$protein.weights
  protein.errors.ref = weight.results[protein.list,]$errors
 
  #Order the errors in order of the weights
  weights.order = order(protein.weights.ref)
  protein.weights.ref = protein.weights.ref[weights.order]
  protein.errors.ref = protein.errors.ref[weights.order]

  #Calculate 95% upper limit of error
  protein.errors.95 = c()
  if (i==1) {
    weights.unique = unique(protein.weights.ref)
    for (weight.unique in weights.unique) {
      protein.errors.sel = protein.errors.ref[which(protein.weights.ref==weight.unique)]
      protein.errors.95 = c(protein.errors.95,quantile(protein.errors.sel,probs=seq(0,1,0.05),na.rm=TRUE)[20])
    }
    protein.errors.runmed = protein.errors.95
    weight.breaks = weights.unique
  } else if (i==2)  {
    weights.unique = unique(protein.weights.ref)
    for (weight.unique in weights.unique) {
      protein.errors.sel = protein.errors.ref[which(protein.weights.ref==weight.unique)]
      protein.errors.95 = c(protein.errors.95,quantile(protein.errors.sel,probs=seq(0,1,0.05),na.rm=TRUE)[20])
    }
    protein.errors.runmed = runmed(protein.errors.95,31)
    weight.breaks = weights.unique
  } else {
    weight.breaks = quantile(protein.weights.ref,probs=seq(0,1,0.02))
    for (j in 1:(length(weight.breaks)-1)) {
      if (j<length(weight.breaks)-1) {
        error.interval = protein.errors.ref[protein.weights.ref>=weight.breaks[j]&protein.weights.ref<weight.breaks[j+1]]
      } else {
        error.interval = protein.errors.ref[protein.weights.ref>=weight.breaks[j]&protein.weights.ref<=weight.breaks[j+1]]
      }
      protein.errors.95[j] = quantile(error.interval,probs=seq(0,1,0.05),na.rm=TRUE)[20]
    }
    #Fill upp empty values
    index.na = which(is.na(protein.errors.95))
    while (length(index.na)>0) {
      protein.errors.95[index.na] = protein.errors.95[index.na+1]
      index.na = which(is.na(protein.errors.95))
    }
    protein.errors.runmed = runmed(protein.errors.95,31)
    weight.breaks = weight.breaks[-length(weight.breaks)]
  }
  
  #Fit a loess curve to error
  data.errors = data.frame(x=weight.breaks,y=protein.errors.runmed)
  lo.errors = loess(y~x,data.errors,span=0.5,degree=1)
  loess.list[i] = list(lo.errors)
  
  if (i==1) {
    plot(data.errors$x,predict(lo.errors,data.errors$x),xlab="Protein weight",ylab="Relative error (%)",type="l",col=i,ylim=c(0,max(protein.errors.runmed)))  
  } else {
    lines(data.errors$x,predict(lo.errors,data.errors$x),col=i)
  }
  legend("topright",no.peptides,col=c(1:7),lty=1)
}
dev.off()

#Create matrix to save relative error estimated by loess
error.matrix = matrix(NA,nrow=nrow(protein.ratios),ncol=ncol(protein.ratios),byrow=FALSE,dimnames=list(rownames(protein.ratios)))

#Get relative error for all ratios
for (i in 1:nrow(protein.weights)) {
  for (j in 1:ncol(protein.weights)) {
    weight = protein.weights[i,j]
    peptides = protein.peptides[i,j]
    if (peptides<6) {
      peptides.index = peptides
    } else if (peptides>5 & peptides<11) {
      peptides.index = 6
    } else {
      peptides.index = 7
    }
    error.matrix[i,j] = predict(loess.list[[peptides.index]],weight)
    if (is.na(error.matrix[i,j])) {
      if (weight>60) {
        while (is.na(error.matrix[i,j])) {
          weight = weight-1
          error.matrix[i,j] = predict(loess.list[[peptides.index]],weight)
        }
      } else if (weight<30) {
        while (is.na(error.matrix[i,j])) {
          weight = weight+1
          error.matrix[i,j] = predict(loess.list[[peptides.index]],weight)
        }
      } else {
      }
    }
  }
}

#Round error and weight
error.matrix = round(error.matrix)
protein.weights = round(protein.weights)

#Save table
quant.table = cbind(protein.ratios,protein.peptides,protein.weights,error.matrix)
ratios = c("113","114","115","116","117","118","119","121")
column.names = c(ratios,paste(ratios,"no.peptides",sep="."),paste(ratios,"weight",sep="."),paste(ratios,"rel.error",sep=".")) 
colnames(quant.table) = column.names

filename = paste(folder.results,dataset.name,"_protein_quant_table.txt",sep="")
write.table(quant.table,file=filename,col.names=NA,sep="\t")
    
