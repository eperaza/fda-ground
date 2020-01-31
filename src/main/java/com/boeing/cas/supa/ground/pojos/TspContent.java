package com.boeing.cas.supa.ground.pojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class TspContent {
	@SerializedName("_version")
	public float version;
	
	@SerializedName("_date")
	public String date;
	
	@SerializedName("tail")
	public String tail;
	
	@SerializedName("provideOptimals")
	public Boolean provideOptimals;
	
	@SerializedName("fuelFlowBetas")
	public List<Double> fuelFlowBetas;
	
	@SerializedName("InfltDB")
	public String infltDb;
	
	@SerializedName("InfltConfig")
	public String infltConfig;
	
	@SerializedName("fuelFlowQstar")
	public Double fuelFlowQstar;
	
	@SerializedName("weightBetas")
	public List<Double> weightBetas;
	
	public TspContent() {
		fuelFlowBetas = new ArrayList<>();
		weightBetas = new ArrayList<>();
	}
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("fuelFlowBetas=");
        builder.append(Arrays.toString(fuelFlowBetas.toArray()));
        builder.append("\n");
        builder.append("fuelFlowQstar=");
        builder.append(fuelFlowQstar);
        builder.append("\nweightBetas=");
        builder.append(Arrays.toString(weightBetas.toArray()));
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == null || !(obj instanceof TspContent)) {
    		return false;
    	}
    	
    	TspContent that = (TspContent)obj;
    	return this.tail.equalsIgnoreCase(that.tail)
    			&& this.provideOptimals == that.provideOptimals
    			&& isEqual(this.fuelFlowBetas, that.fuelFlowBetas)
    			&& this.infltConfig.equals(that.infltConfig)
    			&& this.infltDb.equals(that.infltDb)
    			&& this.fuelFlowQstar.equals(that.fuelFlowQstar)
    			&& isEqual(this.weightBetas, that.weightBetas);
    }
    
    private boolean isEqual(List<Double> a1, List<Double> a2) {
    	if (a1.size() != a2.size()) {
    		return false;
        }
        
        for (int i = a1.size() - 1; i >= 0; i--) {
            if (!a1.get(i).equals(a2.get(i))) {
            	return false;
            }
        }
        
        return true;
    }
}
