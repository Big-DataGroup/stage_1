package org.ulpgc.bigdata.datamarts.storage;

import java.util.List;
import java.util.Map;


public interface IndexStorage {
    void save(Map<String, List<Integer>> index);
}