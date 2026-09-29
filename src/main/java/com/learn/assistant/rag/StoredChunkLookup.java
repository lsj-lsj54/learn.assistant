package com.learn.assistant.rag;

import java.util.Collection;
import java.util.Set;

interface StoredChunkLookup {

    Set<String> findPresent(Collection<String> contents);
}
