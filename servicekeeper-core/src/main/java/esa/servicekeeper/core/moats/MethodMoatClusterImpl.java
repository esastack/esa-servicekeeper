package esa.servicekeeper.core.moats;

import esa.servicekeeper.core.fallback.FallbackHandler;

import java.util.List;

public class MethodMoatClusterImpl extends MoatClusterImpl implements MethodMoatCluster {

    private final FallbackHandler<?> fallbackHandler;

    public MethodMoatClusterImpl(List<Moat<?>> moats, List<MoatClusterListener> listeners, FallbackHandler<?> fallbackHandler) {
        super(moats, listeners);
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public FallbackHandler<?> fallbackHandler() {
        return fallbackHandler;
    }
}
