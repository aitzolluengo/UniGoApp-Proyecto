package com.tzolas.unigoapp.model;

public class ParadaCercana {
    public String stopId;
    public String nombre;
    public double distancia;

    public ParadaCercana(String stopId, String nombre, double distancia) {
        this.stopId = stopId;
        this.nombre = nombre;
        this.distancia = distancia;
    }
}
