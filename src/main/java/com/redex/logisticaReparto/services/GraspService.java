package com.redex.logisticaReparto.services;


import com.redex.logisticaReparto.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

@Service
public class GraspService {
    @Autowired
    private PlanDeVueloService PlanDeVueloService;
    ArrayList<Aeropuerto> aeropuertos;
    ArrayList<Pais> paises;
    ArrayList<Continente> continentes;
    ArrayList<Envio> envios;
    ArrayList<PlanDeVuelo> planes;

    ArrayList<Envio> solucionSimulacion;
    @Autowired
    private PlanDeVueloService planDeVueloService;

}


