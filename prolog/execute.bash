#!/bin/bash

# Loop through all .pro files in the current directory
for file in *.test.pro; do
    # Check if any .pro files exist
    [ -e "$file" ] || continue
    
    # Extract the filename without the extension
    filename="${file%.pro}"
    
    # Execute the command and redirect output to a .txt file
    swipl --stack_limit=10G -s "$file" &> "$filename.txt"

done
