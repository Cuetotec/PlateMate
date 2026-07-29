package com.example.platemate;

import java.util.ArrayList;
import java.util.List;

public class Restaurante {
    private int id;
    private String nombre;
    private List<Plato> platos;

    public Restaurante(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.platos = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<Plato> getPlatos() { return platos; }
    public void setPlatos(List<Plato> platos) { this.platos = platos; }

    @Override
    public String toString() {
        return nombre;
    }
}
