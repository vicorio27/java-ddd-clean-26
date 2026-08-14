package com.sandbox.application.port;

import java.util.function.Supplier;

/**
 * Frontera transaccional expuesta como puerto.
 *
 * <p>Los casos de uso necesitan atomicidad, pero la capa de aplicacion no debe conocer
 * Spring (lo verifica {@code applicationLayerDoesNotDependOnSpring}). En vez de anotar
 * los casos de uso con {@code @Transactional}, la transaccion entra por este puerto y
 * la infraestructura decide como implementarla.
 *
 * <p>Antes cada adaptador de repositorio abria su propia transaccion, asi que
 * "guardar la orden" y "guardar el pago" hacian commit por separado.
 */
public interface UnitOfWork {

    <T> T execute(Supplier<T> work);

    default void execute(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
