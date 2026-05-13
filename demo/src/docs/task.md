---
author: Demo Author
category: Task
keyword:
  - widget
  - installation
---

# Installing the widget {.task}

You can install and configure a widget on your {{product-short}} cluster using the CLI.

## Prerequisites

- You have installed the `oc`{.cmdname} CLI tool.
- You have logged in as a user with `cluster-admin` privileges.
- You have created a `widget.yaml`{.filepath} configuration file.

## About this task

This procedure deploys a widget into a dedicated namespace on {{product-name}} {{version}}.
The widget is available to all projects in the cluster after installation.

1. Create the widget namespace:

   ```bash
   oc create namespace widgets
   ```

2. Apply the widget configuration:

   ```bash
   oc apply -f widget.yaml -n widgets
   ```

   !!! note
       If a widget with the same name already exists, this command updates it in place.

3. Select the deployment strategy:

   - Rolling update
   - Recreate
   - Blue-green

4. Configure the resource profile:

   | Profile    | CPU     | Memory   |
   |------------|---------|----------|
   | Minimal    | `250m`  | `128Mi`  |
   | Standard   | `500m`  | `256Mi`  |
   | Production | `1000m` | `512Mi`  |

5. If using a sidecar widget, configure the network:

   1. Set the service port in the widget configuration.
   2. Create a `NetworkPolicy` to allow traffic:

      ```yaml
      apiVersion: networking.k8s.io/v1
      kind: NetworkPolicy
      metadata:
        name: allow-widget-traffic
        namespace: widgets
      spec:
        podSelector:
          matchLabels:
            app: widget
        ingress:
        - from:
          - namespaceSelector: {}
      ```

   3. Apply the network policy:

      ```bash
      oc apply -f network-policy.yaml -n widgets
      ```

6. Verify that the widget pod is running.

   The output shows the pod status.{.stepresult}


   ```bash
   oc get pods -n widgets
   ```

## Verification

Check the widget status:

```bash
oc get widgets -n widgets -o wide
```

The `STATUS` column should show `Running` and `READY` should show `1/1`.

## Troubleshooting {.tasktroubleshooting platform="openshift"}

If the widget fails to start on OpenShift, check the Security Context Constraints:

```bash
oc get scc -o name
oc admonish scc-subject-review -z widget-sa -n widgets
```

## Next steps

Configure additional widgets by repeating this procedure with a new `widget.yaml`{.filepath} file.

## Related information

- [Understanding widgets](concept.md)
- [Widget configuration reference](reference.md)
- [{{product-name}} documentation]({{product-url}})
- [Kubernetes documentation](https://kubernetes.io/docs/)
