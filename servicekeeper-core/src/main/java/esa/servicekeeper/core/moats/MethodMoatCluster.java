package esa.servicekeeper.core.moats;

import esa.servicekeeper.core.fallback.FallbackHandler;

public interface MethodMoatCluster extends MoatCluster {

    /**
     * get fallbackHandler
     *
     * @return FallbackHandler
     */
    FallbackHandler<?> fallbackHandler();
}
