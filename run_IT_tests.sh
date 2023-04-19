#!/bin/bash

start_time=$(date +%s)  # start timer

# CP CLF
cd cp_classification
./run_IT_tests.sh
if [ $? -ne 0 ]; then
    echo "Error: IT Test for CP Classification server failed."
    exit 1
fi
# CP CLF for TCP model
./run_IT_tests_TCP.sh
if [ $? -ne 0 ]; then
    echo "Error: IT Test for - TCP - CP Classification server failed."
    exit 1
fi
cd ..
# CP REG
cd cp_regression
./run_IT_tests.sh
if [ $? -ne 0 ]; then
    echo "Error: IT Test for CP Regression server failed."
    exit 1
fi
cd ..
# Venn-ABERS
cd vap_classification
./run_IT_tests.sh
if [ $? -ne 0 ]; then
    echo "Error: IT Test for Venn-ABERS server failed."
    exit 1
fi
cd ..


end_time=$(date +%s)  # end timer
echo "

===========================================================
    All tests passed

    Total time elapsed: $((end_time - start_time)) seconds.
===========================================================
"
exit 0