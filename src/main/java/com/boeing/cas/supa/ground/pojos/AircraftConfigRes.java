package com.boeing.cas.supa.ground.pojos;

import java.io.*;
import java.util.zip.ZipOutputStream;

public class AircraftConfigRes {
    String checkSum;
    File aircraftProperty;
    ZipOutputStream tsp;

    public AircraftConfigRes(String checkSum, File property, ZipOutputStream tsp){
        this.checkSum = checkSum;
        this.aircraftProperty = property;
        this.tsp = tsp;
    }

    public String getCheckSum(){
        return this.checkSum;
    }

    public File getAircraftProperty(){
        return this.aircraftProperty;
    }

    public ZipOutputStream getTSP(){
        return this.tsp;
    }
}
