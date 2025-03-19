package org.example.model;

// FileInfo.java
public record FileInfo(
        String name,
        boolean isDirectory,
        String size,
        long modifiedTime
) {}