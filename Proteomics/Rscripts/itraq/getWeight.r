########################################################################################
# Functions for calculating and getting weights
#
# Adopted from:
# Program       : ITRQ.R
# Author        : Getiria Onsongo
# Date          : 7.2.09
#
# Copyright (C) 2009 Getiria Onsongo
#
# Modified by:
# Lina Hultin Rosenberg 120612
# lina.hultin-rosenberg@ki.se
#########################################################################################


# Function getWeightMatrix
# This function takes a matrix (mat) of the form [x,variance] where x is a reference
# intensity value e.g x = min(113, 114). Variance is how much a ratio is differing from
# the expected ratio. This program takes a bin size, starting from the lowest reference
# value (x), gets the mean variance (var) of the bin and assigns this bin a weight of 1/var.
getWeightMatrix = function(mat,bins){
  rows = length(mat[,1])
  size_bins = floor(rows/bins)
  sum_x = 0; sum_v = 0; x_points = c(); v_points = c(); v_points_median = c(); w_points = c(); w_points_median = c(); x_low = c(); x_high = c(); v_values = c()
  counter = 1; bins_filled = 0; bins_minus_one = bins - 1;
  num_points = 1; num_points_vector = c();
  for(i in 1:rows){
    if((i %% size_bins == 0) && (bins_filled < bins_minus_one)){
      sum_x = sum_x + mat[i,1]
      sum_v = sum_v + mat[i,2]
      x = (sum_x/num_points)
      v = (sum_v/num_points)  #The mean variance of the bin
      bin_weight = 1/v        #Weight of the bin (mean)
      v_points[counter] = v
      w_points[counter] = bin_weight
      x_high[counter] = mat[i,1];
      v_points_median[counter] = median(v_values)       #The median variance of the bin
      w_points_median[counter] = 1/(median(v_values))   #Weight of the bin (median)
      num_points_vector[counter] = num_points
		  sum_x = 0; sum_v = 0     #Update
		  bins_filled = bins_filled + 1
		  counter = counter + 1
		  num_points = 1
		  v_values = c()
    }else{
      if (num_points==1) {
        x_low[counter] = mat[i,1]
      }
      if (bins_filled==bins_minus_one) {
        x_high[counter] = mat[i,1]
        num_points_vector[counter] = num_points
      }
      sum_x = sum_x + mat[i,1]
      sum_v = sum_v + mat[i,2]
		  num_points = num_points + 1
		  v_values = c(v_values,mat[i,2])
    }
  }

  #Fill the last bin which may contain up to (2*bins_size - 1) points
  x = (sum_x/(num_points-1))
  v = (sum_v/(num_points-1))    #The mean variance of the bin
  bin_weight <- 1/v             #Weight of the bin (mean)
  v_points[counter] = v
  w_points[counter] = bin_weight
  v_points_median[counter] = median(v_values)     #The median variance of the bin
  w_points_median[counter] = 1/(median(v_values)) #The weight of the bin (median)
  
  #If any of the points had expected value == actual value, the variance will be 0. If by some chance all the points
  #in one bin end up having all points with variance == 0, the weight will be 1/0 = Inf. The steps below change
  #this value from Inf to the next biggest value
  w_points[w_points == Inf] <- max(w_points[w_points != Inf])
  w_points_median[w_points_median == Inf] <- max(w_points_median[w_points_median != Inf])
  
  #Scale weight to have max weight = 100
  w_points = (w_points/max(w_points))*100
  w_points_median = (w_points_median/max(w_points_median))*100
  
  #Return for each bin: number of data points, the lower and upper intensity limits of the bin and the weight
  #NOTE: All x values after the last x, get the value of the last bin
  ans = cbind(num_points_vector, x_low, x_high, w_points_median)

  return(ans)
}

# Function getWeight
# This function takes an array of reference intensity values (weight_ref) and a matrix
# of the upper limit and weight of bins (weight_up,weight_value). For each reference value,
# it returns its weight. The (weight_up,weight_value) tells you if a reference value (weight_ref[i])
# has a value below weight_up[i], then it has a weight of weight_value[i]. It returns a
# (weight_ref,weight_value) pair giving a respective weight for each weight_ref.
getWeight = function(weight_ref, weight_up, weight_value){
  ref_len = length(weight_ref)
  weight_len = length(weight_up)
  val_1 = rep(0,ref_len)
  val_2 = rep(0,ref_len)

  for(i in 1:ref_len){
    if(weight_ref[i] >= weight_up[weight_len]){
      val_1[i] = weight_ref[i]
      val_2[i] = weight_value[weight_len]
    }else{
      pos_val = min(weight_up[weight_ref[i] <= weight_up])
      val_1[i] = weight_ref[i]
      val_2[i] = weight_value[weight_up == pos_val]
    }
  }
  return(cbind(val_1, val_2))
}
