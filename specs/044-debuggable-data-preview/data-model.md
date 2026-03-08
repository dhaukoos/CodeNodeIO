# Data Model: Debuggable Data Runtime Preview

**Feature**: 044-debuggable-data-preview
**Date**: 2026-03-07

## No New Persistent Entities

This feature adds in-memory runtime state only. No new persistent entities or storage changes.

## Runtime State

### Transit Snapshot

| Aspect | Detail |
|--------|--------|
| Scope | Per-connection, in-memory only |
| Key | Connection ID (String, from FlowGraph.connections) |
| Value | Most recent data value (Any?) that passed through the connection |
| Lifecycle | Created on runtime start with debug enabled; cleared on stop or debug toggle off |
| Access Pattern | Written by channel send operations; read by Properties panel UI when paused |

### Debug State

| Aspect | Detail |
|--------|--------|
| Debug enabled | Tied to existing `animateDataFlow` toggle (Boolean) |
| Execution state | Snapshots viewable only when PAUSED |
| Snapshot storage | Map<String, StateFlow<Any?>> keyed by connection ID |
