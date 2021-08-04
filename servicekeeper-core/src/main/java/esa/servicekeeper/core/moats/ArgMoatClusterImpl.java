package esa.servicekeeper.core.moats;

import java.util.List;

public class ArgMoatClusterImpl extends MoatClusterImpl implements ArgMoatCluster {
    public ArgMoatClusterImpl(List<Moat<?>> moats, List<MoatClusterListener> listeners) {
        super(moats, listeners);
    }
}
