# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JGeom is a multi-module Maven project providing 2D and 3D computational geometry libraries for Java (targeting Java 21). It consists of two core modules (`geom2d`, `geom3d`) and numerous extension modules for specialized operations.

## Build Commands

```bash
# Build all modules
mvn clean install

# Build and test all modules
mvn clean verify

# Run tests for a specific module
mvn test -pl geom2d

# Run a single test class
mvn test -pl geom2d -Dtest=Point2DTest

# Skip tests during build
mvn clean install -DskipTests

# Build a specific module and its dependencies
mvn clean install -pl geom3d -am
```

Version is managed automatically by jgitver from git tags — do not set version in POM files manually.

Publishing to GitHub Packages requires `GITHUB_USERNAME` and `GITHUB_PASSWORD` environment variables.

## Module Structure

**Core modules (start here):**
- `geom2d` — 2D geometry: points, lines, curves, polygons, conics, domains, transforms. Depends on `geom2d-clipper` and Apache Commons Math.
- `geom3d` — 3D geometry: points, lines, planes, curves, polygons. Depends on `geom2d`.

**2D extensions** (depend on `geom2d`): `geom2d-bezier`, `geom2d-binpacking`, `geom2d-clipper`, `geom2d-contours`, `geom2d-csg`, `geom2d-decomposition`, `geom2d-font`, `geom2d-math`, `geom2d-nesting`, `geom2d-svg`

**3D extensions** (depend on `geom3d`): `geom3d-csg`, `geom3d-io`, `geom3d-poly2tri`, `geom3d-quickhull`, `geom3d-utils`

**Independent**: `dynamicaabbtree` — AABB spatial index tree (no geometry dependency)

## Architecture

### Core Interfaces
- `GeometricObject2D` / `GeometricObject3D` — marker interfaces for all geometry types; define tolerance-based equality (`almostEquals(obj, epsilon)`) using reflection to compare `double` fields and nested geometric objects.
- `Shape2D` / `Shape3D` — primary interfaces for concrete shapes.

### Tolerance-Based Equality
Floating-point comparisons throughout the library use `Tolerance2D.get()` for configurable precision. Always use `almostEquals()` rather than `equals()` when comparing geometry results in tests.

### Package Layout (geom2d)
`math.geom2d` root contains utility classes (`Point2D`, `Vector2D`, `Box2D`, `Angle2D`, `AffineTransform2D`), with sub-packages: `point/`, `line/`, `curve/`, `polygon/`, `conic/`, `circulinear/`, `domain/`, `transform/`, `grid/`, `exceptions/`.

### Package Layout (geom3d)
Mirrors the 2D layout under `math.geom3d`: `point/`, `line/`, `plane/`, `curve/`, `polygon/`, `conic/`, `circulinear/`, `fitting/`, `transform/`, `exceptions/`.

### Testing Pattern
Each module has `AllTests` suite aggregators. Tests use JUnit 4. The `geom2d-check` module provides shared testing/verification utilities. EqualsVerifier is used to verify `equals`/`hashCode` contracts.

## Key Dependencies
- Apache Commons Math 3.6.1 — mathematical utilities used throughout
- Jackson 2.19.0 — JSON serialization (annotations on geometry classes like `Point2D`)
- JUnit 4.12 — test framework
- EqualsVerifier 4.4.1 — contract testing
