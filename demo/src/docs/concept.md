---
$schema: urn:oasis:names:tc:dita:xsd:concept.xsd
author: Demo Author
category: Concept
keyword:
  - widget
  - architecture
---

# Understanding widgets

Widgets are modular components that extend {{product-name}} with additional functionality.
Each widget runs in an isolated namespace and communicates with {{product-short}} through a well-defined API.

Understanding widget architecture helps you plan deployments and troubleshoot issues in {{product-short}} {{version}}.

## Widget types

The platform supports two widget types:

- **Inline widgets** run inside the main application process and share its memory space.
  They offer the lowest latency but cannot be restarted independently.
- **Sidecar widgets** run as separate containers alongside the main application.
  They can be scaled and restarted independently, at the cost of network overhead.

## How widgets communicate

When a widget receives a request, the following sequence occurs:

1. The platform gateway validates the request headers.
2. The router matches the request to a registered widget endpoint.
3. The widget processes the request and returns a response.

!!! note
    Inline widgets skip step 2 because they register directly with the gateway.

The following table summarizes the trade-offs between widget types:

| Feature            | Inline         | Sidecar             |
|--------------------|----------------|---------------------|
| Latency            | Low            | Medium              |
| Isolation          | Shared process | Separate container  |
| Independent scaling| No             | Yes                 |
| Restart impact     | Full restart   | Widget-only restart |

!!! warning
    Mixing inline and sidecar widgets in the same namespace can cause port conflicts.
    Always assign unique port ranges to each widget type.

## Kubernetes-specific considerations {platform="kubernetes"}

When running on Kubernetes, widgets are deployed as pods.
Sidecar widgets use a sidecar container pattern, sharing the pod network namespace with the main application container.

## OpenShift-specific considerations {platform="openshift"}

On OpenShift, widgets can take advantage of the built-in service mesh.
Use **Routes**{.uicontrol platform="openshift"} to expose widget endpoints externally.

For configuration details, see *Widget configuration reference*.
For installation steps, see *Installing the widget*.
