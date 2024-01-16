package org.apache.spark.sql.catalyst.util;

import org.apache.spark.unsafe.types.UTF8String;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import com.ibm.icu.text.Collator;


// TODO: Using singleton for now. See what is the proper pattern here.

public class CollatorFactory {
  private static HashMap<String, Integer> collationNameToId = new HashMap<String, Integer>(10);
  private static ArrayList<Comparator<UTF8String>> collationComparatorsCache =
    new ArrayList<Comparator<UTF8String>>(10);
  private static ArrayList<Collator> collatorCache = new ArrayList<Collator>(10);

  private static Collator getCollator(String collationName) {
    var collationStrings = collationName.split("-");

    if (collationStrings.length != 2) {
      throw new IllegalArgumentException("Invalid collation name: " + collationName);
    }

    String locale = collationStrings[0];
    var collator = Collator.getInstance(java.util.Locale.forLanguageTag(locale));

    if (collationStrings[1].equalsIgnoreCase("primary")) {
      collator.setStrength(Collator.PRIMARY);
    } else if (collationStrings[1].equalsIgnoreCase("secondary")) {
      collator.setStrength(Collator.SECONDARY);
    } else if (collationStrings[1].equalsIgnoreCase("tertiary")) {
      collator.setStrength(Collator.TERTIARY);
    } else if (collationStrings[1].equalsIgnoreCase("identical")) {
      collator.setStrength(Collator.IDENTICAL);
    } else {
      throw new IllegalArgumentException("Invalid collation strength: " + collationStrings[1]);
    }

    return collator;
  }

  public static Comparator<UTF8String> getComparator(int id) {
    return collationComparatorsCache.get(id - 1);
  }

  public static int getCollationAwareHash(String input, String collation) {
    // TODO: Collator caching...
    return getCollator(collation).getCollationKey(input).hashCode();
  }

  public static int getCollationAwareHash(String input, int collatorId) {
    // TODO: Collator caching...
    return collatorCache.get(collatorId).getCollationKey(input).hashCode();
  }

  public synchronized  static Integer installComparator(String collationName) {
    // TODO: Think about concurrency here.
    // What happens when ArrayList is resized?
    // TODO: Propagate more information about collation (e.g. whether it is binary collation so
    // we can still do binary comparisons.

    if (collationName.isEmpty() || collationName.equals("default")) {
      return 0;
    }

    if (collationNameToId.containsKey(collationName)) {
      return collationNameToId.get(collationName);
    }

//    var collator = getCollator(collationName);
    var col = MyClass.createCollator(collationName);
    var comparator = new Comparator<UTF8String>() {
      @Override
      public int compare(UTF8String o1, UTF8String o2) {
        // TODO: Both Java's collator and ICU's 4j collator work only with UTF16.
        // Hence we do UTF8 -> UTF16 conversion here, which is very expensive.
        // ICU4c has direct UTF8 comparison. Solution is probably to use native library here.

        return MyClass.compare2(col, o1.getBytes(), o2.getBytes());
//        return MyClass.compare(col, o1.toString(), o2.toString());
//        return collator.compare(o1.toString(), o2.toString());
      }
    };

    int id = collationComparatorsCache.size() + 1;
    collationNameToId.put(collationName, id);
    collationComparatorsCache.add(comparator);
//    collatorCache.add(collator);
    return id;
  }
}
