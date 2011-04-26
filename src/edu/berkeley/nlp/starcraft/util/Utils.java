package edu.berkeley.nlp.starcraft.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


public class Utils {

  public static <T> T popFirstFromSet(Collection<T> set, Filter<T> filter) {
    T found = null;
    for (T t : set) {
      if (filter.test(t)) {
        found = t;
        break;
      }
    }
    if (found != null) set.remove(found);
    return found;
  }

  public static <T> T findFirst(Collection<T> set, Filter<T> filter) {
    for (T t : set) {
      if (filter.test(t)) {
        return t;
      }
    }
    return null;
  }

  public static <T> List<T> findAll(Collection<? extends T> set, Filter<? super T> filter) {
    List<T> l = new ArrayList<T>();
    for (T t : set) {
      if (filter.test(t)) l.add(t);
    }
    return l;
  }

  public static <T> int countIf(Collection<T> set, Filter<T> filter) {
    int i = 0;
    for (T t : set) {
      if (filter.test(t)) i++;
    }
    return i;
  }

  public static void copyFileHard(String from, String to) {
    try {
      copyFile(from, to);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void copyFile(String from, String to) throws IOException {
    FileChannel inChannel = null, outChannel = null;
    inChannel = new FileInputStream(new File(from)).getChannel();
    outChannel = new FileOutputStream(new File(to)).getChannel();
    inChannel.transferTo(0, inChannel.size(), outChannel);
    if (inChannel != null) inChannel.close();
    if (outChannel != null) outChannel.close();
  }

  public static Properties loadPropertiesFile(String propertiesFile) {
    Properties props = new Properties();
    try {
      FileInputStream in = new FileInputStream(propertiesFile);
      props.load(in);
      in.close();
      return props;
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T getObjectByClassName(String className) {
    try {
      return (T)Class.forName(className).getConstructor().newInstance();
    } catch (Exception e) {
      Log.getLog("Util").fatal("Instantiation:", e);
      throw new RuntimeException("Failed to instantiate: " + className,e);
    } 

  }

  public static Class<?> getClassByClassName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    throw new RuntimeException("Failed to load: " + className);
  }

  @SuppressWarnings("unchecked")
  public static Object getEnumValue(Class enumClass, String valueName) {
    try {
      return enumClass.getMethod("valueOf", String.class).invoke(enumClass, valueName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static <T> T randomChoice(Collection<? extends T> col) {
  	int n = (int) (Math.random() * col.size());
  	Iterator<? extends T> it = col.iterator();
  	while(n-- != 0) it.next();
  	return it.next();
  }

}
