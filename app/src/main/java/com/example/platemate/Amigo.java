package com.example.platemate;

public class Amigo {
    private int id;
    private String nombre;
    private String idFirestore;

    public Amigo(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdFirestore() { return idFirestore; }
    public void setIdFirestore(String idFirestore) { this.idFirestore = idFirestore; }

    @Override
    public String toString() {
        return this.nombre;
    }
}

