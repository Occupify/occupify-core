package com.occupify.core.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class H2Functions {

  private static final Pattern DIACRITICS_PATTERN =
      Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private H2Functions() {}

  public static String unaccent(String str) {
    if (str == null) {
      return null;
    }
    String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
    String result = DIACRITICS_PATTERN.matcher(nfd).replaceAll("");
    return result.replace('đ', 'd').replace('Đ', 'D');
  }
}
