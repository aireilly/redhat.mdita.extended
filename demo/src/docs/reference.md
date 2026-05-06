---
author: Demo Author
category: Reference
keyword:
  - widget
  - configuration
---

# Widget configuration reference {.reference}

The `widget.yaml`{.filepath} configuration file controls widget behavior, resource limits, and connectivity.
All fields are optional unless marked **required**.

## Configuration fields

| Field             | Type    | Required | Description                                              |
|-------------------|---------|----------|----------------------------------------------------------|
| `name`            | string  | Yes      | A unique identifier for the widget.                      |
| `type`            | string  | Yes      | The widget type: `inline` or `sidecar`.                  |
| `replicas`        | integer | No       | Number of widget replicas. Default: `1`.                 |
| `port`            | integer | No       | Port the widget listens on. Default: `8080`.             |
| `timeout`         | string  | No       | Request timeout duration. Default: `30s`.                |
| `logLevel`        | string  | No       | Logging verbosity: `debug`, `info`, `warn`, or `error`.  |
| `resources.cpu`   | string  | No       | CPU limit in millicores. Default: `500m`.                |
| `resources.memory`| string  | No       | Memory limit. Default: `256Mi`.                          |

## Example configuration

The following `widget.yaml`{.filepath} file configures a sidecar widget with custom resource limits:

```yaml
name: payment-processor
type: sidecar
replicas: 3
port: 9090
timeout: 15s
logLevel: info
resources:
  cpu: "1000m"
  memory: "512Mi"
```

!!! note
    The `resources` fields map directly to Kubernetes container resource limits.
    If you omit these fields, the platform applies default limits.

## Environment variables

You can override configuration fields using environment variables.
Each variable is prefixed with `WIDGET_`{.varname} and uses uppercase with underscores:

| Variable             | Overrides       | Example          |
|----------------------|-----------------|------------------|
| `WIDGET_NAME`        | `name`          | `my-widget`      |
| `WIDGET_TYPE`        | `type`          | `sidecar`        |
| `WIDGET_REPLICAS`    | `replicas`      | `5`              |
| `WIDGET_PORT`        | `port`          | `3000`           |
| `WIDGET_TIMEOUT`     | `timeout`       | `60s`            |
| `WIDGET_LOG_LEVEL`   | `logLevel`      | `debug`          |

!!! important
    Environment variables take precedence over values in `widget.yaml`{.filepath}.

## Advanced configuration {audience="expert"}

For **expert users**{audience="expert"}, the widget runtime accepts additional low-level tuning parameters through a `widget-advanced.yaml` file.
These settings are unsupported and may change between releases.
