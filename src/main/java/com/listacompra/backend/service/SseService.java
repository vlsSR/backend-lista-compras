package com.listacompra.backend.service;

import com.listacompra.backend.dto.SseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    // CopyOnWriteArrayList: seguro para modificarse mientras varios hilos leen/escriben a la vez
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // 0L = sin timeout, la conexión se mantiene abierta indefinidamente
        SseEmitter emitter = new SseEmitter(0L);

        emitters.add(emitter);
        log.info("Cliente SSE conectado. Total: {}", emitters.size());

        Runnable removeEmitter = () -> {
            emitters.remove(emitter);
            log.info("Cliente SSE desconectado. Total: {}", emitters.size());
        };

        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError((ex) -> removeEmitter.run());

        return emitter;
    }

    public void broadcast(SseEnvelope event) {
        List<SseEmitter> muertos = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(event));
            } catch (IOException e) {
                // El cliente ya no está ahí (app cerrada, red caída...); lo limpiamos
                muertos.add(emitter);
            }
        }

        emitters.removeAll(muertos);
        log.info("Evento {} enviado a {} clientes", event.getType(), emitters.size());
    }
}
