package com.learn.assistant.rag.vectorstore;

import java.util.Collection;
import java.util.Set;

public interface StoredChunkLookup {

  Set<String> findPresent(Collection<String> contents);
}
