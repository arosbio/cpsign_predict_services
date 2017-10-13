oc policy add-role-to-user edit system:serviceaccount:modelingweb:default -n modelingweb-deployments

TOKEN="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)"
oc login --token=$TOKEN


