package com.example.platemate;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LocalDataManager {
    private final File restaurantesFile;
    private final File amigosFile;
    private final Gson gson;

    public LocalDataManager(Context context) {
        File dir = context.getFilesDir();
        this.restaurantesFile = new File(dir, "restaurantes.json");
        this.amigosFile = new File(dir, "amigos.json");
        this.gson = new Gson();
    }

    // --- MÉTODOS PARA RESTAURANTES ---
    public List<Restaurante> cargarRestaurantes() {
        if (!restaurantesFile.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(restaurantesFile)) {
            Type type = new TypeToken<List<Restaurante>>(){}.getType();
            List<Restaurante> lista = gson.fromJson(reader, type);
            return lista != null ? lista : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Clon por si MainActivity busca "obtener"
    public List<Restaurante> obtenerRestaurantes() {
        return cargarRestaurantes();
    }

    public void guardarRestaurantes(List<Restaurante> restaurantes) {
        try (FileWriter writer = new FileWriter(restaurantesFile)) {
            gson.toJson(restaurantes, writer);
        } catch (Exception e) {}
    }


    // --- MÉTODOS PARA AMIGOS ---
    public List<Amigo> cargarAmigos() {
        if (!amigosFile.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(amigosFile)) {
            Type type = new TypeToken<List<Amigo>>(){}.getType();
            List<Amigo> lista = gson.fromJson(reader, type);
            return lista != null ? lista : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Clon por si MainActivity busca "obtener"
    public List<Amigo> obtenerAmigos() {
        return cargarAmigos();
    }

    public void guardarAmigos(List<Amigo> amigos) {
        try (FileWriter writer = new FileWriter(amigosFile)) {
            gson.toJson(amigos, writer);
        } catch (Exception e) {}
    }
}
