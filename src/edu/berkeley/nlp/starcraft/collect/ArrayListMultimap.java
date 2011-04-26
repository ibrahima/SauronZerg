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

public class ArrayListMultimap<K,V> implements Multimap<K, V> {


	private Map<K, ArrayList<V>> map = new HashMap<K,ArrayList<V>>();
	private int size = 0;
	
	@Override
  public boolean containsKey(K k) {
	  return map.containsKey(k);
  }

	@Override
  public Collection<Entry<K, V>> entries() {
	  return new AbstractCollection<Entry<K,V>>() {

			@Override
      public boolean add(Entry<K, V> arg0) {
				return put(arg0.getKey(), arg0.getValue());
	    }

			@Override
      public boolean addAll(Collection<? extends Entry<K, V>> arg0) {
				boolean any = false;
	      for(Entry<K,V> e: arg0) {
	      	any |= add(e);
	      }
	      return any;
      }

			@Override
      public void clear() {
	      ArrayListMultimap.this.clear();
      }

			@SuppressWarnings("unchecked")
      @Override
      public boolean contains(Object arg0) {
	      if(arg0 instanceof Map.Entry<?,?>) {
	      	Map.Entry<K,V> kv = (Entry<K,V>)arg0;
	      	return ArrayListMultimap.this.contains(kv.getKey(),kv.getValue());
	      }
	      return false;
      }

			@Override
      public boolean containsAll(Collection<?> arg0) {
	      for(Object o: arg0) {
	      	if(!contains(o)) return false;
	      }
	      return true;
      }

			@Override
      public boolean isEmpty() {
	      return ArrayListMultimap.this.isEmpty();
      }

			@Override
      public Iterator<Entry<K, V>> iterator() {
	      return new Iterator<Entry<K,V>>() {
	      	Iterator<Map.Entry<K,ArrayList<V>>> outer = map.entrySet().iterator();
	      	Iterator<V> inner = null;
	      	K k;
	      	V v;
	      	
	      	public Map.Entry<K, V> next() {
	      		refresh();
	      		Map.Entry<K, V> kv = new AbstractMap.SimpleEntry<K, V>(k, v);
	      		
	      		v = null;
	      		return kv;
	      	}
	      	
	      	public boolean hasNext() {
	      		refresh();
	      		return v != null;
	      	}
	      	
	      	void refresh() {
	      		if(v == null) {
	      			while(inner == null || !inner.hasNext()) {
	      				if(!outer.hasNext()) return;
	      			  Map.Entry<K, ArrayList<V>> e = outer.next();
	      			  k = e.getKey();
	      			  inner = e.getValue().iterator(); 
	      			}
	      			v = inner.next();
	      		}
	      	}

					@Override
          public void remove() {
	          throw new UnsupportedOperationException("No");
	          
          }
	      	
	      	
	      };
      }

			@SuppressWarnings("unchecked")
      @Override
      public boolean remove(Object arg0) {
	      if(arg0 instanceof Map.Entry<?,?>) {
	      	Map.Entry<K,V> kv = (Entry<K,V>)arg0;
	      	return ArrayListMultimap.this.remove(kv.getKey(),kv.getValue());
	      }
	      return false;
      }


			@Override
      public int size() {
	      return size;
      }
	  	
	  };
  }

	@Override
  public ArrayList<V> get(K k) {
      ArrayList<V> ret = map.get(k);
      if(ret == null)return new ArrayList<V>();
	  return map.get(k);
  }

	@Override
  public boolean isEmpty() {
	  return size == 0;
  }

	@Override
  public Set<K> keySet() {
	  return map.keySet();
  }

	@Override
  public boolean put(K k, V v) {
	  if(ensure(k).add(v)) {
	  	size += 1;
	  	return true;
	  }
	  return false;
  }

	@Override
  public boolean putAll(K k, Iterator<? extends V> vs) {
	  ArrayList<V> l = ensure(k);
	  
	  boolean any = false;
	  while(vs.hasNext()) {
	  	boolean added = l.add(vs.next());
	  	if(added) size += 1;
	  	any |= added;
	  }
	  if(!any) {
	  	removeAll(k);
	  }
	  return any;
  }

	private ArrayList<V> ensure(K k) {
	  ArrayList<V> v = map.get(k);
	  if(v == null) {
	  	v = new ArrayList<V>();
	  	map.put(k,v);
	  }
	  return v;
  }

	@Override
  public boolean remove(K k, V v) {
	  if(!containsKey(k)) return false;
	  ArrayList<V> arr = map.get(k);
		if(arr.remove(v)) {
	  	size -= 1;
	  	if(arr.isEmpty()) map.remove(k);
	  	return true;
	  }
	  return false;
  }

	@Override
  public boolean removeAll(K k) {
	  if(!containsKey(k)) return false;
	  ArrayList<V> v = map.remove(k);
	  if(v != null) size -= v.size();
	  return v != null;
  }

	@Override
  public int size() {
	  return size;
  }

	@Override
  public void clear() {
	  map.clear();
	  size = 0;
  }

	@Override
  public boolean contains(K k, V v) {
	  return containsKey(k) && map.get(k).contains(v);
  }
	
	@Override
  public String toString() {
	  return "ArrayListMultimap [map=" + map + "]";
  }
}
