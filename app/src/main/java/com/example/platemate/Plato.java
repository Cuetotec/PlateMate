package com.example.platemate;

public class Plato {
    private int id;
    private String nombre;
    private double precio;
    private String categoria;
    private String idFirestore;

    public Plato(int id, String nombre, double precio, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public  String getCategoria(){
        return categoria;
    }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(double precio) { this.precio = precio; }
    public String getIdFirestore() { return idFirestore; }
    public void setIdFirestore(String idFirestore) { this.idFirestore = idFirestore; }

    @Override
    public String toString() {
        return this.nombre + " (" + precio + "€)";
    }
}
