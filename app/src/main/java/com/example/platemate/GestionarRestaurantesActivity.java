package com.example.platemate;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
public class GestionarRestaurantesActivity extends AppCompatActivity {
    private int idRestaurante;
    private LocalDataManager localData;
    private List<Restaurante> listaRestaurantes;
    private Restaurante restauranteActual;

    private Button btnVolver, btnAnadirAMesa;
    private Spinner spinnerCategorias, spinnerPlatos;
    private ListView listViewPlatos;
    private ArrayAdapter<Plato> adapterPlatos;
    private List<Plato> listaPlatosMostrar = EstadoGlobal.platosEnMesa;
    private List<Consumo> listaConsumosGestion = EstadoGlobal.consumos;

    private List<Plato> todosPlatos = new ArrayList<>();
    private ArrayAdapter<String> adapterCategorias;
    private ArrayAdapter<Plato> adapterPlatosSpinner;

    private final androidx.activity.result.ActivityResultLauncher<String> csvPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) importarCSV(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_restaurantes);

        idRestaurante = getIntent().getIntExtra("id_res", -1);
        Log.d("DEBUG_APP", "ID Restaurante recibido: " + idRestaurante);

        if (idRestaurante == -1) {
            Toast.makeText(this, "Error: Restaurante no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // INICIALIZAMOS LAS VISTAS
        listViewPlatos = findViewById(R.id.listViewPlatos);
        btnVolver = findViewById(R.id.btnVolver);
        btnAnadirAMesa = findViewById(R.id.btnAnadirAMesa);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        spinnerPlatos = findViewById(R.id.spinnerPlatos);

        btnVolver.setOnClickListener(v -> finish());

        Spinner spinnerAmigosMesa = findViewById(R.id.spinnerAmigosMesa);
        ArrayAdapter<Amigo> adapterAmigosMesa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, EstadoGlobal.amigosEnMesa);
        adapterAmigosMesa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAmigosMesa.setAdapter(adapterAmigosMesa);

        btnAnadirAMesa.setOnClickListener(v -> {
            Plato seleccionado = (Plato) spinnerPlatos.getSelectedItem();
            Amigo amigoSel = (Amigo) spinnerAmigosMesa.getSelectedItem();
            if (seleccionado != null && amigoSel != null) {
                listaPlatosMostrar.add(seleccionado);
                listaConsumosGestion.add(new Consumo(seleccionado, amigoSel.getNombre()));
                adapterPlatos.notifyDataSetChanged();
                Toast.makeText(this, seleccionado.getNombre() + " -> " + amigoSel.getNombre(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Selecciona un plato y un amigo", Toast.LENGTH_SHORT).show();
            }
        });

        adapterCategorias = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapterCategorias);

        adapterPlatosSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        adapterPlatosSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlatos.setAdapter(adapterPlatosSpinner);

        adapterPlatos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaPlatosMostrar);
        listViewPlatos.setAdapter(adapterPlatos);

        listViewPlatos.setOnItemLongClickListener((parent, view, position, id) -> {
            Plato plato = listaPlatosMostrar.get(position);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Eliminar plato")
                    .setMessage("¿Deseas eliminar este plato de la mesa?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        listaPlatosMostrar.remove(position);
                        if (position < listaConsumosGestion.size()) listaConsumosGestion.remove(position);
                        adapterPlatos.notifyDataSetChanged();
                        Toast.makeText(this, plato.getNombre() + " eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        listViewPlatos.setOnItemClickListener((parent, view, position, id) -> {
            Plato plato = listaPlatosMostrar.get(position);
            mostrarDialogoEdicionPlato(plato);
        });

        spinnerCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String cat = (String) parent.getItemAtPosition(position);
                filtrarPlatos(cat);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnAnadirPlato = findViewById(R.id.btnAnadirPlato);
        btnAnadirPlato.setOnClickListener(v -> mostrarDialogoNuevoPlato());

        Button btnImportarCsv = findViewById(R.id.btnImportarCsv);
        btnImportarCsv.setOnClickListener(v -> csvPickerLauncher.launch("text/*"));

        cargarRestauranteYCartaDesdeNube();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    private void filtrarPlatos(String categoria) {
        if (categoria == null || categoria.isEmpty()) return;

        List<Plato> filtrados = new ArrayList<>();
        for (Plato p : todosPlatos) {
            if (p.getCategoria().trim().equals(categoria.trim().toUpperCase())) {
                filtrados.add(p);
            }
        }

        adapterPlatosSpinner.clear();
        for (Plato p : filtrados) adapterPlatosSpinner.add(p);
        adapterPlatosSpinner.notifyDataSetChanged();
    }

    private void cargarRestauranteYCartaDesdeNube() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("platos")
                .whereEqualTo("id_restaurante", (double) idRestaurante)
                .get()
                .addOnSuccessListener(snapshots -> {

                    List<Plato> platosNube = new ArrayList<>();
                    Set<String> categoriasSet = new LinkedHashSet<>();

                    for (DocumentSnapshot doc : snapshots) {
                        String nombre = doc.getString("nombre");
                        Double precio = doc.getDouble("precio");
                        String categoria = doc.getString("categoria");

                        if (nombre == null || precio == null || categoria == null) continue;

                        Plato p = new Plato(platosNube.size() + 1, nombre, precio, categoria.toUpperCase());
                        p.setIdFirestore(doc.getId());
                        platosNube.add(p);
                        categoriasSet.add(categoria.toUpperCase());
                    }

                    List<String> categoriasList = new ArrayList<>(categoriasSet);

                    runOnUiThread(() -> {
                        if (restauranteActual == null) {
                            restauranteActual = new Restaurante(idRestaurante, "Restaurante " + idRestaurante);
                        }

                        // SOLO ACTUALIZAMOS LOS SPINNERS DE SELECCIÓN PARA AÑADIR NUEVOS PLATOS
                        todosPlatos.clear();
                        todosPlatos.addAll(platosNube);

                        adapterCategorias.clear();
                        for (String cat : categoriasList) adapterCategorias.add(cat);
                        adapterCategorias.notifyDataSetChanged();

                        if (!categoriasList.isEmpty()) {
                            spinnerCategorias.setSelection(0);
                            filtrarPlatos(categoriasList.get(0));
                        } else {
                            todosPlatos.clear();
                            adapterPlatosSpinner.clear();
                            adapterPlatosSpinner.notifyDataSetChanged();
                        }

                        Toast.makeText(this, "Carta actualizada (" + platosNube.size() + " platos)", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error al leer carta: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        todosPlatos.clear();
                        adapterPlatosSpinner.clear();
                        adapterPlatosSpinner.notifyDataSetChanged();
                        adapterCategorias.clear();
                        adapterCategorias.notifyDataSetChanged();
                        Toast.makeText(this, "Error al cargar carta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }

    private Plato parseRow(String[] datos, int defaultId) {
        if (datos.length < 3) return null;

        try {
            if (datos.length >= 4) {
                int id = Integer.parseInt(datos[0].trim());
                String nombre = datos[1].trim();
                double precio = Double.parseDouble(datos[2].replace("€", "").trim().replace(",", "."));
                String categoria = datos[3].trim().toUpperCase();
                if (!esNumerico(categoria)) return new Plato(id, nombre, precio, categoria);
            }
        } catch (NumberFormatException ignored) {}

        try {
            String nombre = datos[0].trim();
            double precio = Double.parseDouble(datos[1].replace("€", "").trim().replace(",", "."));
            String categoria = datos[2].trim().toUpperCase();
            if (!esNumerico(categoria)) return new Plato(defaultId, nombre, precio, categoria);
        } catch (NumberFormatException ignored) {}

        try {
            double precio = Double.parseDouble(datos[0].replace("€", "").trim().replace(",", "."));
            String nombre = datos[1].trim();
            String categoria = datos[2].trim().toUpperCase();
            if (!esNumerico(categoria)) return new Plato(defaultId, nombre, precio, categoria);
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private void mostrarDialogoEdicionPlato(Plato plato) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText inputNombre = new EditText(this);
        inputNombre.setHint("Nombre del plato");
        inputNombre.setText(plato.getNombre());

        EditText inputPrecio = new EditText(this);
        inputPrecio.setHint("Precio");
        inputPrecio.setText(String.valueOf(plato.getPrecio()));
        inputPrecio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        layout.addView(inputNombre);
        layout.addView(inputPrecio);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar plato")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoNombre = inputNombre.getText().toString().trim();
                    String precioTexto = inputPrecio.getText().toString().trim().replace(",", ".");

                    if (nuevoNombre.isEmpty() || precioTexto.isEmpty()) {
                        Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double nuevoPrecio = Double.parseDouble(precioTexto);

                        FirebaseFirestore.getInstance()
                                .collection("platos")
                                .document(plato.getIdFirestore())
                                .update("nombre", nuevoNombre, "precio", nuevoPrecio)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FIREBASE_UPDATE", "Plato actualizado correctamente!");
                                    recargarListaPlatos();
                                })
                                .addOnFailureListener(e -> Log.e("FIREBASE_UPDATE", "Error al actualizar", e));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Precio no valido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void recargarListaPlatos() {
        cargarRestauranteYCartaDesdeNube();
    }

    private void importarCSV(android.net.Uri uri) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
                String linea;
                int contador = 0;

                reader.readLine();

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                while ((linea = reader.readLine()) != null) {
                    if (linea.trim().isEmpty()) continue;
                    String[] datos = linea.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (datos.length < 5) continue;

                    try {
                        String nombre = datos[1].trim().replace("\"", "");
                        String precioTexto = datos[2].trim().replace("\"", "").replace("€", "").replace(",", ".");
                        String categoria = datos[3].trim().replace("\"", "").toUpperCase();

                        double precio = Double.parseDouble(precioTexto);

                        String idUnico = idRestaurante + "_" + sanitizarId(nombre);

                        java.util.Map<String, Object> plato = new java.util.HashMap<>();
                        plato.put("nombre", nombre);
                        plato.put("precio", precio);
                        plato.put("categoria", categoria);
                        plato.put("id_restaurante", (double) idRestaurante);

                        db.collection("platos").document(idUnico).set(plato)
                                .addOnSuccessListener(aVoid -> Log.d("CSV_IMPORT", "Importado: " + nombre))
                                .addOnFailureListener(e -> Log.e("CSV_IMPORT", "Error en: " + nombre, e));
                        contador++;
                    } catch (Exception e) {
                        Log.e("CSV_IMPORT", "Fila saltada: " + linea, e);
                    }
                }
                reader.close();

                final int total = contador;
                runOnUiThread(() -> {
                    Toast.makeText(this, total + " platos importados correctamente", Toast.LENGTH_LONG).show();
                    recargarListaPlatos();
                });
            } catch (Exception e) {
                Log.e("CSV_IMPORT", "Error al leer archivo", e);
                runOnUiThread(() -> Toast.makeText(this, "Error al importar CSV", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String sanitizarId(String nombre) {
        String normalizado = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD);
        return normalizado.replaceAll("[^\\p{ASCII}]", "").toLowerCase().replace(" ", "_");
    }

    private void mostrarDialogoNuevoPlato() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText inputNombre = new EditText(this);
        inputNombre.setHint("Nombre del plato");

        EditText inputPrecio = new EditText(this);
        inputPrecio.setHint("Precio");
        inputPrecio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText inputCategoria = new EditText(this);
        inputCategoria.setHint("Categoria (ej: TAPA, BEBIDA)");

        layout.addView(inputNombre);
        layout.addView(inputPrecio);
        layout.addView(inputCategoria);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo plato")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = inputNombre.getText().toString().trim();
                    String precioTexto = inputPrecio.getText().toString().trim().replace(",", ".");
                    String categoria = inputCategoria.getText().toString().trim().toUpperCase();

                    if (nombre.isEmpty() || precioTexto.isEmpty() || categoria.isEmpty()) {
                        Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double precio = Double.parseDouble(precioTexto);

                        java.util.Map<String, Object> nuevoPlato = new java.util.HashMap<>();
                        nuevoPlato.put("nombre", nombre);
                        nuevoPlato.put("precio", precio);
                        nuevoPlato.put("categoria", categoria);
                        nuevoPlato.put("id_restaurante", (double) idRestaurante);

                        FirebaseFirestore.getInstance()
                                .collection("platos")
                                .add(nuevoPlato)
                                .addOnSuccessListener(doc -> {
                                    Log.d("FIREBASE_ADD", "Plato creado con ID: " + doc.getId());
                                    Toast.makeText(this, "Plato anadido correctamente", Toast.LENGTH_SHORT).show();
                                    recargarListaPlatos();
                                })
                                .addOnFailureListener(e -> Log.e("FIREBASE_ADD", "Error al crear plato", e));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Precio no valido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean esNumerico(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
