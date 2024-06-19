package com.redex.logisticaReparto.controller;

import com.redex.logisticaReparto.model.*;
import com.redex.logisticaReparto.dto.*;
import com.redex.logisticaReparto.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api")
public class GraspController {
    @Autowired
    AeropuertoService aeropuertoService;
    @Autowired
    ContinenteService continenteService;
    @Autowired
    EnvioService envioService;
    @Autowired
    PaisService paisService;
    @Autowired
    PaqueteService paqueteService;
    @Autowired
    PlanDeVueloService planDeVueloService;

    private Grasp grasp = new Grasp();
    private boolean esPrimeraSimulacion;
    private ZonedDateTime ultimaFechaConsulta;


    @GetMapping("/grasp/iniciar")
    public String iniciarGrasp(){
        ArrayList<Aeropuerto> aeropuertos = aeropuertoService.obtenerTodosAeropuertos();
        ArrayList<Continente> continentes = continenteService.obtenerTodosContinentes();
        ArrayList<Pais> paises = paisService.obtenerTodosPaises();
        grasp.setAeropuertos(aeropuertos);
        grasp.setContinentes(continentes);
        grasp.setPaises(paises);
        this.esPrimeraSimulacion = true;
        return "Se inicio la simulacion";
    }

    //20240529T14:00-05:00
    //20240529T16:00-05:00

    @PostMapping("grasp/ejecucionDiaria/cargarEnvio")
    public ArrayList<Envio> cargarEnviosDiaria(@RequestBody Map<String, String> datos){

            long startTime = System.currentTimeMillis();
            ArrayList<Pais> paises = paisService.obtenerTodosPaises();
            ArrayList<Envio> envios = new ArrayList<>();
            String enviosDatos = datos.get("data");
            String[] lineas = enviosDatos.split("\n");

            for (String linea : lineas) {
                int i= 0;
                String data[] = linea.split("-");
                if (data.length > 1) {
                    Optional<Aeropuerto> aeropuertoOptionalOrig = aeropuertoService.obtenerAeropuertoPorCodigo(data[0]);
                    //CDES:QQ
                    String dataCdes[] = data[4].split(":");
                    Optional<Aeropuerto> aeropuertoOptionalDest = aeropuertoService.obtenerAeropuertoPorCodigo(dataCdes[0]);

                    if (aeropuertoOptionalOrig.isPresent() && aeropuertoOptionalDest.isPresent()) {
                        Aeropuerto aeropuertoOrigen = aeropuertoOptionalOrig.get();
                        Aeropuerto aeropuertoDest = aeropuertoOptionalDest.get();

                        int ciudadOrigen = aeropuertoOrigen.getId_aeropuerto();
                        long numero_envio_Aeropuerto = Long.parseLong(data[1]); // cambiar a numero_envio_Aeropuerto
                        int ciudadDestino = aeropuertoDest.getId_aeropuerto();
                        int numPaquetes = Integer.parseInt(dataCdes[1]);

                        String husoCiudadOrigen = aeropuertoOrigen.getHuso_horario();
                        String husoCiudadDestino = aeropuertoDest.getHuso_horario();

                        //FORI y HORI
                        int anho = Integer.parseInt(data[2].substring(0,4));
                        int mes = Integer.parseInt(data[2].substring(4,6));
                        int dia = Integer.parseInt(data[2].substring(6,8));

                        String tiempoHM[] = data[3].split(":");
                        int hora = Integer.parseInt(tiempoHM[0]);
                        int minutos = Integer.parseInt(tiempoHM[1]);

                        LocalDateTime tiempoOrigen = LocalDateTime .of(LocalDate.of(anho,mes,dia), LocalTime.of(hora,minutos,0));
                        int numDias = envioService.tipoVuelo(ciudadOrigen, ciudadDestino, paises);
                        LocalDateTime tiempoMax = tiempoOrigen.plusDays(numDias); //ya que en el juego de datos aun no hay del mismo pais xd ni habra :v
                        //
                        Envio newEnvio = new Envio(0,numero_envio_Aeropuerto,tiempoOrigen,ciudadOrigen,
                                ciudadDestino,tiempoMax,numPaquetes,husoCiudadOrigen,husoCiudadDestino);
                        envios.add(newEnvio);
                    }

                }
                System.out.println(i);
                i++;
            }

            ArrayList<Paquete> paquetes = new ArrayList<>();

            for (Envio envio : envios) {
                ArrayList<Paquete> paquetesEnvio = new ArrayList<>();
                for (int j = 0; j < envio.getNumPaquetes(); j++) {
                    Paquete paquete = new Paquete(0);
                    paquete.setEnvio(envio);
                    paquetesEnvio.add(paquete);
                    paquetes.add(paquete);
                }
                envio.setPaquetes(paquetesEnvio);
            }

            grasp.setEnvios(envios);

            return envios;
    }



    @GetMapping("grasp/ejecutar/{fechaHora}")
    public ArrayList<Envio> ejecutarGrasp(@PathVariable String fechaHora){
        long startTime = System.currentTimeMillis();
        int anio = Integer.parseInt(fechaHora.substring(0, 4));
        int mes = Integer.parseInt(fechaHora.substring(4, 6));
        int dia = Integer.parseInt(fechaHora.substring(6, 8));
        int hora = Integer.parseInt(fechaHora.substring(9, 11));
        int minutos = Integer.parseInt(fechaHora.substring(12, 14));
        String husoHorarioStr = fechaHora.substring(15);

        ZonedDateTime fechaInicio = ZonedDateTime.of(anio, mes, dia, hora, minutos, 0, 0, ZoneId.of(husoHorarioStr));
        ZonedDateTime fechaFin = fechaInicio.plusHours(2);
        LocalDateTime fechaInicioLocal = fechaInicio.toLocalDateTime();
        LocalDateTime fechaFinLocal = fechaFin.toLocalDateTime();

        //Busqueda de envios en el rango de 2 horas
        ArrayList<Envio> enviosEnRango = envioService.obtenerEnviosPorFecha(fechaInicioLocal, husoHorarioStr, fechaFinLocal);
        //Busqueda de planes en el rango de 12 horas (por ahora) -> se actualizo a 24 horas
        ArrayList<PlanDeVuelo> planesEnRango;

        if (esPrimeraSimulacion) {
            planesEnRango = planDeVueloService.obtenerPlanesVuelosPorFecha(fechaInicioLocal, husoHorarioStr, fechaFin.plusHours(24).toLocalDateTime());
            grasp.setPlanes(planesEnRango);
            esPrimeraSimulacion = false;
            ultimaFechaConsulta = fechaFin.plusHours(24);
        } else {

            grasp.getPlanes().removeIf(plan -> plan.getZonedHora_origen().isBefore(fechaInicio));

            planesEnRango = planDeVueloService.obtenerPlanesVuelosPorFecha(ultimaFechaConsulta.toLocalDateTime(), husoHorarioStr,
                    ultimaFechaConsulta.plusHours(2).toLocalDateTime());
            grasp.getPlanes().addAll(planesEnRango);
            ultimaFechaConsulta = ultimaFechaConsulta.plusHours(2);
        }

        //450
        grasp.getEnvios().addAll(enviosEnRango);
        System.out.println(grasp.getEnvios().size());
        ArrayList<Envio> solucion = grasp.ejecutaGrasp(grasp.getAeropuertos(),grasp.getEnvios(),grasp.getPlanes());

        ArrayList<Envio> enviosSinRuta = grasp.buscarSinRuta(solucion);
        grasp.setEnvios(enviosSinRuta);

        long endTime = System.currentTimeMillis();
        long durationInMillis = endTime - startTime;
        double durationInSeconds = durationInMillis / 1000.0;
        System.out.println("Tiempo de ejecución: " + durationInSeconds + " segundos");
        return solucion;
    }



}
