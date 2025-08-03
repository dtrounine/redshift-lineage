#\!/bin/bash

# Test script using standalone executable
echo "Testing redshift-lineage standalone executable against SQL files"
echo "================================================================"

SUCCESS_COUNT=0
FAILURE_COUNT=0
EMPTY_COUNT=0
TOTAL_COUNT=0

# Check if both arguments are provided
if [ $# -ne 2 ]; then
    echo "Usage: $0 <input_directory> <output_directory>"
    echo "  input_directory:  Directory containing SQL files to test"
    echo "  output_directory: Directory to generate test reports in"
    exit 1
fi

INPUT_DIR="$1"
OUTPUT_DIR="$2"

# Validate input directory exists
if [ ! -d "$INPUT_DIR" ]; then
    echo "Error: Input directory '$INPUT_DIR' does not exist"
    exit 1
fi

# Create results directory
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR/failed_files.txt" "$OUTPUT_DIR/empty_files.txt" "$OUTPUT_DIR/summary.txt"

# Function to test a single SQL file
test_sql_file() {
    local sql_file="$1"
    local filename=$(basename "$sql_file")
    local relative_path=${sql_file#$INPUT_DIR/}
    local relative_dir=$(dirname "$relative_path")
    
    # Create output directory structure to match input structure
    if [ "$relative_dir" != "." ]; then
        mkdir -p "$OUTPUT_DIR/$relative_dir"
    fi
    
    echo -n "Testing: $relative_path ... "
    
    # Check if file is empty or contains only whitespace
    if [ \! -s "$sql_file" ] || [ -z "$(grep -v '^[[:space:]]*$' "$sql_file")" ]; then
        echo "EMPTY"
        EMPTY_COUNT=$((EMPTY_COUNT + 1))
        echo "$sql_file" >> "$OUTPUT_DIR/empty_files.txt"
        TOTAL_COUNT=$((TOTAL_COUNT + 1))
        return
    fi
    
    # Run the standalone executable with output files in same relative path structure
    local output_file="$OUTPUT_DIR/${relative_path}_output.txt"
    local error_file="$OUTPUT_DIR/${relative_path}_error.txt"
    
    timeout 5s build/install/redshift-lineage/bin/redshift-lineage --in-file "$sql_file" > "$output_file" 2> "$error_file"
    exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo "SUCCESS"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "FAILED (exit code: $exit_code)"
        FAILURE_COUNT=$((FAILURE_COUNT + 1))
        # Keep the error logs for failed tests
        echo "$sql_file" >> "$OUTPUT_DIR/failed_files.txt"
        echo "Exit code: $exit_code" >> "$error_file"
        
        # Extract first line of error for summary
        first_error_line=$(head -n 1 "$error_file" 2>/dev/null)
        echo "$relative_path: $first_error_line" >> "$OUTPUT_DIR/summary.txt"
    fi
    
    TOTAL_COUNT=$((TOTAL_COUNT + 1))
}

# Test all SQL files
while IFS= read -r -d '' sql_file; do
    test_sql_file "$sql_file"
done < <(find "$INPUT_DIR" -name "*.sql" -print0 | sort -z)

echo "================================================================"
echo "Testing completed\!"
echo "Total files tested: $TOTAL_COUNT"
echo "Successful parses: $SUCCESS_COUNT"
echo "Failed parses: $FAILURE_COUNT"
echo "Empty files: $EMPTY_COUNT"
if [ $TOTAL_COUNT -gt 0 ]; then
    echo "Failure rate: $(echo "scale=2; $FAILURE_COUNT * 100 / $TOTAL_COUNT" | bc -l)%"
fi
echo ""
echo "Results saved in $OUTPUT_DIR/ directory"

