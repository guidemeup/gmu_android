package org.gmu.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ttg
 * Date: 2/01/13
 * Time: 15:10
 * To change this template use File | Settings | File Templates.
 */
public class DelegationHashMap<K, V> extends HashMap<K, V> {
    private Map<K, V> delegation = null;

    public void setDelegation(Map<K, V> delegation) {
        this.delegation = delegation;
    }

    public V get(Object key) {
        V ret = super.get(key);
        if (ret != null) return ret;
        if (delegation != null) {
            return delegation.get(key);
        }
        return null;
    }
}
