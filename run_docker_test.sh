#!/bin/bash

ARG_LST=("thin" "full") # "thin" "full")
SERVER_TYPE=("cpsign-cp-reg-server" "cpsign-vap-clf-server" "cpsign-cp-clf-server")

for arg in "${ARG_LST[@]}"; do
    for st in "${SERVER_TYPE[@]}"; do

        echo "   now running with argument=$arg and st=$st"
        # Build the image
        docker build --target $st --build-arg SERVICE_TYPE=$arg -t my-img . > /dev/null
        # Start a container of that image
        docker run -p 8080:8080 -d --name my-container my-img
        # Wait 10s for it to start up
        sleep 10

        # Get openapi spec - should be OK
        api="$(curl -s -o /dev/null -I -w "%{http_code}" http://localhost:8080/api/openapi.json)"
        #api=$(curl -I http://localhost:8080/api/openapi.json)
        #echo "Response from the server (openapi): $api"
        if [[ "$api" != "200" ]]; then
            echo "No OpenAPI spec found, instead got response: $api"
            exit 2
        fi

        # Check if there is a Draw UI
        draw_ui="$(curl -s -o /dev/null -I -w "%{http_code}" http://localhost:8080/draw)"
        # Check if there is a Swagger UI
        swagger_ui=$(curl -s http://localhost:8080)


        # IF running FULL we expect to have the draw UI and Swagger UI
        if [[ "$arg" == "full" ]]; then

            if [[ "$draw_ui" == *"302"* ]]; then
                echo "Draw UI FOUND!"
            else
                echo "Did not find the draw UI! instead: $draw_ui"
                exit 2
            fi

            # we expect the swagger UI to be there!
            if [[ "$swagger_ui" == *"<title>Swagger UI</title>"* ]]; then
                echo " - SWAGGER UI PASSED"
            else
                echo " - SWAGGER UI FAILED!"
                echo " instead of swagger UI: $swagger_ui"
                exit 2
            fi
        
        else
            # No draw UI expected
            if [[ "$draw_ui" != "404" ]]; then
                echo "draw UI should be missing, but was: $draw_ui"
                exit 2
            fi
            # We do not expect that the Swagger UI should be there!
            if [[ "$swagger_ui" == *"<title>Swagger UI</title>"* ]]; then
                echo " - SWAGGER UI PRESENT WHEN IT SHOULDN'T!"
                exit 2
            fi
        fi

        # Stop and remove the test container
        docker stop my-container > /dev/null
        docker rm my-container > /dev/null

    done
done

# Verify that using an invalid argument will cause the build to fail

docker build --target 'full' --build-arg SERVICE_TYPE=invalid -t will-fail . > /dev/null 2>&1
status_code=$?

if [ $status_code -eq 0 ]; then
    echo "Docker build should failed with a build-argument 'SERVICE_TYPE' different than the allowed ones"
fi

