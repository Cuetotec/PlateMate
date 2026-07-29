package com.example.platemate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private LocalDataManager localData;
    private Spinner spinnerRestaurantes, spinnerAmigos;
    private Button btnNuevoRestauranteBD, btnFotoTicket, btnSentarAmigo, btnNuevoAmigoBD, btnIrGestion, btnLoteTicket;
    private TableLayout tableMatrizCuentas;
    private List<Amigo> listaAmigos = new ArrayList<>();
    private List<Restaurante> listaRestaurantes = new ArrayList<>();
    private CenaViewModel cenaViewModel;
    private List<Plato> platosPedidosEnMesa = new ArrayList<>();
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Uri fotoTicketUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INICIALIZAR VISTAS
        spinnerRestaurantes = findViewById(R.id.spinnerRestaurantes);
        spinnerAmigos = findViewById(R.id.spinnerAmigos);
        btnNuevoRestauranteBD = findViewById(R.id.btnNuevoRestauranteBD);
        btnFotoTicket = findViewById(R.id.btnFotoTicket);
        btnSentarAmigo = findViewById(R.id.btnSentarAmigo);
        btnNuevoAmigoBD = findViewById(R.id.btnNuevoAmigoBD);
        btnIrGestion = findViewById(R.id.btnIrGestion);
        btnIrGestion.setEnabled(false);
        btnLoteTicket = findViewById(R.id.btnLoteTicket);
        tableMatrizCuentas = findViewById(R.id.tableMatrizCuentas);

        cenaViewModel = new ViewModelProvider(this).get(CenaViewModel.class);
        localData = new LocalDataManager(this);

        // NAVEGACIÓN
        btnIrGestion.setOnClickListener(v -> {
            Log.d("NAV", "Botón pulsado");
            Restaurante sel = (Restaurante) spinnerRestaurantes.getSelectedItem();
            Log.d("NAV", "sel = " + (sel != null ? sel.getNombre() : "null"));
            if (sel != null) {
                Intent i = new Intent(this, GestionarRestaurantesActivity.class);
                i.putExtra("id_res", sel.getId());
                try {
                    startActivity(i);
                } catch (Exception e) {
                    Log.e("NAV", "Error al navegar", e);
                    Toast.makeText(this, "Error al abrir gestión", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Selecciona un restaurante primero", Toast.LENGTH_SHORT).show();
            }
        });

        // INICIALIZAR PERMISO DE CÁMARA
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        });

        // CÁMARA (LANZAR FOTO)
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                Toast.makeText(this, "Analizando ticket...", Toast.LENGTH_SHORT).show();

                try {
                    com.google.mlkit.vision.common.InputImage image =
                            com.google.mlkit.vision.common.InputImage.fromFilePath(this, fotoTicketUri);

                    com.google.mlkit.vision.text.TextRecognizer recognizer =
                            com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);

                    recognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                List<String> posiblesPlatosDesglosados = new ArrayList<>();

                                // RECOGEMOS, FILTRAMOS Y EXTRAEMOS PRECIOS DE LAS LINEAS DEL TICKET
                                for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
                                    for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                                        String textoLinea = line.getText().trim();
                                        String textoUpper = textoLinea.toUpperCase();

                                        // FILTROS DE EXCLUSIÓN (BASURA, FECHAS, TOTALES)
                                        if (textoUpper.contains("FACTURA") || textoUpper.contains("TEL") ||
                                                textoUpper.contains("TOTAL") || textoUpper.contains("IVA") ||
                                                textoUpper.contains("S.L") || textoUpper.contains("S.A") ||
                                                textoUpper.contains("FECHA") || textoUpper.contains("/") ||
                                                textoLinea.length() < 3) {
                                            continue;
                                        }

                                        // EXTRAER PRECIO REAL DE LA LÍNEA
                                        double precioExtraido = 4.00; // Precio por defecto si no lo encuentra
                                        String nombrePlatoLimpio = textoLinea;

                                        // EXPRESIÓN REGULAR PARA BUSCAR UN PRECIO AL FINAL DE LA LINEA
                                        java.util.regex.Pattern patternPrecio = java.util.regex.Pattern.compile("(\\d+[,.]\\d{2})\\s*€?$");
                                        java.util.regex.Matcher matcherPrecio = patternPrecio.compile("(\\d+[,.]\\d{2})\\s*€?$").matcher(textoLinea);

                                        // BUSCAMOS EL PRECIO EN CUALQUIER PARTE DE LA LINEA SI ESTÁ AL FINAL
                                        String[] palabras = textoLinea.split("\\s+");
                                        if (palabras.length > 1) {
                                            String ultimaPalabra = palabras[palabras.length - 1].replace("€", "").replace(",", ".");
                                            try {
                                                precioExtraido = Double.parseDouble(ultimaPalabra);
                                                // SI LA ÚLTIMA PALABRA ERA UN PRECIO, SE LO QUITAMOS AL NOMBRE DEL PLATO
                                                nombrePlatoLimpio = textoLinea.substring(0, textoLinea.lastIndexOf(palabras[palabras.length - 1])).trim();
                                            } catch (NumberFormatException e) {
                                                // NO ERA UN NÚMERO, SE QUEDA EL PRECIO POR DEFECTO
                                            }
                                        }

                                        // LÓGICA DE DESGLOCE DE CANTIDADES
                                        int cantidad = 1;
                                        if (nombrePlatoLimpio.matches("^\\d+\\s+.*")) {
                                            try {
                                                String[] partes = nombrePlatoLimpio.split("\\s+", 2);
                                                cantidad = Integer.parseInt(partes[0]);
                                                nombrePlatoLimpio = partes[1];
                                            } catch (Exception e) {
                                                cantidad = 1;
                                            }
                                        }

                                        // AÑADIMOS EL PLATO A LA LISTA CON SU PRECIO REAL EXTRAÍDO
                                        for (int i = 0; i < cantidad; i++) {
                                            Plato platoTicket = new Plato(
                                                    EstadoGlobal.platosEnMesa.size() + 1,
                                                    nombrePlatoLimpio,
                                                    precioExtraido,
                                                    "TICKET"
                                            );
                                            if (!posiblesPlatosDesglosados.contains(nombrePlatoLimpio)) {

                                            }
                                        }
                                    }
                                }

                                if (posiblesPlatosDesglosados.isEmpty()) {
                                    Toast.makeText(this, "No se han detectado elementos válidos en el ticket", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // PREPARAMOS EL DIALOGO DE SELECCIÓN MÚLTIPLE CON LA LISTA YA DESGLOSADA
                                CharSequence[] itemsArray = posiblesPlatosDesglosados.toArray(new CharSequence[0]);
                                boolean[] checkedItems = new boolean[posiblesPlatosDesglosados.size()];
                                java.util.Arrays.fill(checkedItems, true); // Marcados por defecto para comodidad

                                // MOSTRAMOS LA VENTANA EMERGENTE PARA QUE EL USUARIO ELIJA
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Revisar Platos Detectados")
                                        .setMultiChoiceItems(itemsArray, checkedItems, (dialog, which, isChecked) -> {
                                            checkedItems[which] = isChecked;
                                        })
                                        .setPositiveButton("Añadir a la Mesa", (dialog, which) -> {
                                            int count = 0;
                                            for (int i = 0; i < posiblesPlatosDesglosados.size(); i++) {
                                                if (checkedItems[i]) {
                                                    String nombrePlato = posiblesPlatosDesglosados.get(i);

                                                    // CREAMO Y AÑADIMOS EL PLATO DEFINITIVO A LA MESA
                                                    Plato platoTicket = new Plato(
                                                            EstadoGlobal.platosEnMesa.size() + 1,
                                                            nombrePlato,
                                                            4.00, // Precio unitario orientativo
                                                            "TICKET"
                                                    );
                                                    EstadoGlobal.platosEnMesa.add(platoTicket);
                                                    count++;
                                                }
                                            }

                                            // REFRESAMOS LA MATRIZ VISUALMENTE
                                            dibujarMatrizInteractiva();
                                            Toast.makeText(this, "¡Se han añadido " + count + " platos a la mesa!", Toast.LENGTH_LONG).show();
                                        })
                                        .setNegativeButton("Cancelar", null)
                                        .show();

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al leer el texto del ticket", Toast.LENGTH_SHORT).show();
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnFotoTicket.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnLoteTicket.setOnClickListener(v -> mostrarDialogoLoteTickets());

        // BOTONES AÑADIR
        btnNuevoRestauranteBD.setOnClickListener(v -> mostrarDialogoNuevoRestaurante());
        btnNuevoAmigoBD.setOnClickListener(v -> mostrarDialogoNuevoAmigo());

        // BOTON SENTAR AMIGO
        btnSentarAmigo.setOnClickListener(v -> {
            Amigo seleccionado = (Amigo) spinnerAmigos.getSelectedItem();
            if (seleccionado != null) {
                if (!EstadoGlobal.amigosEnMesa.contains(seleccionado)) {
                    EstadoGlobal.amigosEnMesa.add(seleccionado);
                    if (cenaViewModel != null) cenaViewModel.agregarAmigoAMesa(seleccionado.getNombre());
                    dibujarMatrizInteractiva();
                } else {
                    Toast.makeText(this, "Ya está sentado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cargarRestaurantesDesdeFirestore();
        cargarAmigosDesdeFirestore();

        // MODIFICAR RESTAURANTE (PULSACIÓN LARGA EN EL SPINNER)
        spinnerRestaurantes.setOnLongClickListener(v -> {
            Restaurante seleccionado = (Restaurante) spinnerRestaurantes.getSelectedItem();
            if (seleccionado != null) {
                final EditText input = new EditText(this);
                input.setText(seleccionado.getNombre());
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Modificar Establecimiento")
                        .setView(input)
                        .setPositiveButton("Actualizar", (dialog, which) -> {
                            String nuevoNombre = input.getText().toString().trim();
                            if (!nuevoNombre.isEmpty()) {
                                FirebaseFirestore.getInstance().collection("restaurantes")
                                        .document(String.valueOf(seleccionado.getId()))
                                        .update("nombre", nuevoNombre)
                                        .addOnSuccessListener(aVoid -> cargarRestaurantesDesdeFirestore());
                            }
                        }).setNegativeButton("Cancelar", null).show();
                return true;
            }
            return false;
        });

        // MODIFICAR AMIGO (PULSACIÓN LARGA)
        spinnerAmigos.setOnLongClickListener(v -> {
            Amigo seleccionado = (Amigo) spinnerAmigos.getSelectedItem();
            if (seleccionado != null) {
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 20, 50, 20);
                final EditText input = new EditText(this);
                input.setText(seleccionado.getNombre());
                layout.addView(input);

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Gestionar Amigo: " + seleccionado.getNombre())
                        .setView(layout)
                        .setPositiveButton("Guardar", (d, w) -> {
                            String nuevoNombre = input.getText().toString().trim();
                            if (!nuevoNombre.isEmpty()) {
                                FirebaseFirestore.getInstance().collection("amigos")
                                        .document(seleccionado.getIdFirestore())
                                        .update("nombre", nuevoNombre)
                                        .addOnSuccessListener(aVoid -> cargarAmigosDesdeFirestore());
                            }
                        })
                        .setNeutralButton("Eliminar", (d, w) -> {

                            // VENTANA DE AVISO ADICIONAL
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Confirmar eliminación")
                                    .setMessage("¿Estás seguro de que quieres borrar a " + seleccionado.getNombre() + "? Esta acción no se puede deshacer.")
                                    .setPositiveButton("Sí, borrar", (confirmD, confirmW) -> {
                                        FirebaseFirestore.getInstance().collection("amigos")
                                                .document(seleccionado.getIdFirestore())
                                                .delete()
                                                .addOnSuccessListener(aVoid -> cargarAmigosDesdeFirestore());
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            }
            return false;
        });
    }

    private void abrirCamara() {
        String fileName = "ticket_" + System.currentTimeMillis() + ".jpg";
        File fotoFile = new File(getCacheDir(), fileName);
        fotoTicketUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", fotoFile);
        cameraLauncher.launch(fotoTicketUri);
    }

    private void cargarRestaurantesDesdeFirestore() {
        FirebaseFirestore.getInstance().collection("restaurantes").get()
                .addOnSuccessListener(snapshots -> {
                    listaRestaurantes.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        String nombre = doc.getString("nombre");
                        String idStr = doc.getId().replaceAll("\\D+", "");
                        if (nombre != null && !idStr.isEmpty()) {
                            listaRestaurantes.add(new Restaurante(Integer.parseInt(idStr), nombre));
                        }
                    }
                    ArrayAdapter<Restaurante> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listaRestaurantes);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRestaurantes.setAdapter(adapter);
                    btnIrGestion.setEnabled(!listaRestaurantes.isEmpty());
                });
    }



    private void cargarAmigosDesdeFirestore() {
        FirebaseFirestore.getInstance().collection("amigos").get()
                .addOnSuccessListener(snapshots -> {
                    listaAmigos.clear();
                    int idx = 1;
                    for (DocumentSnapshot doc : snapshots) {
                        String nombre = doc.getString("nombre");
                        if (nombre != null) {
                            Amigo a = new Amigo(idx++, nombre);
                            a.setIdFirestore(doc.getId());
                            listaAmigos.add(a);
                        }
                    }
                    ArrayAdapter<Amigo> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listaAmigos);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAmigos.setAdapter(adapter);
                });
    }

    private void mostrarDialogoNuevoRestaurante() {
        EditText input = new EditText(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo Restaurante").setView(input)
                .setPositiveButton("Guardar", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if(!n.isEmpty()) agregarRestauranteAFirestore(n);
                }).setNegativeButton("Cancelar", null).show();
    }

    private void mostrarDialogoNuevoAmigo() {
        EditText input = new EditText(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo Amigo").setView(input)
                .setPositiveButton("Guardar", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if(!n.isEmpty()) agregarAmigoAFirestore(n);
                }).setNegativeButton("Cancelar", null).show();
    }

    private void agregarRestauranteAFirestore(String nombre) {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        FirebaseFirestore.getInstance().collection("restaurantes").add(data)
                .addOnSuccessListener(v -> cargarRestaurantesDesdeFirestore());
    }

    private void agregarAmigoAFirestore(String nombre) {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        FirebaseFirestore.getInstance().collection("amigos").add(data)
                .addOnSuccessListener(v -> cargarAmigosDesdeFirestore());
    }

    // MÉTODOS DE DIBUJO
    @Override
    protected void onResume() {
        super.onResume();
        dibujarMatrizInteractiva();
    }

    private void dibujarMatrizInteractiva() {
        if (tableMatrizCuentas == null) return;
        tableMatrizCuentas.removeAllViews();

        if (EstadoGlobal.amigosEnMesa.isEmpty()) return;

        List<Plato> platos = new ArrayList<>(EstadoGlobal.platosEnMesa);
        List<Amigo> amigos = new ArrayList<>(EstadoGlobal.amigosEnMesa);

        // CABECERA
        TableRow header = new TableRow(this);
        header.addView(crearCelda("PLATOS", true));
        for (Amigo a : amigos) {
            header.addView(crearCelda(a.getNombre(), true));
        }
        tableMatrizCuentas.addView(header);

        // FILAS (SI HAY PLATOS)
        if (!platos.isEmpty()) {
            for (Plato plato : platos) {
                TableRow row = new TableRow(this);
                row.addView(crearCelda(plato.getNombre() + "\n(" + String.format("%.2f€", plato.getPrecio()) + ")", false));

                for (Amigo amigo : amigos) {
                    CheckBox cb = new CheckBox(this);
                    boolean checked = EstadoGlobal.consumos.stream()
                            .anyMatch(c -> c.getAmigoNombre().equals(amigo.getNombre()) && c.getPlato().equals(plato));

                    cb.setChecked(checked);
                    cb.setOnCheckedChangeListener((v, isChecked) -> {
                        if (isChecked) EstadoGlobal.consumos.add(new Consumo(plato, amigo.getNombre()));
                        else EstadoGlobal.consumos.removeIf(c -> c.getAmigoNombre().equals(amigo.getNombre()) && c.getPlato().equals(plato));
                        dibujarMatrizInteractiva();
                    });
                    row.addView(cb);
                }
                tableMatrizCuentas.addView(row);
            }
        } else {
            // SI NO HAY PLATAS, MOSTRAMOS UN MENSAJE O FILA VACÍA
            TableRow row = new TableRow(this);
            row.addView(crearCelda("Añade platos en 'Gestionar Carta'", false));
            tableMatrizCuentas.addView(row);
        }

        // FILA TOTALES (SOLO SI HAY PLATOS)
        if (!platos.isEmpty()) {
            TableRow footer = new TableRow(this);
            // VARIABLE PARA ACUMULAR EL TOTAL GENERAL DE LA MESA
            double totalGeneralMesa = 0;

            // CALCULAMOS LOS TOTALES DE TODOS LOS AMIGOS Y ACUMULAMOS EL GLOBAL
            List<String> textosTotalesAmigos = new ArrayList<>();
            for (Amigo amigo : amigos) {
                double totalAmigo = 0;
                for (Plato plato : platos) {
                    long comensales = EstadoGlobal.consumos.stream().filter(c -> c.getPlato().equals(plato)).count();
                    boolean marcado = EstadoGlobal.consumos.stream().anyMatch(c -> c.getAmigoNombre().equals(amigo.getNombre()) && c.getPlato().equals(plato));
                    if (marcado && comensales > 0) totalAmigo += (plato.getPrecio() / comensales);
                }
                // SUMAMOS AL TOTAL GLOBAL DE LA MESA
                totalGeneralMesa += totalAmigo;
                textosTotalesAmigos.add(String.format("%.2f€", totalAmigo));
            }

            // AÑADIMOS LA CELDA INICIAL CON LA ETIQUETAY EL TOTAL DE LA MESA
            footer.addView(crearCelda(String.format("TOTAL: %.2f€", totalGeneralMesa), true));

            // AÑADIMOS LAS CELDAS CON EL TOTAL INDIVIDUALDE CADA AMIGO
            for (String textoAmigo : textosTotalesAmigos) {
                footer.addView(crearCelda(textoAmigo, true));
            }
            tableMatrizCuentas.addView(footer);
        }
    }
    private void mostrarDialogoLoteTickets() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // INSTRUCCIONES BREVES PARA EL USUARIO
        TextView instrucciones = new TextView(this);
        instrucciones.setText("Escribe un plato por línea (Nombre - Precio).\nEjemplo:\nSalmorejo - 4.50\nCopa Cerveza - 2.00");
        instrucciones.setPadding(0, 0, 0, 20);
        layout.addView(instrucciones);

        // CAJA DE TEXTO GRANDE MULTILINEA
        final EditText inputLote = new EditText(this);
        inputLote.setHint("Salmorejo - 4.50\nCopa Cerveza - 2.00");
        inputLote.setMinLines(6);
        inputLote.setGravity(Gravity.TOP | Gravity.START);
        layout.addView(inputLote);

        // VENTANA EMERGENTE
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Añadir Ticket por Lote")
                .setView(layout)
                .setPositiveButton("Guardar Todo", (dialog, which) -> {
                    String textoCompleto = inputLote.getText().toString().trim();
                    if (textoCompleto.isEmpty()) return;

                    // SEPARAMOS EL TEXTO POR SALTOS DE LÍNEA
                    String[] lineas = textoCompleto.split("\n");
                    int anadidos = 0;

                    for (String linea : lineas) {
                        try {
                            // BUSCAMOS SI LA LÍNEA TIENE UN GUION PARA SEPARAR NOBRE Y PRECIO
                            if (linea.contains("-")) {
                                String[] partes = linea.split("-");
                                String nombre = partes[0].trim();
                                String precioStr = partes[1].trim().replace("€", "").replace(",", ".");

                                double precio = Double.parseDouble(precioStr);

                                if (!nombre.isEmpty()) {
                                    Plato nuevoPlato = new Plato(
                                            EstadoGlobal.platosEnMesa.size() + 1,
                                            nombre,
                                            precio,
                                            "LOTE"
                                    );
                                    EstadoGlobal.platosEnMesa.add(nuevoPlato);
                                    anadidos++;
                                }
                            }
                        } catch (Exception e) {
                            // SI ALGUNA LÍNEA ESTÁ MAL ESCRITA, LA IGNORA PARA QUE NO ROMPA LA APP
                            Log.e("LOTE", "Error al procesar línea: " + linea);
                        }
                    }

                    // REFRESCAMOS LA MATRIZ INTERACTIVA
                    dibujarMatrizInteractiva();
                    Toast.makeText(this, "¡Se han añadido " + anadidos + " platos a la mesa!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private TextView crearCelda(String texto, boolean negrita) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(10, 8, 10, 8);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);
        if (negrita) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackgroundColor(Color.parseColor("#D6D6D6"));
        } else {
            tv.setBackgroundColor(Color.parseColor("#FAFAFA"));
        }
        return tv;
    }
}