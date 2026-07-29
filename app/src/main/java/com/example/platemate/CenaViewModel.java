package com.example.platemate;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
public class CenaViewModel extends ViewModel {
    // Guardamos la ID de la sesión actual de la cena
    private int sesionId = -1;
    // Guardamos la ID del restaurante seleccionado
    private int restauranteId = -1;

    // Usamos LiveData para las listas. Así, si giran la pantalla, Android sabe qué había pintado
    private MutableLiveData<List<String>> amigosEnMesa = new MutableLiveData<>(new ArrayList<>());

    public int getSesionId() { return sesionId; }
    public void setSesionId(int sesionId) { this.sesionId = sesionId; }

    public int getRestauranteId() { return restauranteId; }
    public void setRestauranteId(int restauranteId) { this.restauranteId = restauranteId; }

    public MutableLiveData<List<String>> getAmigosEnMesa() { return amigosEnMesa; }

    public void agregarAmigoAMesa(String nombreAmigo) {
        List<String> listaActual = amigosEnMesa.getValue();
        if (listaActual != null && !listaActual.contains(nombreAmigo)) {
            listaActual.add(nombreAmigo);
            amigosEnMesa.setValue(listaActual); // Esto notifica a la interfaz
        }
    }
}
