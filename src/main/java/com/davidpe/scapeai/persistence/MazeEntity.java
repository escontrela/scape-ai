package com.davidpe.scapeai.persistence;

public record MazeEntity(Long id, String name, int rows, int cols, String layout) {}
