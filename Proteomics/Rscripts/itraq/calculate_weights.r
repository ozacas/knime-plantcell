########################################################################
# Program to calculate weights based on peptide intensity and variance
# Lina Hultin Rosenberg 20120612 
# lina.hultin-rosenberg@ki.se
########################################################################

#Load getWeight
source("Path to R scripts/getWeight.r")

#Specify folders, full path to data and weights
folder.data = "Path to peptide intensity data folder"
folder.weights = "Path to weights folder"

#Dataset
dataset.name = "test"

#Parameters for weight calculation
channel.1 = "113"   #iTRAQ channel for weight calculation, training data
channel.2 = "114"   #iTRAQ channel for weight calculation, training data
ratio = 1           #Expected ratio between duplicates
bins = 8            #Number of bins for weight calculation

#Load peptide data
filename = paste(folder.data,dataset.name,"_norm_median_quant.txt",sep="")
pep.quant = read.delim(filename,header=TRUE,row.names=1,sep="\t")

##Calculate weight-matrix for 113/114 ratio
  
#Extract iTRAQ channels for weight calculation
index.channel.1 = grep(channel.1,colnames(pep.quant))
index.channel.2 = grep(channel.2,colnames(pep.quant))
pep.quant = pep.quant[,c(index.channel.1,index.channel.2)]
  
#Remove missing data
index.na = c(which(is.na(pep.quant[,1])),which(is.na(pep.quant[,2])))
if (length(index.na)>0) {
  pep.quant.norm = pep.quant.norm[-index.na,]
} 
  
#Calculate variance as deviance from expected ratio (on log2 scale)
estimated.ratio = log2(pep.quant[,index.channel.1]/pep.quant[,index.channel.2])
expected.ratio = log2(ratio)
variance = (estimated.ratio - expected.ratio)^2
  
#Calculate reference intensity as minimum intensity over iTRAQ channels
reference = log2(apply(pep.quant,1,min))
  
#Create variance matrix and sort in ascending order of reference intensity 
variance.matrix = cbind(reference, variance) 
variance.matrix = variance.matrix[order(variance.matrix[,1]),]
  
#Create weight matrix (call function getWeight)
weight.matrix = getWeightMatrix(variance.matrix,bins)
  
#Save variance and weight matrix
filename = paste(folder.weights,dataset.name,"_",channel.1,channel.2,"_variance_matrix.txt",sep="")
write.table(variance.matrix,file=filename,row.names=FALSE,sep="\t")
  
filename = paste(folder.weights,dataset.name,"_",channel.1,channel.2,"_weight_matrix.txt",sep="")
write.table(weight.matrix,file=filename,row.names=FALSE,sep="\t")
      
##Plot ratios versus minimum intensity
filename = paste(folder.weights,dataset.name,"_ratio_plot.tif",sep="")
tiff(file=filename)
par(mar=c(2.5,2.5,0.5,0.5),mgp=c(1.4,0.5,0))
plot(reference,estimated.ratio,main="",xlab="log2 of minimum intensity",ylab="log2 of ratio",pch=20,ylim=c(-2,2))
abline(a=0,b=0)
abline(v=weight.matrix[-nrow(weight.matrix),3],col=grey(0.8))
dev.off()
  
##Plot weights for bins
filename = paste(folder.weights,dataset.name,"_weight_plot.tif",sep="")
tiff(file=filename)
par(mar=c(2.5,2.5,1,0.5),mgp=c(1.4,0.5,0))
barplot(weight.matrix[,4],main="",names.arg="",xlab="Bins",ylab="Weights")
dev.off()
  
  
  
  
