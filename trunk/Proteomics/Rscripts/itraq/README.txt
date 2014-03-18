Basic instructions for running R scripts calculate_weights.r, calculate_error.r, generate_output.r


- calculate_weights.r (needs subfunction getWeightMatrix.r) 

Main program to calculate weight matrix for training data (duplicate samples in iTRAQ channels 113/114).

1 Load peptide intensity data and protein accession table for training data 
2 Calculate variance for each peptide ratio, variance = (log2(estimated.ratio)-log2(expected.ratio))^2
3 Calculate intensity reference for each peptide, reference = log2(min(intensity))
4 Create variance matrix with reference and variance as columns
5 Create weight matrix by calling function getWeightMatrix
5.1 getWeightmatrix takes variance matrix and number of bins as input. Ouputs a weight matrix with number of peptides in each bin, 
lower and upper intensity limit for bin and weight for bin  
6 Save weight and variance matrix
7 Plot ratios versus minimum intensity, plot weights for bins


- calculate_error.r (needs subfunction getWeightMatrix.r and R library Hmisc) 

Main program to calculate weighted protein ratios and relative error for training data

1 Load weight matrix, peptide intensity data and protein accession table for training data
2 For each protein, calculate intensity reference for peptides belonging to protein
3 Use intensity reference of peptides to look up weight in weight matrix, by calling function getWeight
3.1 getWeight takes reference intensity and weight matrix as input, outputs reference intensity and corresponding weight
4 Calculate weighted protein ratio as weighted mean over peptides
5 Calculate total protein weight as mean over peptide weights
6 Calculate relative error of protein ratio, error = abs(expected.ratio-estimated.ratio)/expected.ratio * 100
7 Save table with weighted protein ratios, protein weights, number of peptides, relative error for each protein (error table)


- generate_output.r (needs subfunction getWeightMatrix.r and R library Hmisc)

Main program to calculate weighted protein ratios and estimate relative error for all ratios  

1 Load weight matrix and error table for training data
2 Load peptide intensity data and protein accession table for all ratios
3 For each protein and ratio, calculate intensity reference for peptides belonging to protein 
4 Use intensity reference of peptides to look up weight in weight matrix, by calling function getWeight
5 Calculate weighted protein ratio as weighted mean over peptides
6 Calculate total protein weight as mean over peptide weights
7 Plot loess smoother of relative error versus protein weight for proteins with different number of peptides (training data)
8 Use protein weights and loess function to predict relative error for all protein ratios
9 Save table of weighted protein ratios, protein weights, number of peptides and estimated relative error