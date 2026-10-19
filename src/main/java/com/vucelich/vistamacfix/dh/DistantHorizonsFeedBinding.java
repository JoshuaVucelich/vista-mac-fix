package com.vucelich.vistamacfix.dh;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDeferredRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.vucelich.vistamacfix.VistaFeedGuard;

/**
 * Loaded only after Distant Horizons is confirmed present. Touching this
 * class loads {@code DhApi}. DH 3.0.3 has no cancelable fade-pass event.
 * Fade drawing is an internal shader, and {@code DhApiBeforeRenderPassEvent}
 * cannot be cancelled.
 */
public final class DistantHorizonsFeedBinding {
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean BOUND = new AtomicBoolean();

    private DistantHorizonsFeedBinding() {
    }

    public static void bind() {
        if (!BOUND.compareAndSet(false, true)) {
            return;
        }
        DhApi.events.bind(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent() {
            @Override
            public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> param) {
                if (VistaFeedGuard.hidesLods()) {
                    param.cancelEvent();
                }
            }
        });
        DhApi.events.bind(DhApiBeforeDeferredRenderEvent.class, new DhApiBeforeDeferredRenderEvent() {
            @Override
            public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> param) {
                if (VistaFeedGuard.hidesLods()) {
                    param.cancelEvent();
                }
            }
        });
        LOGGER.info(
                "Vista Mac Fix: bound DhApiBeforeRenderEvent and DhApiBeforeDeferredRenderEvent (DH {})",
                DhApi.getModVersion()
        );
    }
}
