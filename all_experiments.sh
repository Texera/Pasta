#!/bin/bash

# Check if the correct number of arguments was provided
if [ $# -ne 2 ]; then
    echo "Usage: $0 <input-directory> <output-directory>"
    exit 1
fi

# Assign arguments to variables
input_dir=$1
output_dir=$2

# Check if there are any .dot files in the directory
if compgen -G "${input_dir}/*.dot" > /dev/null; then
    # Use GNU parallel to run Java program on all .dot files
    parallel java -jar YourJavaProgram.jar ::: "${input_dir}"/*.dot ::: "${output_dir}"
    echo "Processing complete."
else
    echo "No .dot files found in the directory."
fi
