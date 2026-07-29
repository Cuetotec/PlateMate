package com.example.platemate;

public class Consumo {
    private Plato plato;
    private String amigoNombre;
    private int cantidad;

    public Consumo(Plato plato, String amigoNombre) {
        this(plato, amigoNombre, 1);
    }

    public Consumo(Plato plato, String amigoNombre, int cantidad) {
        this.plato = plato;
        this.amigoNombre = amigoNombre;
        this.cantidad = cantidad;
    }

    public Plato getPlato() { return plato; }
    public String getAmigoNombre() { return amigoNombre; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
