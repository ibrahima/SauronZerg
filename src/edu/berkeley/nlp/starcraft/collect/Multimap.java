//Multimap.java:


package edu.berkeley.nlp.starcraft.collect;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public interface Multimap<K, V> {

  public boolean containsKey(K k);

  public Collection<Entry<K, V>> entries();

  public ArrayList<V> get(K k);

  public boolean isEmpty();


  public Set<K> keySet();


  public boolean put(K k, V v);

  public boolean putAll(K k, Iterator<? extends V> vs);

  public boolean remove(K k, V v);

  public boolean removeAll(K k);

  public int size();

  public void clear();

  public boolean contains(K k, V v);

  public String toString();
}
