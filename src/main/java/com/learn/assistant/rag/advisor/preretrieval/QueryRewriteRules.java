package com.learn.assistant.rag.advisor.preretrieval;

import com.learn.assistant.properties.RagProperties;

final class QueryRewriteRules {

  private QueryRewriteRules() {}

  static boolean longEnough(String text, RagProperties properties) {
    return text != null && text.length() > properties.getRetrieval().getRewriteMinLength();
  }

  static boolean containsLatinLetter(String text) {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
        return true;
      }
    }
    return false;
  }
}
